package com.ascend.feature.community

import com.ascend.core.domain.community.FriendEdge
import com.ascend.core.domain.community.FriendGateway
import com.ascend.core.domain.community.FriendProfileGateway
import com.ascend.core.domain.community.FriendshipId
import com.ascend.core.domain.community.ModerationGateway
import com.ascend.core.domain.community.ProfileCard
import com.ascend.core.domain.community.RelationshipState
import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.ReportReason
import com.ascend.core.domain.community.ShareSettings
import com.ascend.core.domain.community.ShareSettingsGateway
import com.ascend.core.domain.community.SharedProfile

/** In-memory fakes for community gateway tests — no SDK, no network. */
class FakeFriendGateway(
    var friends: List<FriendEdge> = emptyList(),
    var pending: List<FriendEdge> = emptyList(),
) : FriendGateway {
    val sent = mutableListOf<RemoteUserId>()
    val accepted = mutableListOf<FriendshipId>()
    val removed = mutableListOf<FriendshipId>()

    override suspend fun listFriends() = RemoteResult.Success(friends)

    override suspend fun listPendingRequests() = RemoteResult.Success(pending)

    override suspend fun relationshipWith(other: RemoteUserId) = RemoteResult.Success<RelationshipState>(RelationshipState.None)

    override suspend fun sendRequest(to: RemoteUserId): RemoteResult<Unit> {
        sent += to
        return RemoteResult.Success(Unit)
    }

    override suspend fun acceptRequest(id: FriendshipId): RemoteResult<Unit> {
        accepted += id
        return RemoteResult.Success(Unit)
    }

    override suspend fun declineRequest(id: FriendshipId) = RemoteResult.Success(Unit)

    override suspend fun cancelRequest(id: FriendshipId) = RemoteResult.Success(Unit)

    override suspend fun removeFriend(id: FriendshipId): RemoteResult<Unit> {
        removed += id
        return RemoteResult.Success(Unit)
    }
}

class FakeFriendProfileGateway(
    var cards: List<ProfileCard> = emptyList(),
    var shared: SharedProfile? = null,
    var lookupResult: ProfileCard? = null,
) : FriendProfileGateway {
    override suspend fun lookupByHandle(handle: String) = RemoteResult.Success(lookupResult)

    override suspend fun friendCards() = RemoteResult.Success(cards)

    override suspend fun fetchSharedProfile(user: RemoteUserId) = RemoteResult.Success(shared)

    override suspend fun publishOwnSharedProfile(profile: SharedProfile) = RemoteResult.Success(Unit)
}

class FakeModerationGateway : ModerationGateway {
    val blocked = mutableListOf<RemoteUserId>()
    val reports = mutableListOf<Triple<RemoteUserId?, ReportReason, String?>>()

    override suspend fun block(user: RemoteUserId): RemoteResult<Unit> {
        blocked += user
        return RemoteResult.Success(Unit)
    }

    override suspend fun unblock(user: RemoteUserId) = RemoteResult.Success(Unit)

    override suspend fun listBlocked() = RemoteResult.Success(blocked.toList())

    override suspend fun isBlocked(user: RemoteUserId) = RemoteResult.Success(user in blocked)

    override suspend fun report(
        subject: RemoteUserId?,
        reason: ReportReason,
        note: String?,
    ): RemoteResult<Unit> {
        reports += Triple(subject, reason, note)
        return RemoteResult.Success(Unit)
    }
}

class FakeShareSettingsGateway(
    var settings: ShareSettings = ShareSettings.PRIVATE_DEFAULT,
) : ShareSettingsGateway {
    override suspend fun getOwn() = RemoteResult.Success(settings)

    override suspend fun update(settings: ShareSettings): RemoteResult<ShareSettings> {
        this.settings = settings
        return RemoteResult.Success(settings)
    }
}
