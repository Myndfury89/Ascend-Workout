package com.ascend.core.domain.community

import kotlinx.coroutines.flow.Flow

/**
 * Authentication against the remote identity provider. This interface is the seam that keeps the
 * Supabase SDK out of feature/domain code (pt 11): the only implementation lives in the data layer and
 * wraps the SDK; everything above depends on this abstraction. Auth is always optional — no method
 * here is required for the single-player loop to function.
 */
interface AuthGateway {
    /** Observes the current identity state. Emits [AuthState.SignedOut] when no session exists. */
    fun authState(): Flow<AuthState>

    suspend fun currentUserId(): RemoteUserId?

    suspend fun signUp(
        email: String,
        password: String,
    ): RemoteResult<AuthState>

    suspend fun signIn(
        email: String,
        password: String,
    ): RemoteResult<AuthState>

    suspend fun signOut()
}

/** The player's own remote profile (identity only). Cross-user reads arrive with the P2 consent model. */
interface ProfileGateway {
    suspend fun fetchOwnProfile(): RemoteResult<RemoteProfile?>

    suspend fun upsertOwnProfile(
        handle: String?,
        displayName: String?,
    ): RemoteResult<RemoteProfile>
}

/**
 * Persists the local link to a remote identity. This is a purely local concern (the association
 * between this device's player and the signed-in remote user); it holds no remote/SDK types and is
 * what lets the identity be a link rather than a data migration.
 */
interface SessionStore {
    fun linkedRemoteUserId(): Flow<RemoteUserId?>

    suspend fun setLinkedRemoteUserId(id: RemoteUserId?)
}
