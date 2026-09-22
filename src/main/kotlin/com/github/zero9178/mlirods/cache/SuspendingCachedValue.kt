package com.github.zero9178.mlirods.cache

import com.intellij.concurrency.currentThreadContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.*
import java.lang.ref.SoftReference
import kotlin.coroutines.CoroutineContext

/**
 * Receiver of the function computing a [SuspendingCachedValue], through which the computation declares what its result
 * depends on, and the scope of the coroutines it launches.
 *
 * Dependencies may be declared at any point and from any thread, child coroutines of the computation included. They
 * are stamped the moment they are declared.
 * A dependency that may change at any time (outside of a write action) it is best declared before reading from it, as
 * a change happening in between then makes for a value that is outdated rather than one that is wrong.
 */
sealed interface SuspendingCachedValueScope : CoroutineScope {

    /**
     * Makes the value depend on the modification count of all [trackers]. [ModificationTracker.EVER_CHANGED] makes the
     * value not be cached at all: concurrent requests still share one computation, but the next request computes anew.
     */
    fun dependsOn(vararg trackers: ModificationTracker)

    /**
     * Makes the value depend on the modification stamp of [file], i.e. on its content.
     */
    fun dependsOn(file: VirtualFile)

    /**
     * Makes the value depend on the modification stamp of [document].
     */
    fun dependsOn(document: Document)

    /**
     * Makes the value depend on the file containing [element], and on [element] remaining valid. Elements not within a
     * file, such as directories, depend on any PSI change in the project instead.
     */
    fun dependsOn(element: PsiElement)
}

/**
 * Thrown into a computation requesting a [SuspendingCachedValue] that is – directly or through any number of other
 * cached values – waiting for the value being computed, as neither could ever complete. Only values not saying what
 * they are on a cycle are requested in vain, see [SuspendingCachedValue].
 */
class CyclicCachedValueDependencyException internal constructor(cycle: List<String>) :
    IllegalStateException("Cyclic dependency between cached values: ${cycle.joinToString(" -> ")}")

/**
 * A value cached until one of the dependencies it declared changes, or until memory runs low, computed by a suspending
 * function.
 *
 * This is [com.intellij.psi.util.CachedValue] for computations that are coroutines: the computation declares its
 * dependencies (see [SuspendingCachedValueScope]), these are stamped, and the value is served from the cache for as
 * long as all stamps remain what they were. Like the platform's, it is requested and computed with read access, which
 * is what keeps everything a computation looks at from changing while it does. What it adds is:
 *
 *  * **Computations are not duplicated.** A request arriving while the value is being computed waits until that
 *    computation is done instead of starting its own, as the platform does. The computation is part of the request
 *    that started it. Should that request be cancelled, one of those waiting computes the value instead.
 *  * **Dependencies are transitive.** A computation requesting another [SuspendingCachedValue] depends on everything
 *    the requested value depends on, without having to declare so.
 *  * **Cycles do not deadlock**, see below.
 *  * **Computations can be parallel and deep**, see below.
 *
 * Exceptions thrown by the computation are rethrown to everyone waiting for it and are not cached.
 *
 * ### Requesting values
 *
 * The platform calls with read access but cannot suspend, which is what [getBlocking] is for. It is where suspending
 * code is entered, and once it is, it should not be left: a provider requests other values using [await], and offers
 * whatever it is part of to other providers as a suspending function, such that these never have to call [getBlocking]
 * while already being a coroutine. That works, but occupies a thread, and its stack, for as long as it takes.
 *
 * Callers that are not cancellable at all, or whose read access is that of a write action or the write-intent lock, as
 * is that of the EDT, are served as well. What they do not get is the above: the value is computed for however long
 * it takes, as it would by the platform's cached values.
 *
 * ### Recursion and parallelism
 *
 * A provider is an ordinary coroutine. Requesting another value from it that has to be computed is an ordinary call
 * on the stack of the thread, and recursing deep enough overflows it just like it would without coroutines. What
 * coroutines add is the ability to do something about it where it matters: a provider launching the requests it makes,
 * as in `async { other.await() }`, has them computed from the bottom of the stack of some thread, in parallel if there
 * are several, while it is suspended and lives on the heap. Nothing this class does on top of calling the provider
 * recurses with the depth of such nesting.
 *
 * ### Cycles
 *
 * Values requesting each other in a cycle could never complete. What happens instead is up to [onCycle], which is what
 * the value is whenever it is part of a cycle: its provider is cancelled and whatever it came to discarded, no matter
 * which of the values on the cycle was requested first, or which request was the one to close it. The values of a
 * program containing a cycle are therefore the same every time. They are cached like any other, until a dependency
 * declared by any of the values on the cycle changes, as the cycle might be gone then.
 *
 * Without [onCycle], a cycle is a failure: the request closing it throws [CyclicCachedValueDependencyException], unless
 * the value requested is one with [onCycle], which it then gets.
 *
 * [onCycle] is called with no lock held but may be called more than once per cycle, and from any thread.
 *
 * [name] identifies the value in diagnostics.
 *
 * @see getSuspendingCachedValue
 */
class SuspendingCachedValue<T>(
    internal val name: String = "<anonymous>",
    internal val onCycle: (() -> T)? = null,
    private val provider: suspend SuspendingCachedValueScope.() -> T,
) {
    private val lock = Any()

    /**
     * Last value computed, possibly outdated, or collected for lack of memory, as the platform's cached values are.
     * Written with [lock] held.
     */
    @Volatile
    private var data: SoftReference<Data<T>>? = null

    /**
     * Completed by the request computing the value right now with what it computed, or with `null` if it got cancelled
     * before it could. Guarded by [lock].
     */
    private var inFlight: CompletableDeferred<Result<Data<T>>?>? = null

    /**
     * The provider being run right now, if any, which is where [WaitForGraph] continues from a value to the values it
     * is waiting for.
     */
    @Volatile
    internal var computation: Computation? = null

    private val upToDateData: Data<T>?
        get() = data?.get()?.takeIf { it.isUpToDate }

    /**
     * Returns the cached value if it is up to date. Otherwise, waits for the request computing it right now, or
     * computes it as part of this one if there is none.
     *
     * When called from within the computation of another [SuspendingCachedValue], that value additionally depends on
     * everything this one depends on.
     */
    @RequiresReadLock
    suspend fun await(): T {
        val consumer = currentCoroutineContext()[Computation]
        while (true) {
            upToDateData?.let {
                consumer?.consume(it)
                return it.value
            }

            val (pending, isOurs) = synchronized(lock) {
                // Somebody published a value since; it is most likely the one we are after.
                if (upToDateData != null) return@synchronized null
                inFlight?.let { return@synchronized it to false }
                CompletableDeferred<Result<Data<T>>?>().also { inFlight = it } to true
            } ?: continue

            // No outcome means whoever was computing got cancelled. We were not.
            val outcome = WaitForGraph.waiting(consumer, this, onCycle = { dependencies ->
                assert(!isOurs) {
                    "a value on a cycle is being computed already"
                }
                // Nothing to answer if the provider asking is one that was cancelled for the cycle.
                currentCoroutineContext().ensureActive()
                // Not waiting for the computation to come to the very same, which it may be us keeping from doing so.
                Result.success(Data(checkNotNull(onCycle)(), dependencies))
            }) {
                if (isOurs) compute(pending) else pending.await()
            } ?: continue
            val result = outcome.getOrThrow()
            consumer?.consume(result)
            return result.value
        }
    }

    /**
     * [await] for callers that cannot suspend, see the documentation of the class.
     */
    @RequiresReadLock
    @RequiresBlockingContext
    fun getBlocking(): T {
        // Worth not setting up a coroutine for, this being what almost every call amounts to.
        upToDateData?.let {
            currentComputation.get()?.consume(it)
            return it.value
        }
        // The platform makes the context of a calling provider, if any, the context of the coroutine.
        val request = {
            runBlockingCancellable {
                // Read access is lent to us, and so is write access. That of the write-intent lock is not: what we are
                // in instead is a level of locking of our own, below the one of the caller, in which nobody has access
                // to anything yet. A read action in it is not kept from starting by anything the caller holds, and is
                // something that can be lent.
                if (ApplicationManager.getApplication().isReadAccessAllowed) return@runBlockingCancellable await()

                ReadAction.computeBlocking<T, RuntimeException> { runBlockingCancellable { await() } }
            }
        }
        if (currentThreadContext()[Job] != null || ProgressManager.getGlobalProgressIndicator() != null) {
            return request()
        }

        // Not everything the platform does is cancellable. Such a caller gets what it would from the platform's cached
        // values: the value, however long that takes. It is given something to be cancelled by regardless, as blocking
        // on a coroutine demands there to be one.
        return ProgressManager.getInstance().runProcess<T>(request, EmptyProgressIndicator())
    }

    private suspend fun compute(pending: CompletableDeferred<Result<Data<T>>?>): Result<Data<T>> {
        var outcome: Result<Data<T>>? = null
        val computation = Computation(this)
        try {
            this.computation = computation
            val computed = try {
                // The request may be running anywhere, while a provider wants to be where its coroutines are parallel.
                val value = withContext(Dispatchers.Default + computation) {
                    computation.coroutineContext = coroutineContext
                    computation.provider()
                }
                Result.success(value)
            } catch (e: Throwable) {
                Result.failure(e)
            }

            // Takes precedence over the request having been cancelled, which it is if made by a provider cancelled for
            // being on the same cycle. The value would otherwise be computed from the others once requested again
            // rather than be what it is on a cycle, making it depend on which of them was requested first.
            outcome = computation.cycleDependencies?.let { dependencies ->
                runCatching { Data(checkNotNull(onCycle)(), dependencies) }
            }
            if (outcome != null) {
                currentCoroutineContext().ensureActive()
                return outcome
            }

            outcome = computed.fold({ Result.success(computation.seal(it)) }) {
                // A cancellation exception despite not being cancelled is a failure of the provider like any other,
                // e.g. a timeout.
                if (it is CancellationException && !currentCoroutineContext().isActive) throw it
                Result.failure(it)
            }
            return outcome
        } finally {
            // No longer waiting for anything by now, its coroutines being done.
            this.computation = null
            synchronized(lock) {
                assert(inFlight === pending) {
                    "this method should never have been called otherwise"
                }
                inFlight = null
                outcome?.getOrNull()?.let { data = SoftReference(it) }
            }
            // Only now, so that those waiting see the above when resumed.
            pending.complete(outcome)
        }
    }

    override fun toString() = "SuspendingCachedValue($name)"
}

/**
 * A computed value with everything needed to tell whether it is still up to date.
 */
internal class Data<T>(val value: T, val dependencies: Set<Dependency>) {
    val isUpToDate: Boolean
        get() = dependencies.all { it.isUpToDate }
}

/**
 * The stamp [source] had when the dependency was declared, to be compared with its current one. A stamp of `null` is
 * never up to date.
 *
 * Equal if declared on the same source while it had the same stamp, which is what makes the dependencies of a value
 * a small set no matter how many values it was computed from: these mostly depend on the same few things.
 */
internal class Dependency(private val source: Any, private val currentStamp: () -> Long?) {
    private val stamp = currentStamp()

    val isUpToDate: Boolean
        get() = stamp != null && stamp == currentStamp()

    override fun equals(other: Any?) = other is Dependency && other.source === source && other.stamp == stamp

    override fun hashCode() = 31 * System.identityHashCode(source) + stamp.hashCode()
}

/**
 * Which value is waiting for which, to keep values requesting each other in a cycle from deadlocking.
 *
 * A thread-local stack of what is being computed, which is how the platform guards against recursion, does not work
 * here: a computation that is joined rather than started is not on the stack of whoever joins it. Two computations
 * started independently and then requesting each other's value would each see a computation they know nothing about,
 * and wait forever.
 */
private object WaitForGraph {

    /**
     * Runs [action], during which [from] is waiting for [to], whether by waiting for somebody else computing it or by
     * computing it itself. [from] is `null` if the request is not made by the computation of a cached value, which
     * nobody can be waiting for and hence is never part of a cycle.
     *
     * If [to] is, directly or through other values, already waiting for the value [from] computes, [action] is not run.
     * Every computation on the cycle whose value says what it is on a cycle is cancelled to yield just that. The
     * request is answered by [onCycle] if [to] is such a value, called with what the values on the cycle depend on,
     * and fails with [CyclicCachedValueDependencyException] otherwise.
     *
     * A computation may wait for the same value any number of times at once, its coroutines being parallel, and is
     * waiting for it until the last of them is done. Inlined for [action] to be able to suspend.
     */
    inline fun <R> waiting(
        from: Computation?,
        to: SuspendingCachedValue<*>,
        onCycle: (Set<Dependency>) -> R,
        action: () -> R,
    ): R {
        if (from == null) return action()

        addEdge(from, to)?.let { return onCycle(it) }
        try {
            return action()
        } finally {
            removeEdge(from, to)
        }
    }

    /**
     * Returns `null` if the edge was added, and what the values on the cycle it would have closed depend on otherwise.
     */
    @Synchronized
    fun addEdge(from: Computation, to: SuspendingCachedValue<*>): Set<Dependency>? {
        // Checking and adding being atomic makes it impossible for a cycle to ever make it into the graph.
        val path = pathBetween(to, from.owner)
        if (path == null) {
            from.waitingFor.merge(to, 1, Int::plus)
            return null
        }

        // All of them requested the next one because of what they had depended on by then. These are the dependencies
        // to change for the cycle to be gone, whatever else the computations would have come to depend on.
        val computations = path.mapNotNull { if (it === from.owner) from else it.computation }
        val dependencies = computations.flatMapTo(LinkedHashSet()) { it.dependenciesSoFar() }
        computations.forEach { if (it.owner.onCycle != null) it.cancelForCycle(dependencies) }

        if (to.onCycle != null) return dependencies
        throw CyclicCachedValueDependencyException((listOf(from.owner) + path).map { it.name })
    }

    @Synchronized
    fun removeEdge(from: Computation, to: SuspendingCachedValue<*>) {
        from.waitingFor.computeIfPresent(to) { _, count -> if (count == 1) null else count - 1 }
    }

    /**
     * Not recursive, as values waiting for each other is exactly where the stack may not be deep enough.
     *
     * Linear in the number of values and edges reachable from [from], which at worst is everything being computed.
     * Usually O(1): [from] is the value requested, which is waiting for nothing unless it is being computed already.
     */
    private fun pathBetween(
        from: SuspendingCachedValue<*>,
        to: SuspendingCachedValue<*>,
    ): List<SuspendingCachedValue<*>>? {
        val reachedFrom = HashMap<SuspendingCachedValue<*>, SuspendingCachedValue<*>>()
        val visited = hashSetOf(from)
        val stack = mutableListOf(from)
        while (stack.isNotEmpty()) {
            val value = stack.removeLast()
            if (value === to) return generateSequence(value) { reachedFrom[it] }.toList().asReversed()

            value.computation?.waitingFor?.keys?.forEach {
                if (!visited.add(it)) return@forEach
                reachedFrom[it] = value
                stack.add(it)
            }
        }
        return null
    }
}

/**
 * The [Computation] the current thread is running the provider of, for the benefit of code that cannot access the
 * coroutine context because it does not suspend. Maintained by [Computation] being a [ThreadContextElement].
 */
private val currentComputation = ThreadLocal<Computation?>()

/**
 * A provider being run, collecting the dependencies it declares. Being part of the coroutine context of the provider is
 * what makes requesting other cached values from within it discoverable.
 */
internal class Computation(val owner: SuspendingCachedValue<*>) : SuspendingCachedValueScope,
    ThreadContextElement<Computation?> {

    companion object Key : CoroutineContext.Key<Computation>

    override val key: CoroutineContext.Key<*> get() = Key

    /**
     * The one the provider is running with, this being part of it.
     */
    override lateinit var coroutineContext: CoroutineContext

    private val dependencies = LinkedHashSet<Dependency>()

    /**
     * Values the provider is waiting for, and how many of its coroutines are. Guarded by [WaitForGraph].
     */
    val waitingFor = HashMap<SuspendingCachedValue<*>, Int>()

    /**
     * What the values of the cycles the provider was cancelled for depend on, or `null` if it was not. Written with
     * [WaitForGraph] held.
     */
    @Volatile
    var cycleDependencies: Set<Dependency>? = null
        private set

    override fun updateThreadContext(context: CoroutineContext): Computation? =
        currentComputation.get().also { currentComputation.set(this) }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Computation?) =
        currentComputation.set(oldState)

    private fun add(dependency: Dependency) {
        synchronized(dependencies) { dependencies.add(dependency) }
    }

    override fun dependsOn(vararg trackers: ModificationTracker) =
        trackers.forEach { add(Dependency(it) { it.modificationCount }) }

    override fun dependsOn(file: VirtualFile) = add(Dependency(file) { file.modificationStamp })

    override fun dependsOn(document: Document) = add(Dependency(document) { document.modificationStamp })

    override fun dependsOn(element: PsiElement) = add(Dependency(element) {
        if (!element.isValid) return@Dependency null

        val file = if (element is PsiDirectory) null else element.containingFile
        file?.modificationStamp ?: PsiModificationTracker.getInstance(element.project).modificationCount
    })

    /**
     * Called when the provider was handed [data] of another cached value, making its value depend on the same things.
     *
     * Its dependencies rather than the value itself: checking a value computed from a value computed from a value would
     * otherwise recurse just as deep as computing them did, and keep all of them alive.
     */
    fun consume(data: Data<*>) {
        synchronized(dependencies) { dependencies.addAll(data.dependencies) }
    }

    fun dependenciesSoFar(): Set<Dependency> = synchronized(dependencies) { LinkedHashSet(dependencies) }

    fun <T> seal(value: T): Data<T> = Data(value, dependenciesSoFar())

    /**
     * Cancels the provider for its value to be what it is on a cycle instead, depending on [dependencies]. Only ever
     * called on a provider waiting for or requesting a value, and hence running.
     */
    fun cancelForCycle(dependencies: Set<Dependency>) {
        cycleDependencies = cycleDependencies.orEmpty() + dependencies
        coroutineContext.cancel()
    }
}
