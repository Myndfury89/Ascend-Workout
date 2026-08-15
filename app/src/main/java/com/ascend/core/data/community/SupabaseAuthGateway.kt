package com.ascend.core.data.community

import com.ascend.core.domain.community.AuthGateway
import com.ascend.core.domain.community.AuthState
import com.ascend.core.domain.community.RemoteErrorKind
import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.SessionStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase-backed [AuthGateway]. The SDK lives only here (behind the domain interface). When the
 * client is null (backend unconfigured), every method degrades gracefully so local-only play is
 * unaffected. On a successful sign-in it mirrors the remote id into [SessionStore] — the additive
 * "link" that never touches local Room data.
 */
@Singleton
class SupabaseAuthGateway
    @Inject
    constructor(
        private val client: SupabaseClient?,
        private val sessionStore: SessionStore,
    ) : AuthGateway {
        override fun authState(): Flow<AuthState> = client?.auth?.sessionStatus?.map { it.toAuthState() } ?: flowOf(AuthState.SignedOut)

        override suspend fun currentUserId(): RemoteUserId? = client?.auth?.currentUserOrNull()?.id?.let(::RemoteUserId)

        override suspend fun signUp(
            email: String,
            password: String,
        ): RemoteResult<AuthState> {
            val supabase = client ?: return notConfigured()
            return runRemote {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                linkAndState(supabase)
            }
        }

        override suspend fun signIn(
            email: String,
            password: String,
        ): RemoteResult<AuthState> {
            val supabase = client ?: return notConfigured()
            return runRemote {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                linkAndState(supabase)
            }
        }

        override suspend fun signInWithGoogleIdToken(
            idToken: String,
            rawNonce: String?,
        ): RemoteResult<AuthState> {
            val supabase = client ?: return notConfigured()
            return runRemote {
                supabase.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = Google
                    nonce = rawNonce
                }
                linkAndState(supabase)
            }
        }

        override suspend fun signOut() {
            runCatching { client?.auth?.signOut() }
            sessionStore.setLinkedRemoteUserId(null)
        }

        private suspend fun linkAndState(supabase: SupabaseClient): RemoteResult<AuthState> {
            val user = supabase.auth.currentUserOrNull()
            val id = user?.id
            if (id != null) sessionStore.setLinkedRemoteUserId(RemoteUserId(id))
            return RemoteResult.Success(user.toAuthState())
        }

        private fun SessionStatus.toAuthState(): AuthState =
            when (this) {
                is SessionStatus.Authenticated -> session.user.toAuthState()
                else -> AuthState.SignedOut
            }

        private fun UserInfo?.toAuthState(): AuthState =
            if (this == null) {
                AuthState.SignedOut
            } else {
                AuthState.SignedIn(RemoteUserId(id), emailVerified = emailConfirmedAt != null)
            }

        private fun notConfigured(): RemoteResult<AuthState> =
            RemoteResult.Failure(RemoteErrorKind.UNKNOWN, "Community backend is not configured.")

        private suspend fun runRemote(block: suspend () -> RemoteResult<AuthState>): RemoteResult<AuthState> =
            try {
                block()
            } catch (e: IOException) {
                RemoteResult.Offline
            } catch (e: Exception) {
                RemoteResult.Failure(RemoteErrorKind.UNKNOWN, e.message)
            }
    }
