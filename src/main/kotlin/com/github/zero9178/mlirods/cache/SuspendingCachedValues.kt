package com.github.zero9178.mlirods.cache

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderEx
import java.util.concurrent.ConcurrentHashMap

/**
 * One [Key] per provider class so that distinct call sites get their own slot on a holder, mirroring how
 * [com.intellij.psi.util.CachedValuesManager.getCachedValue] derives its key.
 */
private val keyForProviderClass = ConcurrentHashMap<Class<*>, Key<SuspendingCachedValue<*>>>()

/**
 * Returns the key identifying the call site [provider] was written at.
 */
@Suppress("UNCHECKED_CAST")
fun <T> suspendingCachedValueKeyOf(provider: Any): Key<SuspendingCachedValue<T>> =
    keyForProviderClass.computeIfAbsent(provider.javaClass) {
        Key.create("SuspendingCachedValue#${it.name}")
    } as Key<SuspendingCachedValue<T>>

/**
 * Returns the [SuspendingCachedValue] stored in the user data of the receiver under [key], creating it with [provider]
 * if there is none yet.
 *
 * Use this over [getSuspendingCachedValue] to get access to more than [SuspendingCachedValue.await], e.g. to offer the
 * value to blocking callers as well.
 *
 * [provider] is handed the receiver so that it does not need to capture it. Like with the platform's cached values it
 * should not capture anything else either: the provider of the first call is the one kept for all subsequent
 * computations, no matter what later calls captured.
 */
fun <H : UserDataHolder, T> H.suspendingCachedValue(
    key: Key<SuspendingCachedValue<T>>,
    provider: suspend SuspendingCachedValueScope.(H) -> T,
): SuspendingCachedValue<T> {
    getUserData(key)?.let { return it }

    val holder = this
    val created = SuspendingCachedValue(key.toString()) { provider(holder) }
    if (holder is UserDataHolderEx) return holder.putUserDataIfAbsent(key, created)

    return synchronized(holder) {
        getUserData(key) ?: created.also { putUserData(key, it) }
    }
}

/**
 * Returns the value computed by [provider], cached in the user data of the receiver under a key unique to the call
 * site. This is the counterpart of [com.intellij.psi.util.CachedValuesManager.getCachedValue].
 *
 * @see suspendingCachedValue
 */
suspend fun <H : UserDataHolder, T> H.getSuspendingCachedValue(
    provider: suspend SuspendingCachedValueScope.(H) -> T,
): T = suspendingCachedValue(suspendingCachedValueKeyOf(provider), provider).await()
