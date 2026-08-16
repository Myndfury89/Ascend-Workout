package com.ascend.core.data.community

import com.ascend.core.domain.community.ModerationGateway
import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.ReportReason
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

private const val BLOCKS = "blocks"
private const val REPORTS = "reports"

/** Supabase-backed [ModerationGateway]. Blocking + reporting; RLS scopes rows to the current user. */
@Singleton
class SupabaseModerationGateway
    @Inject
    constructor(
        private val client: SupabaseClient?,
    ) : ModerationGateway {
        private suspend fun currentUid(): String? = client?.auth?.currentUserOrNull()?.id

        override suspend fun block(user: RemoteUserId): RemoteResult<Unit> {
            val c = client ?: return notConfigured()
            val uid = currentUid() ?: return notSignedIn()
            return runRemoteCall {
                c.postgrest.from(BLOCKS).insert(BlockDto(blockerId = uid, blockedId = user.value))
                RemoteResult.Success(Unit)
            }
        }

        override suspend fun unblock(user: RemoteUserId): RemoteResult<Unit> {
            val c = client ?: return notConfigured()
            val uid = currentUid() ?: return notSignedIn()
            return runRemoteCall {
                c.postgrest.from(BLOCKS).delete {
                    filter {
                        eq("blocker_id", uid)
                        eq("blocked_id", user.value)
                    }
                }
                RemoteResult.Success(Unit)
            }
        }

        override suspend fun listBlocked(): RemoteResult<List<RemoteUserId>> {
            val c = client ?: return notConfigured()
            currentUid() ?: return notSignedIn()
            return runRemoteCall {
                val blocked = c.postgrest.from(BLOCKS).select().decodeList<BlockDto>().map { RemoteUserId(it.blockedId) }
                RemoteResult.Success(blocked)
            }
        }

        override suspend fun isBlocked(user: RemoteUserId): RemoteResult<Boolean> {
            val c = client ?: return notConfigured()
            currentUid() ?: return notSignedIn()
            return runRemoteCall {
                val hit = c.postgrest.from(BLOCKS).select { filter { eq("blocked_id", user.value) } }.decodeList<BlockDto>()
                RemoteResult.Success(hit.isNotEmpty())
            }
        }

        override suspend fun report(
            subject: RemoteUserId?,
            reason: ReportReason,
            note: String?,
        ): RemoteResult<Unit> {
            val c = client ?: return notConfigured()
            val uid = currentUid() ?: return notSignedIn()
            return runRemoteCall {
                c.postgrest.from(REPORTS).insert(ReportDto.from(reporterId = uid, subject = subject, reason = reason, note = note))
                RemoteResult.Success(Unit)
            }
        }
    }
