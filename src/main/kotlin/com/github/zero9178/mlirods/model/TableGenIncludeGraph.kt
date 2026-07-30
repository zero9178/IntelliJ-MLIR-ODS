package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.index.TableGenIncludeEntry
import com.github.zero9178.mlirods.index.getIncludeEntries
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.lsp.isTableGenFile
import com.github.zero9178.mlirods.lsp.isTableGenFileName
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.backgroundWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportProgressScope
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.TestOnly
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Whether this event may change what an 'include' directive resolves to, i.e. whether it makes a TableGen file appear at
 * or disappear from a path.
 *
 * Directories count just as much as TableGen files do: they carry whatever files they contain with them, and an include
 * path only resolves anything at all once the directory it names exists.
 */
private val VFileEvent.changesIncludeResolution: Boolean
    get() = when (this) {
        is VFileContentChangeEvent -> false
        // The created file does not exist yet, making its name all there is to go by.
        is VFileCreateEvent -> isDirectory || childName.isTableGenFileName
        is VFileCopyEvent -> file.isDirectory || newChildName.isTableGenFileName
        // A rename makes the file disappear under its old name and appear under its new one.
        is VFilePropertyChangeEvent -> propertyName == VirtualFile.PROP_NAME && (file.isDirectory ||
                (oldValue as String).isTableGenFileName || (newValue as String).isTableGenFileName)
        // Deletions and moves, both of which name an existing file.
        else -> file?.let { it.isDirectory || it.isTableGenFile } == true
    }

/**
 * Maintains the include graph of all TableGen files reachable from the compile commands and exposes, per file, the
 * active compilation context it derives from that graph: its include paths, the files it is included from and the files
 * included before it. It is the single source of truth for include resolution and cross-file scoping.
 *
 * The state the service keeps is nothing but the graph itself – files as nodes, resolved 'include' directives as edges
 * that can be walked in both directions. Everything a caller asks about is a traversal of that graph rather than
 * something stored in it: which root a file derives its context from is the search for the first root that reaches it,
 * and which files are visible to a file is the prefix of the root's expansion that ends with it. Nothing about a file's
 * position in the graph lives in the file's node, which is what keeps an update local – recomputing the edges of the
 * files that actually changed – and every query linear in the size of the graph at worst, with the result cached for as
 * long as the graph does not change.
 */
@Service(Service.Level.PROJECT)
class TableGenIncludeGraphService(val project: Project, private val cs: CoroutineScope) : Disposable {

    companion object {
        private val LOGGER = logger<TableGenIncludeGraphService>()

        /**
         * Number of files whose edges are resolved in one read action and applied in one write action. Batching keeps
         * the number of write actions – and therefore the number of times the graph is observable in a partial state –
         * proportional to the number of files that changed rather than to the size of the graph, while still being
         * small enough not to block readers for long.
         */
        private const val BATCH_SIZE = 64

        /**
         * Upper bound on the number of rounds [update] performs before giving up. A cold start takes one round per
         * level of the include chains below the roots, hence the generous bound; reaching it means edges and contexts
         * keep invalidating each other, which the include paths being propagated unchanged down every include chain
         * makes impossible. The bound only exists so that a bug cannot turn into a livelock.
         */
        private const val MAX_ROUNDS = 1000
    }

    // -----------------------------------------------------------------------------------------------------------------
    // The graph
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Edge of the include graph, i.e. one 'include' directive of a file that resolved to another file.
     */
    internal data class IncludeEdge(
        /**
         * The include directive this edge was created from.
         */
        val entry: TableGenIncludeEntry,
        /**
         * File the directive resolved to.
         */
        val node: FileNode,
    )

    /**
     * Node of the include graph, one per file that is part of it.
     *
     * Nodes are only ever mutated under a write action and only ever read under a read action, which is what makes the
     * plain collections below safe to use.
     */
    internal class FileNode(val file: VirtualFile) {

        /**
         * Files this file includes, in the lexical order of the directives they stem from. A file included more than
         * once only yields a single edge as including it twice does not make it reachable twice.
         */
        var outgoing: List<IncludeEdge> = emptyList()
            private set

        /**
         * Files including this file, keyed and therefore ordered by their url, so that walking the graph backwards is
         * as deterministic as walking it forwards. A file can appear at most once as [outgoing] holds at most one edge
         * per target.
         */
        private val myIncoming = TreeMap<String, FileNode>()

        /**
         * Files including this file, in a deterministic order.
         */
        val incoming: Collection<FileNode>
            get() = myIncoming.values

        /**
         * Include paths [outgoing] was resolved against, or `null` if the edges were never computed. Both what a
         * directive resolves to and whether it resolves at all is a function of these, making them the criterion for
         * whether a node's edges have to be recomputed after its context changed.
         */
        var includePaths: List<VirtualFile>? = null
            private set

        /**
         * Replaces the outgoing edges of this node with [edges], resolved against [paths], and mirrors the change into
         * the incoming edges of the nodes involved. Returns whether the edges actually changed.
         */
        fun setOutgoing(edges: List<IncludeEdge>, paths: List<VirtualFile>): Boolean {
            includePaths = paths
            if (outgoing == edges) return false

            // 'FileNode' has identity equality, making this an identity set.
            val targets = edges.mapTo(mutableSetOf()) { it.node }
            outgoing.forEach {
                if (it.node !in targets) it.node.myIncoming.remove(file.url, this)
            }
            edges.forEach { it.node.myIncoming[file.url] = this }
            outgoing = edges
            return true
        }
    }

    /**
     * A file the compile commands name, i.e. an entry point into the graph contributing the include paths every file
     * reachable from it resolves its includes against.
     */
    internal class Root(val node: FileNode, val includePaths: List<VirtualFile>)

    /**
     * Context in which a given [TableGenFile] is parsed and resolved in. This is derived from some root file which
     * has a compilation command.
     */
    class TableGenCompilationContext internal constructor(val includePaths: List<VirtualFile>)

    /**
     * Reference to a node that remembers the file it is mapped from, allowing [gcNodes] to drop exactly the entries
     * whose node has been collected via [myCollectedNodes].
     */
    private class NodeReference(
        node: FileNode, val file: VirtualFile, queue: ReferenceQueue<FileNode>
    ) : WeakReference<FileNode>(node, queue)

    private val myCollectedNodes = ReferenceQueue<FileNode>()

    // No need to use a read or write action for this BUT existence or non-existence of a node mustn't be an observable
    // leak outside this service either. Note that the nodes inside must still only be modified under a write action.
    private val myFileToNodeMapping = ConcurrentHashMap<VirtualFile, NodeReference>()

    /**
     * Roots in descending priority: a file reachable from several of them derives its context from the first one.
     * Only mutated under a write action.
     */
    private var myRoots: List<Root> = emptyList()

    // -----------------------------------------------------------------------------------------------------------------
    // Everything derived from the graph
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The order in which a root pastes the files it includes into itself, i.e. its depth-first expansion.
     */
    private class Expansion(root: FileNode) {

        /**
         * Files in the order the expansion reaches them, each appearing exactly once: a file already pasted in is not
         * pasted in a second time.
         */
        private val myOrder = mutableListOf<VirtualFile>()

        /**
         * Position of each file of [myOrder] within it.
         */
        private val myPositions = HashMap<VirtualFile, Int>()

        /**
         * For each position, the position just past the last file the file at that position pastes in, i.e. the end of
         * its subtree.
         */
        private val myEnds = mutableListOf<Int>()

        init {
            class Frame(val node: FileNode, val position: Int, var next: Int = 0)

            // Iterative rather than recursive as include chains are arbitrarily long.
            val stack = mutableListOf<Frame>()

            fun enter(node: FileNode) {
                val position = myOrder.size
                if (myPositions.putIfAbsent(node.file, position) != null) return

                myOrder.add(node.file)
                myEnds.add(position)
                stack.add(Frame(node, position))
            }

            enter(root)
            while (stack.isNotEmpty()) {
                val frame = stack.last()
                val edges = frame.node.outgoing
                if (frame.next < edges.size) enter(edges[frame.next++].node)
                else {
                    myEnds[frame.position] = myOrder.size
                    stack.removeLast()
                }
            }
        }

        /**
         * Returns everything the expansion contains up to and including everything [file] pastes into it, or `null` if
         * [file] is not part of the expansion at all.
         *
         * That prefix is exactly the set of files visible to [file]: the files pasted in before it – the files it is
         * included from and the files included before it – plus the files it pastes in itself. The latter are all in
         * the prefix because a depth-first traversal only leaves a file once everything reachable from it has been
         * visited, so any file [file] includes was either already pasted in before it or is pasted in within its
         * subtree.
         */
        fun visibleFrom(file: VirtualFile): Set<VirtualFile>? {
            val position = myPositions[file] ?: return null
            return Prefix(myOrder, myPositions, myEnds[position])
        }
    }

    /**
     * Immutable view of the first [size] files of [order], which [positions] maps to their position within.
     *
     * Materializing the prefix would make every query proportional to the number of files visible to the queried one,
     * while [contains] – the only thing a search scope ever needs – is a lookup.
     */
    private class Prefix(
        private val order: List<VirtualFile>,
        private val positions: Map<VirtualFile, Int>,
        override val size: Int,
    ) : AbstractSet<VirtualFile>() {

        override fun contains(element: VirtualFile): Boolean = (positions[element] ?: return false) < size

        override fun iterator(): Iterator<VirtualFile> = order.subList(0, size).iterator()
    }

    // Both caches below are dropped as soon as the graph changes and are computed – and read – under a read action, so
    // the graph they describe cannot change underneath them. That also makes them consistent with each other without
    // having to be cached together: a reader validating both against the same modification count within one read action
    // cannot see one of them describe a newer graph than the other.

    private val myLabelingCache: CachedValue<Map<VirtualFile, Root>> =
        CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result.create(computeLabeling(myRoots), graphChangedModificationTracker)
        }

    /**
     * Maps every file that has a context to the root it derives that context from, i.e. to the first root that reaches
     * it.
     */
    private val labeling: Map<VirtualFile, Root>
        @RequiresReadLock get() = myLabelingCache.value

    /**
     * Labels every file with the first root of [roots] that reaches it.
     *
     * The whole map is computed in a single traversal: expanding the roots in priority order while never entering a
     * file twice labels every file with the first root that got to it, in one pass over the graph for all files at
     * once. Searching backwards from a single file instead would have to enumerate all of that file's ancestors to find
     * the best root among them – and do so again for the next file.
     */
    @RequiresReadLock
    private fun computeLabeling(roots: List<Root>): Map<VirtualFile, Root> {
        val result = HashMap<VirtualFile, Root>()
        val stack = mutableListOf<FileNode>()
        roots.forEach { root ->
            stack.add(root.node)
            while (stack.isNotEmpty()) {
                val node = stack.removeLast()
                if (result.putIfAbsent(node.file, root) != null) continue

                // Reversed so that the traversal descends into the includes in lexical order.
                node.outgoing.asReversed().forEach { stack.add(it.node) }
            }
        }
        return result
    }

    /**
     * The expansions computed so far. Unlike the labeling, which covers every file at once, there is one expansion per
     * root – i.e. per compile command – so they are memoized individually as only the roots someone actually asks about
     * are worth expanding.
     */
    private val myExpansionCache: CachedValue<ConcurrentHashMap<Root, Expansion>> =
        CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result.create(ConcurrentHashMap<Root, Expansion>(), graphChangedModificationTracker)
        }

    @RequiresReadLock
    private fun expansionOf(root: Root): Expansion =
        myExpansionCache.value.computeIfAbsent(root) { Expansion(it.node) }

    // -----------------------------------------------------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns the active compilation context of [vf] – the one derived from the highest-priority compile-command root
     * that reaches [vf] – or `null` if [vf] is not reachable from any file with compile commands.
     */
    @RequiresReadLock
    fun getContextOf(vf: VirtualFile): TableGenCompilationContext? =
        labeling[vf]?.let { TableGenCompilationContext(it.includePaths) }

    /**
     * Returns the set of files that currently have an active context, i.e. the compile-command roots and every file
     * transitively included from one of them.
     */
    @RequiresReadLock
    fun getFilesWithContext(): Set<VirtualFile> = labeling.keys

    /**
     * Returns the set of all files that are visible to [file] in its active context: the files it is included from, the
     * files included before it (together with their transitive includes) and its own transitive includes.
     */
    @RequiresReadLock
    fun getIncludedFiles(file: TableGenFile): Set<VirtualFile> = CachedValuesManager.getCachedValue(file) {
        val result = file.originalFile.virtualFile?.let { vf ->
            labeling[vf]?.let { expansionOf(it).visibleFrom(vf) }
        }.orEmpty()
        CachedValueProvider.Result.create(result, graphChangedModificationTracker)
    }

    /**
     * Returns the files that include [vf] in their active context, in a deterministic order.
     */
    @RequiresReadLock
    fun getIncludersOf(vf: VirtualFile): List<VirtualFile> =
        getNodeFor(vf)?.incoming?.map { it.file }.orEmpty()

    // -----------------------------------------------------------------------------------------------------------------
    // Change notification
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Modification tracker which gets incremented each time the graph changes, i.e. each time an edge appears,
     * disappears or moves and each time the roots change. Anything derived from the graph – which file has which
     * context, which files an 'include' directive may resolve to – is therefore invalidated by it.
     */
    val graphChangedModificationTracker: ModificationTracker = ModificationTracker { graphGeneration.value }

    /**
     * Generation counter bumped in lockstep with [graphChangedModificationTracker].
     * Consumers can collect this [StateFlow] to react to graph changes.
     */
    val graphGeneration: Flow<Long>
        field = MutableStateFlow(0L)

    /**
     * [SharedFlow] that receives the compilation commands state any time that state has finished being applied to the
     * graph. Used by tests to await the graph having settled after a compile-commands change. Emitting the very same
     * state twice therefore also yields twice.
     */
    @TestOnly
    val finishedCompileCommands: SharedFlow<CompilationCommandsState>
        field = MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Number of graph updates that have been triggered but have not finished being applied yet.
     *
     * The count is incremented synchronously by whatever triggers the update (a PSI change, new files being indexed),
     * i.e. before the coroutine performing it is even dispatched. Something that caused an update – e.g. a test writing
     * to a file – can therefore await this reaching zero afterwards to know the graph has caught up with it.
     */
    @TestOnly
    val updatesInFlight: StateFlow<Int>
        field = MutableStateFlow(0)

    override fun dispose() {

    }

    // -----------------------------------------------------------------------------------------------------------------
    // Updating the graph
    // -----------------------------------------------------------------------------------------------------------------

    // Lock that holds up the invariant that there might only be one graph modification operation occurring at once.
    // This lock only needs to be held by any writing operations, not any reading operations as it merely ensures that
    // any inflight coroutines are not conflicting and therefore there are no lost-updates.
    //
    // Read operations (that must be executed under a read lock) are allowed to observe partial states. Anything they
    // cache must depend on 'graphChangedModificationTracker' so that a computation performed on a partial state is
    // dropped again once a consistent state has been established.
    private val mySingleUpdateLock = Mutex()

    /**
     * Files whose edges have to be recomputed regardless of whether their context changed, and whether that is the case
     * for every file of the graph. Guarded by the monitor of [myPendingFiles].
     */
    private val myPendingFiles = mutableSetOf<VirtualFile>()
    private var myPendingAllFiles = false

    /**
     * Launches [action] as a graph update, serialized with all other graph updates via [mySingleUpdateLock] and
     * accounted for in [updatesInFlight].
     */
    private fun launchUpdate(action: suspend CoroutineScope.() -> Unit) {
        updatesInFlight.update { it + 1 }
        cs.launch {
            mySingleUpdateLock.withLock {
                action()
            }
        }.invokeOnCompletion {
            updatesInFlight.update { it - 1 }
        }
    }

    /**
     * Schedules the edges of the nodes of [files], if any, to be recomputed.
     *
     * Every file is only ever recomputed once per update: whatever accumulates here while an update is in flight is
     * drained by the next one in a single batch. Triggers whose files were already taken care of that way therefore
     * find nothing left to do, which keeps a bulk change – e.g. a version control operation touching every file of the
     * project – to a single pass over the affected nodes instead of one pass, and one write action, per file.
     */
    private fun scheduleUpdateOf(files: Collection<VirtualFile>) {
        if (files.isEmpty()) return

        synchronized(myPendingFiles) {
            myPendingFiles.addAll(files)
        }
        launchUpdate { update() }
    }

    /**
     * Schedules the edges of every node to be recomputed, which is what a TableGen file having appeared or disappeared
     * amounts to: any 'include' directive of any file may resolve to a different file than before.
     */
    private fun scheduleUpdateOfAll() {
        synchronized(myPendingFiles) {
            myPendingAllFiles = true
        }
        launchUpdate { update() }
    }

    /**
     * Brings the graph back in sync with the files it is built from.
     *
     * Resolving the includes of a file needs the include paths of the file's context, which are a property of the graph
     * those includes are part of: a file only resolves anything once it is reachable from a root, and the files it then
     * includes become reachable in turn. The update therefore alternates between deriving every file's context from the
     * graph – a single traversal, see [computeLabeling] – and recomputing the edges of the files whose current edges
     * were resolved against different include paths than their context now provides, until nothing is left to do.
     *
     * A file that merely changed its contents settles in a single round; a root being added takes one round per level
     * of the include chains below it, as each round discovers the files the previous one made reachable.
     *
     * Unless [force] is set, an update whose files another update already drained returns immediately, so that a burst
     * of triggers costs a single pass.
     */
    private suspend fun update(force: Boolean = false) {
        val (pendingFiles, pendingAllFiles) = synchronized(myPendingFiles) {
            val result = myPendingFiles.toSet() to myPendingAllFiles
            myPendingFiles.clear()
            myPendingAllFiles = false
            result
        }
        if (!force && !pendingAllFiles && pendingFiles.isEmpty()) return

        // Files to recompute even if their context did not change. 'null' stands for every file.
        var forced: Set<VirtualFile>? = if (pendingAllFiles) null else pendingFiles

        var round = 0
        while (true) {
            val stale = readAction { collectStaleNodes(forced) }
            if (stale.isEmpty()) break

            if (++round > MAX_ROUNDS) {
                LOGGER.error("Include graph did not settle after $MAX_ROUNDS rounds")
                break
            }
            // Whatever changed on disk has been taken into account by the first round; every further round only exists
            // to catch up with the contexts the previous one changed.
            forced = emptySet()

            applyEdges(stale)
        }

        gcNodes()
    }

    /**
     * Returns every node whose edges disagree with its context, paired with the include paths that context provides.
     * Nodes in [forced] – `null` meaning every node – are returned even if they agree.
     */
    @RequiresReadLock
    private fun collectStaleNodes(forced: Set<VirtualFile>?): List<Pair<FileNode, List<VirtualFile>>> {
        val labeling = this.labeling
        return myFileToNodeMapping.values.mapNotNull { reference ->
            val node = reference.get() ?: return@mapNotNull null

            // A file without a context resolves no includes at all, which is precisely what an empty list of include
            // paths yields, so it needs no special casing. Note that clearing the edges of such a file also matters for
            // the files it used to include: as long as it holds edges to them it keeps them alive.
            val paths = labeling[node.file]?.includePaths.orEmpty()
            if (node.includePaths == paths && !(forced == null || node.file in forced)) return@mapNotNull null

            node to paths
        }
    }

    /**
     * Recomputes and applies the outgoing edges of every node of [stale].
     *
     * Resolving the includes of a file is by far the most expensive part of an update – it reads the file's directives
     * off the include index and looks each of them up in the include paths – so it is done for a whole batch of files
     * at once, in parallel and outside of any write action; only the resulting edges are then applied to the graph in
     * a single write action per batch.
     */
    private suspend fun applyEdges(stale: List<Pair<FileNode, List<VirtualFile>>>) {
        val batches = stale.chunked(BATCH_SIZE)
        reportProgressScope(batches.size) { reporter ->
            batches.map { batch ->
                async {
                    reporter.itemStep(
                        MyBundle.message("tableGen.progress.updatingContext", batch.first().first.file.name)
                    ) {
                        // Resolving does not look at the graph, only at the file and its include paths, so a graph
                        // change in between this and the write action below cannot invalidate the result. A change to
                        // the include paths themselves is caught by the next round, which finds the node stale again.
                        val resolved = readAction {
                            batch.map { (node, paths) -> Triple(node, paths, resolveIncludes(node, paths)) }
                        }

                        backgroundWriteAction {
                            var changed = false
                            resolved.forEach { (node, paths, includes) ->
                                val edges = includes.map { (entry, file) -> IncludeEdge(entry, getOrCreate(file)) }
                                if (node.setOutgoing(edges, paths)) changed = true
                            }
                            if (changed) graphGeneration.update { it + 1 }
                        }
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * Resolves the 'include' directives of [node]'s file against [includePaths], dropping the ones that resolve to
     * nothing and the ones that resolve to a file an earlier directive already includes.
     */
    @RequiresReadLock
    private fun resolveIncludes(
        node: FileNode, includePaths: List<VirtualFile>
    ): List<Pair<TableGenIncludeEntry, VirtualFile>> {
        val file = node.file
        if (includePaths.isEmpty() || !file.isValid || project.isDisposed) return emptyList()

        val seen = mutableSetOf<VirtualFile>()
        return getIncludeEntries(project, file).mapNotNull { entry ->
            val included = includePaths.firstNotNullOfOrNull { path ->
                if (!path.isValid) null else path.findFileByRelativePath(entry.suffix)
            } ?: return@mapNotNull null

            // Including a file more than once does not make it reachable twice; the earlier include always wins.
            if (!seen.add(included)) return@mapNotNull null

            entry to included
        }
    }

    /**
     * Drops the mapping of every node that has been garbage collected.
     *
     * Only nodes that have actually been collected end up in [myCollectedNodes], making this proportional to the number
     * of dead nodes rather than to the size of the graph. In particular, the common case of nothing having been
     * collected does not take a write action at all.
     */
    private suspend fun gcNodes() {
        val collected = generateSequence { myCollectedNodes.poll() }.filterIsInstance<NodeReference>().toList()
        if (collected.isEmpty()) return

        backgroundWriteAction {
            // Removal is conditional on the reference still being the mapped one so that a node recreated for the same
            // file in the meantime survives.
            collected.forEach { myFileToNodeMapping.remove(it.file, it) }
        }
    }

    private fun getOrCreate(vf: VirtualFile): FileNode {
        var node: FileNode? = null
        // 'compute' rather than 'computeIfAbsent' as the mapping may still hold a reference whose node has been
        // collected but which 'gcNodes' has not gotten around to removing yet.
        myFileToNodeMapping.compute(vf) { file, reference ->
            node = reference?.get()
            if (node != null) return@compute reference

            val created = FileNode(file)
            node = created
            NodeReference(created, file, myCollectedNodes)
        }
        // Kotlin doesn't know that the compute method is a crossinline lambda and therefore can't prove that node will
        // never be null due to concurrent access.
        return node!!
    }

    private fun getNodeFor(vf: VirtualFile): FileNode? {
        if (!vf.isTableGenFile) return null

        return myFileToNodeMapping[vf]?.get()
    }

    init {
        // Refresh the edges of a file if its contents change.
        //
        // Note that this deliberately listens for document and VFS changes rather than for Psi changes: a file's
        // include directives are read off its document or off the include index, neither of which requires the file's
        // Psi to be loaded. Psi change events are only ever fired for files whose Psi someone has parsed, so listening
        // for those would miss every file the user has not opened.
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val vf = FileDocumentManager.getInstance().getFile(event.document) ?: return
                if (!vf.isTableGenFile) return

                scheduleUpdateOf(listOf(vf))
            }
        }, this)

        // Documents only cover files that are being edited. Files changing on disk – e.g. by being saved or by a
        // version control operation – have to be picked up as well, as does the file tree itself changing shape.
        VirtualFileManager.getInstance().addAsyncFileListener({ events ->
            val changed = mutableSetOf<VirtualFile>()
            var structureChanged = false
            events.forEach { event ->
                if (event is VFileContentChangeEvent) {
                    if (event.file.isTableGenFile) changed.add(event.file)
                } else if (event.changesIncludeResolution) structureChanged = true
            }
            if (changed.isEmpty() && !structureChanged) return@addAsyncFileListener null

            object : AsyncFileListener.ChangeApplier {
                // Any file appearing, disappearing or being renamed may change what the includes of any file resolve
                // to, not just those of the file the event is about, hence the world is refreshed rather than the file.
                override fun afterVfsChange() =
                    if (structureChanged) scheduleUpdateOfAll() else scheduleUpdateOf(changed)
            }
        }, this)

        cs.launch(start = CoroutineStart.UNDISPATCHED) {
            project.service<CompilationCommands>().flow.collect { state ->
                updatesInFlight.update { it + 1 }
                try {
                    mySingleUpdateLock.withLock {
                        withBackgroundProgress(project, MyBundle.message("tableGen.progress.updatingContexts")) {
                            val startTime = System.nanoTime()

                            backgroundWriteAction {
                                // The order of the compile commands is the priority of the roots they yield.
                                myRoots = state.map.map { (file, paths) -> Root(getOrCreate(file), paths.paths) }
                                graphGeneration.update { it + 1 }
                            }
                            // Every file's context may have changed, so the graph has to be walked even if nothing was
                            // scheduled for recomputation.
                            update(force = true)

                            LOGGER.info(
                                "Updating contexts after compile commands change took " + "${(System.nanoTime() - startTime) / 1.0e9} seconds"
                            )
                        }
                        finishedCompileCommands.emit(state)
                    }
                } finally {
                    updatesInFlight.update { it - 1 }
                }
            }
        }
    }
}
