package com.ascend.core.domain.community

/** A friendship row's id. */
@JvmInline
value class FriendshipId(val value: String)

enum class FriendshipStatus { PENDING, ACCEPTED }

/**
 * A friendship edge as seen by the current user. [incoming] is true when the other user sent the
 * request to me (so I may accept); false when I sent it (I may cancel).
 */
data class FriendEdge(
    val id: FriendshipId,
    val other: RemoteUserId,
    val status: FriendshipStatus,
    val incoming: Boolean,
)

/** Minimal discovery card from a handle lookup — safe to show a non-friend. No progression/build data. */
data class ProfileCard(
    val userId: RemoteUserId,
    val handle: String?,
    val displayName: String?,
    val avatarBodyBase: String?,
)

/** One shared class-affinity entry (dominantEligible only, by construction upstream). */
data class SharedAffinity(
    val classId: String,
    val affinity: Double,
)

/**
 * The derived, presentation-only snapshot a friend may view. Contains no raw evidence, no Build
 * Characteristic scores/coverage, and no health data — those are never placed here. Nullable fields
 * reflect what the owner consented to share (see [SharePolicy]).
 */
data class SharedProfile(
    val userId: RemoteUserId,
    val selectedClass: String? = null,
    val classLevel: Int? = null,
    val rank: String? = null,
    val overallLevel: Int? = null,
    val buildIdentity: String? = null,
    val topAffinities: List<SharedAffinity> = emptyList(),
    val avatarBodyBase: String? = null,
)

/** v1 caps sharing at FRIENDS; PUBLIC is deferred and intentionally not modelled here. */
enum class ShareVisibility { PRIVATE, FRIENDS }

/** A user's consent settings for what friends may see. */
data class ShareSettings(
    val visibility: ShareVisibility = ShareVisibility.PRIVATE,
    val shareBuildIdentity: Boolean = false,
    val shareClassProgress: Boolean = false,
) {
    companion object {
        val PRIVATE_DEFAULT = ShareSettings()
    }
}

enum class ReportReason { HARASSMENT, SPAM, INAPPROPRIATE_NAME, OTHER }

/**
 * The current user's relationship to another user, modelled explicitly rather than as overlapping
 * booleans. A block always supersedes any friendship/request (see [RelationshipResolver]).
 */
sealed interface RelationshipState {
    /** No relationship. */
    data object None : RelationshipState

    /** I sent a request that is still pending (I may cancel). */
    data object OutgoingRequest : RelationshipState

    /** They sent me a request that is still pending (I may accept/decline). */
    data object IncomingRequest : RelationshipState

    /** Accepted friends. */
    data object Friends : RelationshipState

    /** I have blocked them. */
    data object Blocked : RelationshipState

    /** They have blocked me — inaccessible. */
    data object BlockedBy : RelationshipState
}

/**
 * Resolves the unambiguous [RelationshipState] from raw flags, with block taking precedence over any
 * friendship or pending request. Pure and deterministic so the rule lives in one tested place.
 */
object RelationshipResolver {
    fun resolve(
        iBlockedThem: Boolean,
        theyBlockedMe: Boolean,
        friends: Boolean,
        incomingPending: Boolean,
        outgoingPending: Boolean,
    ): RelationshipState =
        when {
            iBlockedThem -> RelationshipState.Blocked
            theyBlockedMe -> RelationshipState.BlockedBy
            friends -> RelationshipState.Friends
            incomingPending -> RelationshipState.IncomingRequest
            outgoingPending -> RelationshipState.OutgoingRequest
            else -> RelationshipState.None
        }
}
