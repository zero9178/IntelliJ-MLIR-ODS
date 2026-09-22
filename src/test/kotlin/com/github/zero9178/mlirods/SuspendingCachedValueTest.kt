package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.cache.CyclicCachedValueDependencyException
import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.cache.SuspendingCachedValueScope
import com.github.zero9178.mlirods.cache.getSuspendingCachedValue
import com.github.zero9178.mlirods.cache.suspendingCachedValue
import com.github.zero9178.mlirods.model.getProjectContextDependentCacheSuspending
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.util.concurrency.ThreadingAssertions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SuspendingCachedValueTest : BasePlatformTestCase() {

    // Waiting on the EDT for coroutines is a deadlock in waiting, and nothing here needs it.
    override fun runInDispatchThread() = false

    /**
     * Runs [action] the way values are requested: as a coroutine that a caller with read access is blocked on.
     */
    private fun test(action: suspend CoroutineScope.() -> Unit) {
        // Explicitly of type 'Unit', or what is returned makes JUnit not consider the callers tests.
        ReadAction.computeBlocking(ThrowableComputable<Unit, RuntimeException> {
            ProgressManager.getInstance().runProcess({
                runBlockingCancellable {
                    withTimeout(30.seconds) { action() }
                }
            }, EmptyProgressIndicator())
        })
    }

    private fun <T> cachedValue(
        name: String = "test",
        onCycle: (() -> T)? = null,
        provider: suspend SuspendingCachedValueScope.() -> T,
    ) = SuspendingCachedValue(name, onCycle, provider)

    /**
     * Requests [value] and only returns once the request is suspended waiting for the computation.
     */
    private fun <T> CoroutineScope.request(value: SuspendingCachedValue<T>) =
        async(start = CoroutineStart.UNDISPATCHED) { value.await() }

    fun `test value is cached until a dependency changes`() = test {
        val tracker = SimpleModificationTracker()
        val runs = AtomicInteger()
        val value = cachedValue {
            dependsOn(tracker)
            runs.incrementAndGet()
        }

        assertEquals(1, value.await())
        assertEquals(1, value.await())

        tracker.incModificationCount()
        assertEquals(2, value.await())
        assertEquals(2, value.await())
    }

    fun `test value without dependencies is cached forever`() = test {
        val runs = AtomicInteger()
        val value = cachedValue { runs.incrementAndGet() }

        assertEquals(1, value.await())
        assertEquals(1, value.await())
    }

    fun `test launched requests nest without overflowing the stack`() = test {
        val tracker = SimpleModificationTracker()
        val runs = AtomicInteger()
        val values = ArrayList<SuspendingCachedValue<Int>>()
        repeat(20_000) { i ->
            values += cachedValue("value $i") {
                dependsOn(tracker)
                runs.incrementAndGet()
                // Computed from the bottom of a stack rather than on top of ours, which is up to the provider to do.
                if (i == 0) 0 else async { values[i - 1].await() }.await() + 1
            }
        }

        assertEquals(values.lastIndex, values.last().await())
        // Neither does telling whether a value computed from that many others is up to date.
        assertEquals(values.lastIndex, values.last().await())
        assertEquals(values.size, runs.get())

        tracker.incModificationCount()
        assertEquals(values.lastIndex, values.last().await())
        assertEquals(2 * values.size, runs.get())
    }

    fun `test concurrent requests share one computation`() = test {
        val gate = CompletableDeferred<Unit>()
        val runs = AtomicInteger()
        val value = cachedValue {
            gate.await()
            runs.incrementAndGet()
        }

        val requests = List(50) { request(value) }
        gate.complete(Unit)

        assertEquals(List(50) { 1 }, requests.awaitAll())
        assertEquals(1, runs.get())
    }

    fun `test cancelling a waiting request does not affect the computation`() = test {
        val gate = CompletableDeferred<Unit>()
        val runs = AtomicInteger()
        val value = cachedValue {
            gate.await()
            runs.incrementAndGet()
        }

        val computing = request(value)
        val waiting = request(value)
        waiting.cancelAndJoin()
        gate.complete(Unit)

        assertEquals(1, computing.await())
        assertEquals(1, runs.get())
    }

    fun `test waiting request takes over from a cancelled computation`() = test {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val runs = AtomicInteger()
        val value = cachedValue {
            if (runs.incrementAndGet() > 1) return@cachedValue "second"

            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }

        val computing = request(value)
        val waiting = List(3) { request(value) }
        started.await()
        computing.cancelAndJoin()
        cancelled.await()

        // Only one of them took over, which the others wait for in turn.
        assertEquals(List(3) { "second" }, waiting.awaitAll())
        assertEquals(2, runs.get())
    }

    fun `test failure reaches every request and is not cached`() = test {
        val gate = CompletableDeferred<Unit>()
        val runs = AtomicInteger()
        val value = cachedValue {
            gate.await()
            if (runs.incrementAndGet() == 1) throw IllegalArgumentException("first run")
            "recovered"
        }

        supervisorScope {
            val requests = List(5) { request(value) }
            gate.complete(Unit)
            requests.forEach {
                val failure = runCatching { it.await() }.exceptionOrNull()
                assertInstanceOf(failure, IllegalArgumentException::class.java)
            }
        }
        assertEquals(1, runs.get())

        assertEquals("recovered", value.await())
    }

    fun `test cancellation exception of the provider is a failure`() = test {
        val value = cachedValue {
            withTimeout(1.milliseconds) { awaitCancellation() }
        }

        val failure = runCatching { value.await() }.exceptionOrNull()
        assertInstanceOf(failure, kotlinx.coroutines.TimeoutCancellationException::class.java)
    }

    fun `test ever changed is shared but not cached`() = test {
        val gate = CompletableDeferred<Unit>()
        val runs = AtomicInteger()
        val value = cachedValue {
            dependsOn(ModificationTracker.EVER_CHANGED)
            gate.await()
            runs.incrementAndGet()
        }

        val requests = List(5) { request(value) }
        gate.complete(Unit)
        assertEquals(List(5) { 1 }, requests.awaitAll())

        assertEquals(2, value.await())
        assertEquals(3, value.await())
    }

    fun `test dependencies are transitive`() = test {
        val tracker = SimpleModificationTracker()
        val innerRuns = AtomicInteger()
        val outerRuns = AtomicInteger()
        val inner = cachedValue("inner") {
            dependsOn(tracker)
            innerRuns.incrementAndGet()
        }
        val middle = cachedValue("middle") { inner.await() }
        val outer = cachedValue("outer") {
            outerRuns.incrementAndGet()
            middle.await() * 10
        }

        assertEquals(10, outer.await())
        assertEquals(10, outer.await())
        assertEquals(1, outerRuns.get())

        tracker.incModificationCount()
        assertEquals(20, outer.await())
        assertEquals(2, outerRuns.get())
        assertEquals(2, innerRuns.get())
    }

    fun `test requesting oneself fails`() = test {
        lateinit var value: SuspendingCachedValue<Int>
        value = cachedValue("self") { value.await() }

        val failure = runCatching { value.await() }.exceptionOrNull()
        assertInstanceOf(failure, CyclicCachedValueDependencyException::class.java)
        assertEquals("Cyclic dependency between cached values: self -> self", failure!!.message)
    }

    fun `test computations requesting each other fail`() = test {
        // Both computations are started independently, so that neither is a request made by the other: the cycle only
        // comes into being by each joining the computation of the other.
        val gate = CompletableDeferred<Unit>()
        lateinit var a: SuspendingCachedValue<Int>
        lateinit var b: SuspendingCachedValue<Int>
        a = cachedValue("a") {
            gate.await()
            b.await()
        }
        b = cachedValue("b") {
            gate.await()
            a.await()
        }

        supervisorScope {
            val requests = listOf(request(a), request(b))
            gate.complete(Unit)
            requests.forEach {
                val failure = runCatching { it.await() }.exceptionOrNull()
                assertInstanceOf(failure, CyclicCachedValueDependencyException::class.java)
            }
        }
    }

    fun `test requesting oneself yields what the value is on a cycle`() = test {
        val runs = AtomicInteger()
        val resumed = AtomicInteger()
        lateinit var value: SuspendingCachedValue<Int>
        value = cachedValue("self", onCycle = { -1 }) {
            runs.incrementAndGet()
            value.await().also { resumed.incrementAndGet() } + 1
        }

        // Not what the provider makes of it, which it does not get to.
        assertEquals(-1, value.await())
        assertEquals(-1, value.await())
        assertEquals(1, runs.get())
        assertEquals(0, resumed.get())
    }

    /**
     * Values requesting each other, neither of which makes what it is on a cycle out of the other being so.
     */
    private fun cyclicPair(): Pair<SuspendingCachedValue<Int>, SuspendingCachedValue<Int>> {
        lateinit var a: SuspendingCachedValue<Int>
        lateinit var b: SuspendingCachedValue<Int>
        a = cachedValue("a", onCycle = { -1 }) { b.await() + 10 }
        b = cachedValue("b", onCycle = { -2 }) { a.await() + 100 }
        return a to b
    }

    fun `test values on a cycle do not depend on the one requested first`() = test {
        cyclicPair().let { (a, b) ->
            assertEquals(-1, a.await())
            assertEquals(-2, b.await())
        }
        cyclicPair().let { (a, b) ->
            assertEquals(-2, b.await())
            assertEquals(-1, a.await())
        }
    }

    fun `test values on a cycle do not depend on the request closing it`() = test {
        val gate = CompletableDeferred<Unit>()
        lateinit var a: SuspendingCachedValue<Int>
        lateinit var b: SuspendingCachedValue<Int>
        a = cachedValue("a", onCycle = { -1 }) {
            gate.await()
            b.await() + 10
        }
        b = cachedValue("b", onCycle = { -2 }) {
            gate.await()
            a.await() + 100
        }

        val requests = listOf(request(a), request(b))
        gate.complete(Unit)
        assertEquals(listOf(-1, -2), requests.awaitAll())
    }

    fun `test value computed from a cycle`() = test {
        // Once with the cycle computed as part of it, and once with it computed already.
        for (first in listOf(true, false)) {
            val (a, b) = cyclicPair()
            val outside = cachedValue("outside") { a.await() * 2 }

            if (!first) assertEquals(-2, b.await())
            assertEquals(-2, outside.await())
            assertEquals(-2, b.await())
        }
    }

    fun `test value on a cycle not saying what it is then`() = test {
        for (failingFirst in listOf(true, false)) {
            lateinit var failing: SuspendingCachedValue<Int>
            lateinit var recovering: SuspendingCachedValue<Int>
            failing = cachedValue("failing") { recovering.await() + 10 }
            recovering = cachedValue("recovering", onCycle = { -1 }) { failing.await() + 100 }

            if (failingFirst) assertEquals(9, failing.await())
            assertEquals(-1, recovering.await())
            assertEquals(9, failing.await())
        }
    }

    fun `test value on a cycle depends on what every value on it depends on`() = test {
        val tracker = SimpleModificationTracker()
        var cyclic = true
        lateinit var a: SuspendingCachedValue<Int>
        lateinit var b: SuspendingCachedValue<Int>
        a = cachedValue("a", onCycle = { -1 }) { b.await() + 10 }
        b = cachedValue("b", onCycle = { -2 }) {
            dependsOn(tracker)
            if (cyclic) a.await() else 7
        }

        assertEquals(-1, a.await())

        // 'a' was cancelled before it got anything from 'b', let alone its dependencies.
        cyclic = false
        tracker.incModificationCount()
        assertEquals(17, a.await())
    }

    fun `test sharing a dependency is not a cycle`() = test {
        val shared = cachedValue("shared") { 1 }
        val left = cachedValue("left") { shared.await() }
        val right = cachedValue("right") { shared.await() }
        val top = cachedValue("top") {
            val l = async { left.await() }
            val r = async { right.await() }
            l.await() + r.await() + shared.await()
        }

        assertEquals(3, top.await())
    }

    /**
     * A value whose computation needs read access on more than the thread requesting it.
     */
    private fun valueReadingInParallel() = cachedValue {
        List(2) {
            async {
                ThreadingAssertions.assertReadAccess()
                1
            }
        }.awaitAll().sum()
    }

    // The callers below are not what 'getBlocking' asks for, but are what the platform may be. None of them is within
    // 'test', which is a caller as asked for.

    fun `test blocking request without anything cancelling it`() {
        val value = valueReadingInParallel()
        assertEquals(2, ReadAction.computeBlocking<Int, RuntimeException> { value.getBlocking() })
    }

    fun `test blocking request with the read access of the write intent lock`() {
        val value = valueReadingInParallel()
        assertEquals(2, runInEdtAndGet {
            assertTrue(ApplicationManager.getApplication().isWriteIntentLockAcquired)
            value.getBlocking()
        })
    }

    fun `test blocking request within a write action`() {
        val value = valueReadingInParallel()
        assertEquals(2, WriteAction.computeAndWait<Int, RuntimeException> { value.getBlocking() })
    }

    fun `test user data caches per holder and call site`() = test {
        val runs = AtomicInteger()
        suspend fun first(holder: UserDataHolderBase) = holder.getSuspendingCachedValue { runs.incrementAndGet() }
        suspend fun second(holder: UserDataHolderBase) = holder.getSuspendingCachedValue { -runs.incrementAndGet() }

        val holder = UserDataHolderBase()
        assertEquals(1, first(holder))
        assertEquals(1, first(holder))
        assertEquals(-2, second(holder))
        assertEquals(3, first(UserDataHolderBase()))
        assertEquals(1, first(holder))
    }

    fun `test user data value is handed its holder`() = test {
        val key = Key.create<SuspendingCachedValue<Any>>("test")
        val holder = UserDataHolderBase()

        val value = holder.suspendingCachedValue(key) { it }
        assertSame(holder, value.await())
        assertSame(value, holder.suspendingCachedValue(key) { error("There is a value already") })
    }

    // The two below are no coroutines as a whole as they write in between, which a test with read access cannot. Their
    // parts are functions of their own as JUnit takes the lambdas of a method called 'test...' for malformed tests.

    fun `test element dependency follows its file`() {
        val file = myFixture.configureByText("test.td", "class A;")
        val other = myFixture.addFileToProject("other.td", "class B;")
        val runs = AtomicInteger()
        suspend fun text() = file.getSuspendingCachedValue { element ->
            dependsOn(element)
            runs.incrementAndGet()
            // No read action needed: the provider runs with the read access of whoever is blocked on it.
            element.text
        }

        test { assertEquals("class A;", text()) }
        append(other, "class C;")
        test { assertEquals("class A;", text()) }
        assertEquals(1, runs.get())

        append(file, "class D;")
        test { assertEquals("class A;class D;", text()) }
        assertEquals(2, runs.get())
    }

    fun `test context dependent cache follows psi changes`() {
        val file = myFixture.configureByText("test.td", "class A;")
        val runs = AtomicInteger()
        suspend fun text() = getProjectContextDependentCacheSuspending(file) { element ->
            runs.incrementAndGet()
            element.text
        }

        test { assertEquals("class A;", text()) }
        test { assertEquals("class A;", text()) }
        assertEquals(1, runs.get())

        append(file, "class B;")
        test { assertEquals("class A;class B;", text()) }
        assertEquals(2, runs.get())
    }

    private fun append(file: PsiFile, text: String) = WriteCommandAction.runWriteCommandAction(project) {
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        document.insertString(document.textLength, text)
        PsiDocumentManager.getInstance(project).commitDocument(document)
    }
}
