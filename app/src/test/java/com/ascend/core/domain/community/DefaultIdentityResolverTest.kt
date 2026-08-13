package com.ascend.core.domain.community

import com.ascend.core.common.LOCAL_USER_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultIdentityResolverTest {
    private class FakeSessionStore(initial: RemoteUserId? = null) : SessionStore {
        private val state = MutableStateFlow(initial)

        override fun linkedRemoteUserId(): Flow<RemoteUserId?> = state

        override suspend fun setLinkedRemoteUserId(id: RemoteUserId?) {
            state.value = id
        }
    }

    @Test
    fun `local user id is the permanent local key regardless of sign-in`() =
        runTest {
            val signedOut = DefaultIdentityResolver(FakeSessionStore())
            val signedIn = DefaultIdentityResolver(FakeSessionStore(RemoteUserId("uuid-1")))

            assertEquals(LOCAL_USER_ID, signedOut.localUserId())
            assertEquals(LOCAL_USER_ID, signedIn.localUserId())
        }

    @Test
    fun `signed out has no remote id and is not signed in`() =
        runTest {
            val resolver = DefaultIdentityResolver(FakeSessionStore())
            assertNull(resolver.currentRemoteUserId())
            assertNull(resolver.remoteUserId().first())
            assertFalse(resolver.isSignedIn().first())
        }

    @Test
    fun `linking a remote id reflects in the resolver without touching the local id`() =
        runTest {
            val store = FakeSessionStore()
            val resolver = DefaultIdentityResolver(store)

            store.setLinkedRemoteUserId(RemoteUserId("uuid-42"))

            assertEquals(RemoteUserId("uuid-42"), resolver.currentRemoteUserId())
            assertTrue(resolver.isSignedIn().first())
            // The local key is untouched by linking — no orphaning is possible.
            assertEquals(LOCAL_USER_ID, resolver.localUserId())
        }
}
