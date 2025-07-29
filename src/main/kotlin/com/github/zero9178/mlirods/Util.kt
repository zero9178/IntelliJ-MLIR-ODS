package com.github.zero9178.mlirods

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import kotlin.reflect.KProperty

fun <T, R> List<CachedValue<T>>.merge(context: PsiElement, function: (List<T>) -> R): CachedValue<R> =
    getCachedValue(context) {
        CachedValueProvider.Result.create(function.invoke(this.map {
            it.value
        }), this.ifEmpty { listOf(ModificationTracker.NEVER_CHANGED) })
    }

operator fun <T> CachedValue<T>.getValue(receiver: Any?, property: KProperty<*>): T {
    return value
}

/**
 * Create a cached value with the given provider and non-tracked return value, store it in PSI element's user data. If it's already stored, reuse it.
 * The passed cached value provider may only depend on the passed context PSI element and project/application components/services,
 * see [CachedValue] documentation for more details.
 * @return The cached value
 */
fun <T> getCachedValue(context: PsiElement, provider: CachedValueProvider<T>): CachedValue<T> {
    val manager = CachedValuesManager.getManager(context.project)
    return getCachedValue<T>(
        context,
        manager.getKeyForClass<T>(provider.javaClass),
        provider
    )
}

/**
 * Create a cached value with the given provider and non-tracked return value, store it in PSI element's user data. If it's already stored, reuse it.
 * The passed cached value provider may only depend on the passed context PSI element and project/application components/services,
 * see [CachedValue] documentation for more details.
 *
 * @return The cached value
 */
fun <T> getCachedValue(
    context: PsiElement,
    key: Key<CachedValue<T>>,
    provider: CachedValueProvider<T>
): CachedValue<T> {
    var value = context.getUserData(key)
    if (value != null) {
        return value
    }

    val manager = CachedValuesManager.getManager(context.project)
    if (context is UserDataHolderEx) {
        value = manager.createCachedValue(context, provider, false)
        value = context.putUserDataIfAbsent(key, value)
    } else {
        value = synchronized(context) {
            value = manager.createCachedValue(context, provider, false)
            context.putUserData(key, value)
            value
        }
    }
    return value
}