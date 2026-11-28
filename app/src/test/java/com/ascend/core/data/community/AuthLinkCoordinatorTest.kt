package com.ascend.core.data.community

import com.ascend.core.domain.community.AuthGateway
import com.ascend.core.domain.community.AuthState
import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLinkCoordinatorTest {
    @Test
    fun `signed in maps to the remote id it should link`() {
        val state = AuthState.SignedIn(RemoteUserId("uuid-7"), emailVerified = true)
        assertEquals(RemoteUserId("uuid-7"), state.linkedRemoteUserId())
    }

    @Test
    fun `signed out maps to no link`() {
        assertNull(AuthState.SignedOut.linkedRemoteUserId())
    }

    @Test
    fun `start is a no-op when Community is disabled`() =
        runTest {
            val auth = RecordingAuthGateway()
            val store = RecordingSessionStore()
            val coordinator = AuthLinkCoordinator(auth, store, backgroundScope)
            coordinator.start(communityEnabled = false)
            advanceUntilIdle()
            assertEquals(0, auth.authStateCalls)
            assertTrue(store.writes.isEmpty())
        }

    @Test
    fun `start observes auth state when Community is enabled`() =
        runTest(UnconfinedTestDispatcher()) {
            val auth = RecordingAuthGateway()
            val store = RecordingSessionStore()
            val coordinator = AuthLinkCoordinator(auth, store, backgroundScope)
            coordinator.start(communityEnabled = true)
            advanceUntilIdle()
            assertEquals(1, auth.authStateCalls)
        }
}

private class RecordingAuthGateway : AuthGateway {
    var authStateCalls = 0

    override fun authState(): Flow<AuthState> {
        authStateCalls++
        return MutableSharedFlow()
    }

    override suspend fun currentUserId(): RemoteUserId? = null

    override suspend fun signUp(
        email: String,
        password: String,
    ): RemoteResult<AuthState> = RemoteResult.Offline

    override suspend fun signIn(
        email: String,
        password: String,
    ): RemoteResult<AuthState> = RemoteResult.Offline

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        rawNonce: String?,
    ): RemoteResult<AuthState> = RemoteResult.Offline

    override suspend fun signOut() = Unit
}

private class RecordingSessionStore : SessionStore {
    val writes = mutableListOf<RemoteUserId?>()

    override fun linkedRemoteUserId(): Flow<RemoteUserId?> = flowOf(null)

    override suspend fun setLinkedRemoteUserId(id: RemoteUserId?) {
        writes += id
    }
}
