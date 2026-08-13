package com.ascend.core.domain.community

import com.ascend.core.common.LOCAL_USER_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Resolves which identity an operation should use — the crux of the "link, not migration" strategy.
 *
 * Local data (workouts, progression, Build) is ALWAYS keyed by [localUserId] and that key never
 * changes, so signing in can never orphan existing history and never-signing-in loses nothing.
 * Only inherently cross-user operations use [remoteUserId], which is null until the player links a
 * remote account.
 */
interface IdentityResolver {
    /** The permanent local Room key. Never changes, whether or not the player ever signs in. */
    fun localUserId(): String

    /** The linked remote id when signed in, else null. */
    fun remoteUserId(): Flow<RemoteUserId?>

    /** A one-shot read of the current remote id (null when signed out). */
    suspend fun currentRemoteUserId(): RemoteUserId?

    fun isSignedIn(): Flow<Boolean>
}

/**
 * Default resolver: local id is the constant [LOCAL_USER_ID]; remote id is whatever link the
 * [SessionStore] currently holds. Pure and fully testable without the SDK.
 */
class DefaultIdentityResolver
    @Inject
    constructor(
        private val sessionStore: SessionStore,
    ) : IdentityResolver {
        override fun localUserId(): String = LOCAL_USER_ID

        override fun remoteUserId(): Flow<RemoteUserId?> = sessionStore.linkedRemoteUserId()

        override suspend fun currentRemoteUserId(): RemoteUserId? = sessionStore.linkedRemoteUserId().first()

        override fun isSignedIn(): Flow<Boolean> = sessionStore.linkedRemoteUserId().map { it != null }
    }
