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
 * [name] identifies the value in diagnostics.
 *
 * @see getSuspendingCachedValue
 */
class SuspendingCachedValue<T>(
    private val name: String = "<anonymous>",
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
            val outcome = (if (isOurs) compute(pending) else pending.await()) ?: continue
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
        try {
            val computation = Computation()
            outcome = try {
                // The request may be running anywhere, while a provider wants to be where its coroutines are parallel.
                val value = withContext(Dispatchers.Default + computation) {
                    computation.coroutineContext = coroutineContext
                    computation.provider()
                }
                Result.success(computation.seal(value))
            } catch (e: Throwable) {
                // A cancellation exception despite not being cancelled is a failure of the provider like any other,
                // e.g. a timeout.
                if (e is CancellationException && !currentCoroutineContext().isActive) throw e
                Result.failure(e)
            }
            return outcome
        } finally {
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
 * The [Computation] the current thread is running the provider of, for the benefit of code that cannot access the
 * coroutine context because it does not suspend. Maintained by [Computation] being a [ThreadContextElement].
 */
private val currentComputation = ThreadLocal<Computation?>()

/**
 * A provider being run, collecting the dependencies it declares. Being part of the coroutine context of the provider is
 * what makes requesting other cached values from within it discoverable.
 */
internal class Computation : SuspendingCachedValueScope, ThreadContextElement<Computation?> {

    companion object Key : CoroutineContext.Key<Computation>

    override val key: CoroutineContext.Key<*> get() = Key

    /**
     * The one the provider is running with, this being part of it.
     */
    override lateinit var coroutineContext: CoroutineContext

    private val dependencies = LinkedHashSet<Dependency>()

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

    fun <T> seal(value: T): Data<T> = Data(value, synchronized(dependencies) { LinkedHashSet(dependencies) })
}
