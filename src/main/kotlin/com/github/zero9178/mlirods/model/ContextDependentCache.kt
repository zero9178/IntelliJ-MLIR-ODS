package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.cache.SuspendingCachedValueScope
import com.github.zero9178.mlirods.cache.suspendingCachedValue
import com.github.zero9178.mlirods.cache.suspendingCachedValueKeyOf
import com.github.zero9178.mlirods.language.TableGenLanguage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ParameterizedCachedValue
import com.intellij.psi.util.PsiModificationTracker
import java.util.concurrent.ConcurrentHashMap

/**
 * One [Key] per [provider] class so that distinct call sites get their own cached-value slot on an element, mirroring
 * how [CachedValuesManager.getProjectPsiDependentCache] derives its key from the provider class.
 */
private val keyForProviderClass = ConcurrentHashMap<Class<*>, Key<ParameterizedCachedValue<*, *>>>()

@Suppress("UNCHECKED_CAST")
private fun <T, P> cacheKeyFor(providerClass: Class<*>): Key<ParameterizedCachedValue<T, P>> =
    keyForProviderClass.computeIfAbsent(providerClass) {
        Key.create("TableGenContextDependentCache#${it.name}")
    } as Key<ParameterizedCachedValue<T, P>>

/**
 * What everything cached by [getProjectContextDependentCache] depends on: the TableGen PSI and the include graph of
 * [project].
 */
private fun projectContextDependencies(project: Project): Array<Any> = arrayOf(
    PsiModificationTracker.getInstance(project).forLanguage(TableGenLanguage.INSTANCE),
    project.service<TableGenIncludeGraphService>().graphChangedModificationTracker,
)

/**
 * The slots of the keyed caches [element] has for the call site [providerClass] identifies, one per key. The map is
 * dropped along with everything in it whenever what everything cached here depends on changes.
 */
private fun <S, P : PsiElement, K : Any> slotsOf(element: P, providerClass: Class<*>): ConcurrentHashMap<K, S> =
    CachedValuesManager.getManager(element.project).getParameterizedCachedValue(
        element,
        cacheKeyFor<ConcurrentHashMap<K, S>, P>(providerClass),
        { param: P ->
            CachedValueProvider.Result.create(ConcurrentHashMap<K, S>(), *projectContextDependencies(param.project))
        },
        false,
        element,
    )

/**
 * Context-aware replacement for [CachedValuesManager.getProjectPsiDependentCache], for a value that depends on a [key]
 * besides [element], typically the [TableGenCompilationContext] the value is computed in or a
 * [TableGenEvaluationContext][com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext] carrying one.
 *
 * Like the platform helper, the value computed by [provider] is recomputed on PSI change. Unlike the platform helper it
 * only tracks changes to TableGen PSI ([PsiModificationTracker.forLanguage]) rather than any language, as nothing
 * cached here depends on the PSI of other languages. In addition, it is invalidated whenever the include graph changes
 * (see [TableGenIncludeGraphService.graphChangedModificationTracker]).
 *
 * Almost every cross-file lookup in this project depends on these: resolution happens relative to a compilation
 * context (which files it pastes in, in which order, ...), and that context may change without any PSI edit — e.g.
 * because the compile commands changed, or a TableGen file was added or removed. A plain
 * [CachedValuesManager.getProjectPsiDependentCache] would keep serving stale results in those cases.
 *
 * The value is cached per [element] and [key], so that a computation in another context neither finds nor overwrites
 * it. Keys must have value equality, as it is what lets a context obtained anew hit what was cached under an equal one.
 * [provider] is only ever run for [key] and may therefore capture it.
 */
fun <T, P : PsiElement, K : Any> getProjectContextDependentCache(element: P, key: K, provider: (P) -> T): T {
    val manager = CachedValuesManager.getManager(element.project)
    // Only the slot is created here, never the value: 'computeIfAbsent' must not re-enter the map, which a computation
    // reaching the same element and key through a cycle would do.
    return slotsOf<CachedValue<T>, P, K>(element, provider.javaClass).computeIfAbsent(key) {
        manager.createCachedValue {
            CachedValueProvider.Result.create(provider(element), *projectContextDependencies(element.project))
        }
    }.value
}

/**
 * Makes the value being computed depend on what everything cached by [getProjectContextDependentCache] depends on: the
 * TableGen PSI and the include graph of [project].
 */
fun SuspendingCachedValueScope.dependsOnProjectContext(project: Project) = dependsOn(
    PsiModificationTracker.getInstance(project).forLanguage(TableGenLanguage.INSTANCE),
    project.service<TableGenIncludeGraphService>().graphChangedModificationTracker,
)

/**
 * Suspending counterpart of [getProjectContextDependentCache] for a value that depends on nothing but [element]:
 * [provider] is a coroutine, and is run once no matter how many threads request the value while it does. See
 * [SuspendingCachedValue] for what that is good for and how to call it.
 *
 * Returns the cached value rather than what it computes, for the caller to choose between
 * [SuspendingCachedValue.await] and [SuspendingCachedValue.getBlocking].
 */
fun <T, P : PsiElement> projectContextDependentSuspendingCachedValue(
    element: P,
    provider: suspend SuspendingCachedValueScope.(P) -> T,
): SuspendingCachedValue<T> = element.suspendingCachedValue(suspendingCachedValueKeyOf<T>(provider)) { param ->
    dependsOnProjectContext(param.project)
    provider(param)
}

/**
 * Suspending counterpart of [getProjectContextDependentCache]: [provider] is a coroutine, cached per [element] and
 * [key] like there, and run once no matter how many threads request the value while it does. See
 * [SuspendingCachedValue] for what that is good for and how to call it.
 *
 * Returns the cached value rather than what it computes, for the caller to choose between
 * [SuspendingCachedValue.await] and [SuspendingCachedValue.getBlocking]. [what] names the value in diagnostics, and
 * [onCycle] is what the value is when requested in a cycle, see [SuspendingCachedValue].
 */
fun <T, P : PsiElement, K : Any> projectContextDependentSuspendingCachedValue(
    element: P,
    key: K,
    what: String,
    onCycle: (() -> T)? = null,
    provider: suspend SuspendingCachedValueScope.(P) -> T,
): SuspendingCachedValue<T> =
    // Creating the map of slots neither suspends nor takes part in a cycle, which is why a platform cached value holds
    // it rather than another suspending one.
    slotsOf<SuspendingCachedValue<T>, P, K>(element, provider.javaClass).computeIfAbsent(key) {
        // Identified by what is at hand without loading the tree of a stubbed element.
        SuspendingCachedValue(
            "$what of ${element.javaClass.simpleName}@${System.identityHashCode(element)} in $key", onCycle,
        ) {
            dependsOnProjectContext(element.project)
            provider(element)
        }
    }

/**
 * Requests the value of [projectContextDependentSuspendingCachedValue].
 */
suspend fun <T, P : PsiElement> getProjectContextDependentCacheSuspending(
    element: P,
    provider: suspend SuspendingCachedValueScope.(P) -> T,
): T = projectContextDependentSuspendingCachedValue(element, provider = provider).await()
