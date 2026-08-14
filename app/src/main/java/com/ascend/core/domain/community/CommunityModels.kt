package com.ascend.core.domain.community

/**
 * A remote (Supabase) user id — deliberately a distinct type from the local Room user id
 * ([com.ascend.core.common.LOCAL_USER_ID]). Local data is never re-keyed to this; the two identities
 * are resolved independently so a player can play fully local-only forever and, if they choose to sign
 * in, link a remote identity without any risk to existing history.
 */
@JvmInline
value class RemoteUserId(val value: String)

/** The authenticated identity state. Signed-out is a complete, first-class state, not a degraded one. */
sealed interface AuthState {
    data object SignedOut : AuthState

    data class SignedIn(
        val userId: RemoteUserId,
        val emailVerified: Boolean,
    ) : AuthState
}

/** A player's minimal public-facing remote profile — identity only, never training data. */
data class RemoteProfile(
    val userId: RemoteUserId,
    val handle: String?,
    val displayName: String?,
)

/**
 * The outcome of a remote operation, made explicit so feature code can never silently assume
 * connectivity or success. Offline is separated from failure so the UI degrades gracefully without
 * treating "no network" as an error.
 */
sealed interface RemoteResult<out T> {
    data class Success<out T>(val value: T) : RemoteResult<T>

    data object Offline : RemoteResult<Nothing>

    data class Failure(
        val kind: RemoteErrorKind,
        val message: String? = null,
    ) : RemoteResult<Nothing>
}

/** Coarse, provider-agnostic error categories — no Supabase types leak through. */
enum class RemoteErrorKind {
    AUTH,
    RATE_LIMITED,
    EMAIL_UNVERIFIED,
    NOT_FOUND,
    CONFLICT,
    UNKNOWN,
}
