package com.github.zero9178.mlirods

import com.intellij.openapi.diagnostic.ControlFlowException
import kotlinx.coroutines.CancellationException

/**
 * Rethrows [e] if it is thrown to unwind rather than to report a failure, i.e. cancellation of the current coroutine or
 * progress indicator. Such an exception must never be logged or swallowed as it is how the platform asks the work to
 * stop.
 */
fun rethrowIfControlFlow(e: Throwable) {
    if (e is ControlFlowException || e is CancellationException) throw e
}
