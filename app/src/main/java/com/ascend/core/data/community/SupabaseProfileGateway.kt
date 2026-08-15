package com.ascend.core.data.community

import com.ascend.core.domain.community.ProfileGateway
import com.ascend.core.domain.community.RemoteErrorKind
import com.ascend.core.domain.community.RemoteProfile
import com.ascend.core.domain.community.RemoteResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase-backed [ProfileGateway] over the `profiles` table (identity only). RLS restricts every
 * query to the signed-in user's own row; this gateway additionally refuses when signed out or
 * unconfigured, degrading gracefully rather than throwing.
 */
@Singleton
class SupabaseProfileGateway
    @Inject
    constructor(
        private val client: SupabaseClient?,
    ) : ProfileGateway {
        override suspend fun fetchOwnProfile(): RemoteResult<RemoteProfile?> {
            val supabase = client ?: return notConfigured()
            val uid = supabase.auth.currentUserOrNull()?.id ?: return notSignedIn()
            return runRemote {
                val dto =
                    supabase.postgrest.from(PROFILES)
                        .select { filter { eq("id", uid) } }
                        .decodeSingleOrNull<ProfileDto>()
                RemoteResult.Success(dto?.toDomain())
            }
        }

        override suspend fun upsertOwnProfile(
            handle: String?,
            displayName: String?,
        ): RemoteResult<RemoteProfile> {
            val supabase = client ?: return notConfigured()
            val uid = supabase.auth.currentUserOrNull()?.id ?: return notSignedIn()
            return runRemote {
                val dto = ProfileDto(id = uid, handle = handle, displayName = displayName)
                supabase.postgrest.from(PROFILES).upsert(dto)
                RemoteResult.Success(dto.toDomain())
            }
        }

        private fun <T> notConfigured(): RemoteResult<T> =
            RemoteResult.Failure(RemoteErrorKind.UNKNOWN, "Community backend is not configured.")

        private fun <T> notSignedIn(): RemoteResult<T> = RemoteResult.Failure(RemoteErrorKind.AUTH, "Not signed in.")

        private suspend fun <T> runRemote(block: suspend () -> RemoteResult<T>): RemoteResult<T> =
            try {
                block()
            } catch (e: IOException) {
                RemoteResult.Offline
            } catch (e: Exception) {
                RemoteResult.Failure(RemoteErrorKind.UNKNOWN, e.message)
            }

        private companion object {
            const val PROFILES = "profiles"
        }
    }
