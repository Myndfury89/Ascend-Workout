package com.ascend.core.data.community

import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.ShareSettings
import com.ascend.core.domain.community.ShareSettingsGateway
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

private const val SHARE_SETTINGS = "share_settings"

/** Supabase-backed [ShareSettingsGateway] — the current user's own consent row (RLS: own-row only). */
@Singleton
class SupabaseShareSettingsGateway
    @Inject
    constructor(
        private val client: SupabaseClient?,
    ) : ShareSettingsGateway {
        private suspend fun currentUid(): String? = client?.auth?.currentUserOrNull()?.id

        override suspend fun getOwn(): RemoteResult<ShareSettings> {
            val c = client ?: return notConfigured()
            val uid = currentUid() ?: return notSignedIn()
            return runRemoteCall {
                val dto =
                    c.postgrest.from(SHARE_SETTINGS).select { filter { eq("user_id", uid) } }
                        .decodeSingleOrNull<ShareSettingsDto>()
                RemoteResult.Success(dto?.toDomain() ?: ShareSettings.PRIVATE_DEFAULT)
            }
        }

        override suspend fun update(settings: ShareSettings): RemoteResult<ShareSettings> {
            val c = client ?: return notConfigured()
            val uid = currentUid() ?: return notSignedIn()
            return runRemoteCall {
                c.postgrest.from(SHARE_SETTINGS).upsert(ShareSettingsDto.from(uid, settings))
                RemoteResult.Success(settings)
            }
        }
    }
