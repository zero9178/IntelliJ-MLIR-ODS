package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.language.TableGenLanguage
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
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

/**
 * Context-aware replacement for [CachedValuesManager.getProjectPsiDependentCache].
 *
 * Like the platform helper, the value computed by [provider] is cached per [element] and recomputed on PSI change.
 * Unlike the platform helper it only tracks changes to TableGen PSI ([PsiModificationTracker.forLanguage]) rather than
 * any language, as nothing cached here depends on the PSI of other languages. In addition, it is invalidated whenever
 * the propagated [TableGenContext] of any file changes (see [TableGenContextService.contextChangedModificationTracker]).
 *
 * Almost every cross-file lookup in this project depends on these: resolution happens relative to the active context of
 * a file (which files it includes, which defines are active, ...), and that context may change without any PSI edit —
 * e.g. because the compile commands changed, or a TableGen file was added or removed. A plain
 * [CachedValuesManager.getProjectPsiDependentCache] would keep serving stale results in those cases.
 */
fun <T, P : PsiElement> getProjectContextDependentCache(element: P, provider: (P) -> T): T {
    @Suppress("UNCHECKED_CAST")
    val key = keyForProviderClass.computeIfAbsent(provider.javaClass) {
        Key.create("TableGenContextDependentCache#${it.name}")
    } as Key<ParameterizedCachedValue<T, P>>

    return CachedValuesManager.getManager(element.project).getParameterizedCachedValue(
        element,
        key,
        { param: P ->
            val service = param.project.service<TableGenContextService>()
            val tableGenPsiTracker =
                PsiModificationTracker.getInstance(param.project).forLanguage(TableGenLanguage.INSTANCE)
            CachedValueProvider.Result.create(
                provider(param),
                tableGenPsiTracker,
                service.contextChangedModificationTracker,
            )
        },
        false,
        element,
    )
}
