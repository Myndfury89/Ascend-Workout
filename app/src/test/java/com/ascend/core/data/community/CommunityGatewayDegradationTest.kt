package com.ascend.core.data.community

import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.ReportReason
import com.ascend.core.domain.community.ShareSettings
import com.ascend.core.domain.community.SharedProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * With no backend configured (null client), every gateway op degrades to a Failure rather than
 * crashing — so local-only play and a backend-less build are unaffected. No live client is constructed.
 */
class CommunityGatewayDegradationTest {
    private fun <T> assertFails(result: RemoteResult<T>) =
        assertTrue("expected graceful Failure, got $result", result is RemoteResult.Failure)

    @Test
    fun `friend gateway degrades gracefully`() =
        runTest {
            val g = SupabaseFriendGateway(client = null)
            assertFails(g.listFriends())
            assertFails(g.listPendingRequests())
            assertFails(g.relationshipWith(RemoteUserId("x")))
            assertFails(g.sendRequest(RemoteUserId("x")))
        }

    @Test
    fun `moderation gateway degrades gracefully`() =
        runTest {
            val g = SupabaseModerationGateway(client = null)
            assertFails(g.block(RemoteUserId("x")))
            assertFails(g.listBlocked())
            assertFails(g.report(RemoteUserId("x"), ReportReason.SPAM, null))
        }

    @Test
    fun `share-settings gateway degrades gracefully`() =
        runTest {
            val g = SupabaseShareSettingsGateway(client = null)
            assertFails(g.getOwn())
            assertFails(g.update(ShareSettings()))
        }

    @Test
    fun `friend-profile gateway degrades gracefully`() =
        runTest {
            val g = SupabaseFriendProfileGateway(client = null)
            assertFails(g.lookupByHandle("hunter"))
            assertFails(g.fetchSharedProfile(RemoteUserId("x")))
            assertFails(g.publishOwnSharedProfile(SharedProfile(RemoteUserId("me"))))
        }
}
