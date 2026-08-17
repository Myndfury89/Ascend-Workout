package com.ascend.core.data.community

import com.ascend.core.domain.community.FriendProfileGateway
import com.ascend.core.domain.community.ProfileCard
import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.SharedProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private const val SHARED_PROFILES = "shared_profiles"
private const val LOOKUP_FN = "lookup_profile_by_handle"
private const val FRIEND_CARDS_FN = "friend_cards"

/**
 * Supabase-backed [FriendProfileGateway]. Discovery goes through the server-side lookup RPC (minimal
 * card, safety-filtered); friend snapshots are read from shared_profiles (RLS gates access to exactly
 * what SharePolicy permits); the current user publishes only their own snapshot.
 */
@Singleton
class SupabaseFriendProfileGateway
    @Inject
    constructor(
        private val client: SupabaseClient?,
    ) : FriendProfileGateway {
        private suspend fun currentUid(): String? = client?.auth?.currentUserOrNull()?.id

        override suspend fun lookupByHandle(handle: String): RemoteResult<ProfileCard?> {
            val c = client ?: return notConfigured()
            currentUid() ?: return notSignedIn()
            return runRemoteCall {
                val card =
                    c.postgrest.rpc(LOOKUP_FN, buildJsonObject { put("p_handle", handle) })
                        .decodeList<ProfileCardDto>()
                        .firstOrNull()
                        ?.toDomain()
                RemoteResult.Success(card)
            }
        }

        override suspend fun friendCards(): RemoteResult<List<ProfileCard>> {
            val c = client ?: return notConfigured()
            currentUid() ?: return notSignedIn()
            return runRemoteCall {
                val cards = c.postgrest.rpc(FRIEND_CARDS_FN).decodeList<ProfileCardDto>().map { it.toDomain() }
                RemoteResult.Success(cards)
            }
        }

        override suspend fun fetchSharedProfile(user: RemoteUserId): RemoteResult<SharedProfile?> {
            val c = client ?: return notConfigured()
            currentUid() ?: return notSignedIn()
            return runRemoteCall {
                val dto =
                    c.postgrest.from(SHARED_PROFILES).select { filter { eq("user_id", user.value) } }
                        .decodeSingleOrNull<SharedProfileDto>()
                RemoteResult.Success(dto?.toDomain())
            }
        }

        override suspend fun publishOwnSharedProfile(profile: SharedProfile): RemoteResult<Unit> {
            val c = client ?: return notConfigured()
            currentUid() ?: return notSignedIn()
            return runRemoteCall {
                c.postgrest.from(SHARED_PROFILES).upsert(SharedProfileDto.from(profile))
                RemoteResult.Success(Unit)
            }
        }
    }
