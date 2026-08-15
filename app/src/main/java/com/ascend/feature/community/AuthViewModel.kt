package com.ascend.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.BuildConfig
import com.ascend.core.domain.community.AuthGateway
import com.ascend.core.domain.community.AuthState
import com.ascend.core.domain.community.RemoteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Read + action state for the additive, opt-in auth surface. Never required for the core loop. */
data class AuthUiState(
    val signedIn: Boolean = false,
    val emailVerified: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val googleAvailable: Boolean = false,
)

private data class ActionState(
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * Drives sign-in / sign-up / sign-out over [AuthGateway]. Purely additive — nothing here gates the
 * single-player experience. Google is offered only when a web client id is configured; email/password
 * always works (throttled). Remote failures surface as a message, never a crash.
 */
@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authGateway: AuthGateway,
    ) : ViewModel() {
        private val action = MutableStateFlow(ActionState())
        private val googleAvailable = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

        val uiState: StateFlow<AuthUiState> =
            combine(authGateway.authState(), action) { auth, act ->
                AuthUiState(
                    signedIn = auth is AuthState.SignedIn,
                    emailVerified = (auth as? AuthState.SignedIn)?.emailVerified ?: false,
                    loading = act.loading,
                    error = act.error,
                    googleAvailable = googleAvailable,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AuthUiState(googleAvailable = googleAvailable))

        fun signIn(
            email: String,
            password: String,
        ) = runAction { authGateway.signIn(email.trim(), password) }

        fun signUp(
            email: String,
            password: String,
        ) = runAction { authGateway.signUp(email.trim(), password) }

        /** Called with a Google ID token acquired by the UI layer (Credential Manager, added next). */
        fun submitGoogleIdToken(idToken: String) = runAction { authGateway.signInWithGoogleIdToken(idToken) }

        fun signOut() {
            viewModelScope.launch { authGateway.signOut() }
        }

        fun dismissError() = action.update { it.copy(error = null) }

        private fun runAction(block: suspend () -> RemoteResult<AuthState>) {
            viewModelScope.launch {
                action.update { it.copy(loading = true, error = null) }
                val result = block()
                action.update {
                    it.copy(
                        loading = false,
                        error =
                            when (result) {
                                is RemoteResult.Success -> null
                                RemoteResult.Offline -> "You're offline. Check your connection and try again."
                                is RemoteResult.Failure -> result.message ?: "Something went wrong. Try again."
                            },
                    )
                }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
