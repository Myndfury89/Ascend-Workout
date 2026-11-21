package com.ascend.core.common

import com.ascend.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityAccessTest {
    @Test
    fun `debug unit tests see Community disabled for the beta`() {
        assertFalse(BuildConfig.COMMUNITY_ENABLED)
        assertFalse(CommunityFeatures.isEnabled)
    }

    @Test
    fun `account entry and routes are hidden when the flag is off`() {
        assertFalse(CommunityAccess.accountEntryVisible(communityEnabled = false))
        assertFalse(CommunityAccess.registerCommunityRoutes(communityEnabled = false))
        assertFalse(CommunityAccess.shouldStartAuthLinkCoordinator(communityEnabled = false))
    }

    @Test
    fun `account entry and routes are available when the flag is on`() {
        assertTrue(CommunityAccess.accountEntryVisible(communityEnabled = true))
        assertTrue(CommunityAccess.registerCommunityRoutes(communityEnabled = true))
        assertTrue(CommunityAccess.shouldStartAuthLinkCoordinator(communityEnabled = true))
    }

    @Test
    fun `backend client is not created when Community is disabled even if secrets are present`() {
        assertFalse(
            CommunityAccess.shouldCreateBackendClient(
                communityEnabled = false,
                supabaseUrl = "https://example.supabase.co",
                supabaseKey = "anon-key",
            ),
        )
    }

    @Test
    fun `backend client may be created only when Community is enabled and configured`() {
        assertTrue(
            CommunityAccess.shouldCreateBackendClient(
                communityEnabled = true,
                supabaseUrl = "https://example.supabase.co",
                supabaseKey = "anon-key",
            ),
        )
        assertFalse(
            CommunityAccess.shouldCreateBackendClient(
                communityEnabled = true,
                supabaseUrl = "",
                supabaseKey = "anon-key",
            ),
        )
        assertFalse(
            CommunityAccess.shouldCreateBackendClient(
                communityEnabled = true,
                supabaseUrl = "https://example.supabase.co",
                supabaseKey = "",
            ),
        )
        assertFalse(
            CommunityAccess.shouldCreateBackendClient(
                communityEnabled = true,
                supabaseUrl = "   ",
                supabaseKey = "anon-key",
            ),
        )
    }

    @Test
    fun `disabled and enabled predicates stay aligned`() {
        assertEquals(false, CommunityAccess.accountEntryVisible(false))
        assertEquals(false, CommunityAccess.registerCommunityRoutes(false))
        assertEquals(true, CommunityAccess.accountEntryVisible(true))
        assertEquals(true, CommunityAccess.registerCommunityRoutes(true))
    }
}
