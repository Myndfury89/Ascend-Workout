package com.ascend.core.domain.community

/**
 * Friend relationships. Domain seam only — no Supabase/SDK types appear here; the data-layer
 * implementation (P2A.3) wraps the SDK behind this interface. All operations use [RemoteUserId]
 * (remote identity); local game identity is never involved. Results reuse the P1 [RemoteResult] so
 * success / offline / expected-failure / remote-error stay distinguishable.
 */
interface FriendGateway {
    suspend fun listFriends(): RemoteResult<List<FriendEdge>>

    /** Pending requests in both directions (see [FriendEdge.incoming]). */
    suspend fun listPendingRequests(): RemoteResult<List<FriendEdge>>

    suspend fun relationshipWith(other: RemoteUserId): RemoteResult<RelationshipState>

    suspend fun sendRequest(to: RemoteUserId): RemoteResult<Unit>

    suspend fun acceptRequest(id: FriendshipId): RemoteResult<Unit>

    suspend fun declineRequest(id: FriendshipId): RemoteResult<Unit>

    suspend fun cancelRequest(id: FriendshipId): RemoteResult<Unit>

    suspend fun removeFriend(id: FriendshipId): RemoteResult<Unit>
}

/**
 * Moderation. Blocking must ultimately take precedence over friendship/profile visibility (enforced by
 * [SharePolicy] + server RLS). Report carries only a subject, a coarse [ReportReason], and free text —
 * never any sensitive profile/health data.
 */
interface ModerationGateway {
    suspend fun block(user: RemoteUserId): RemoteResult<Unit>

    suspend fun unblock(user: RemoteUserId): RemoteResult<Unit>

    suspend fun listBlocked(): RemoteResult<List<RemoteUserId>>

    suspend fun isBlocked(user: RemoteUserId): RemoteResult<Boolean>

    suspend fun report(
        subject: RemoteUserId?,
        reason: ReportReason,
        note: String?,
    ): RemoteResult<Unit>
}

/** The current user's own consent settings for what friends may see. */
interface ShareSettingsGateway {
    suspend fun getOwn(): RemoteResult<ShareSettings>

    suspend fun update(settings: ShareSettings): RemoteResult<ShareSettings>
}

/**
 * Reads of other users' presentation data and publishing of the current user's own snapshot.
 * [lookupByHandle] returns only a minimal [ProfileCard] (discovery). [fetchSharedProfile] returns the
 * derived [SharedProfile] a friend consented to share. What actually ends up in a published snapshot is
 * decided by [SharePolicy] — never by the UI or the data layer independently.
 */
interface FriendProfileGateway {
    suspend fun lookupByHandle(handle: String): RemoteResult<ProfileCard?>

    suspend fun fetchSharedProfile(user: RemoteUserId): RemoteResult<SharedProfile?>

    suspend fun publishOwnSharedProfile(profile: SharedProfile): RemoteResult<Unit>
}
