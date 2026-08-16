package com.ascend.core.data.community

import com.ascend.core.domain.community.FriendEdge
import com.ascend.core.domain.community.FriendGateway
import com.ascend.core.domain.community.FriendshipId
import com.ascend.core.domain.community.RelationshipResolver
import com.ascend.core.domain.community.RelationshipState
import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.RemoteUserId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

private const val FRIENDSHIPS = "friendships"
private const val BLOCKS = "blocks"
private const val STATUS_ACCEPTED = "ACCEPTED"
private const val STATUS_PENDING = "PENDING"

/**
 * Supabase-backed [FriendGateway]. SDK confined to the data layer. A null client (backend
 * unconfigured) or a signed-out user degrades gracefully. RLS is the server-side enforcer;
 * [com.ascend.core.domain.community.SharePolicy] mirrors the same rules client-side.
 *
 * Note: a client can read only its OWN blocks (RLS), so "they blocked me" is not directly observable
 * here — the server simply denies their data. relationshipWith() therefore reports it as absence, which
 * is safe (P2A.3 verifies this against live RLS).
 */
@Singleton
class SupabaseFriendGateway
    @Inject
    constructor(
        private val client: SupabaseClient?,
    ) : FriendGateway {
        private suspend fun currentUid(): String? = client?.auth?.currentUserOrNull()?.id

        private suspend fun myFriendships(
            c: SupabaseClient,
            uid: String,
        ): List<FriendshipDto> {
            val asRequester = c.postgrest.from(FRIENDSHIPS).select { filter { eq("requester_id", uid) } }.decodeList<FriendshipDto>()
            val asAddressee = c.postgrest.from(FRIENDSHIPS).select { filter { eq("addressee_id", uid) } }.decodeList<FriendshipDto>()
            return (asRequester + asAddressee).distinctBy { it.id }
        }

        override suspend fun listFriends(): RemoteResult<List<FriendEdge>> {
            val c = client ?: return notConfigured()
            val uid = currentUid() ?: return notSignedIn()
            return runRemoteCall {
                RemoteResult.Success(myFriendships(c, uid).filter { it.status == STATUS_ACCEPTED }.map { it.toEdge(uid) })
            }
        }

        override suspend fun listPendingRequests(): RemoteResult<List<FriendEdge>> {
            val c = client ?: return notConfigured()
            val uid = currentUid() ?: return notSignedIn()
            return runRemoteCall {
                RemoteResult.Success(myFriendships(c, uid).filter { it.status == STATUS_PENDING }.map { it.toEdge(uid) })
            }
        }

        override suspend fun relationshipWith(other: RemoteUserId): RemoteResult<RelationshipState> {
            val c = client ?: return notConfigured()
            val uid = currentUid() ?: return notSignedIn()
            return runRemoteCall {
                val iBlockedThem =
                    c.postgrest.from(BLOCKS).select { filter { eq("blocked_id", other.value) } }
                        .decodeList<BlockDto>().any { it.blockerId == uid }
                val edge = myFriendships(c, uid).firstOrNull { it.requesterId == other.value || it.addresseeId == other.value }
                RemoteResult.Success(
                    RelationshipResolver.resolve(
                        iBlockedThem = iBlockedThem,
                        theyBlockedMe = false,
                        friends = edge?.status == STATUS_ACCEPTED,
                        incomingPending = edge?.status == STATUS_PENDING && edge.addresseeId == uid,
                        outgoingPending = edge?.status == STATUS_PENDING && edge.requesterId == uid,
                    ),
                )
            }
        }

        override suspend fun sendRequest(to: RemoteUserId): RemoteResult<Unit> {
            val c = client ?: return notConfigured()
            val uid = currentUid() ?: return notSignedIn()
            return runRemoteCall {
                c.postgrest.from(FRIENDSHIPS).insert(FriendshipInsertDto(requesterId = uid, addresseeId = to.value))
                RemoteResult.Success(Unit)
            }
        }

        override suspend fun acceptRequest(id: FriendshipId): RemoteResult<Unit> {
            val c = client ?: return notConfigured()
            currentUid() ?: return notSignedIn()
            return runRemoteCall {
                c.postgrest.from(FRIENDSHIPS).update({ set("status", STATUS_ACCEPTED) }) { filter { eq("id", id.value) } }
                RemoteResult.Success(Unit)
            }
        }

        override suspend fun declineRequest(id: FriendshipId): RemoteResult<Unit> = deleteFriendship(id)

        override suspend fun cancelRequest(id: FriendshipId): RemoteResult<Unit> = deleteFriendship(id)

        override suspend fun removeFriend(id: FriendshipId): RemoteResult<Unit> = deleteFriendship(id)

        private suspend fun deleteFriendship(id: FriendshipId): RemoteResult<Unit> {
            val c = client ?: return notConfigured()
            currentUid() ?: return notSignedIn()
            return runRemoteCall {
                c.postgrest.from(FRIENDSHIPS).delete { filter { eq("id", id.value) } }
                RemoteResult.Success(Unit)
            }
        }
    }
