package com.ascend.core.data.community

import com.ascend.core.common.di.ApplicationScope
import com.ascend.core.domain.community.AuthGateway
import com.ascend.core.domain.community.AuthState
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single writer of the local↔remote identity link. It observes the live auth state and mirrors it
 * into [SessionStore], so on launch a restored Supabase session re-establishes the link, and an
 * expired/invalid session clears a stale one. It only ever touches the remote-link store — the local
 * Room key (LOCAL_USER_ID) is never re-keyed, so existing history can't be orphaned.
 *
 * Robust by construction: the observed flow is `.catch`-guarded so a transient error can never leak to
 * the global handler and crash the app scope.
 */
@Singleton
class AuthLinkCoordinator
    @Inject
    constructor(
        private val authGateway: AuthGateway,
        private val sessionStore: SessionStore,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        private val started = AtomicBoolean(false)

        /** Idempotent; safe to call once at app start. */
        fun start() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                authGateway.authState()
                    .map { it.linkedRemoteUserId() }
                    .distinctUntilChanged()
                    .catch { }
                    .collect { sessionStore.setLinkedRemoteUserId(it) }
            }
        }
    }

/** The remote id implied by an auth state: present only when signed in. */
internal fun AuthState.linkedRemoteUserId(): RemoteUserId? = (this as? AuthState.SignedIn)?.userId
