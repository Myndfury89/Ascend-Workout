package com.ascend.core.data.community

import com.ascend.core.domain.community.AuthState
import com.ascend.core.domain.community.RemoteUserId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
