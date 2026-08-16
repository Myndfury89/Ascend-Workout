package com.ascend.feature.community

import com.ascend.core.domain.community.ProfileCard
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.ReportReason
import com.ascend.core.domain.community.SharedProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FriendProfileViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `load resolves the name and the consented profile`() {
        val profiles =
            FakeFriendProfileGateway(
                cards = listOf(ProfileCard(RemoteUserId("u1"), "alpha", "Alpha", null)),
                shared = SharedProfile(RemoteUserId("u1"), selectedClass = "Mage", buildIdentity = "Mage-leaning"),
            )
        val vm = FriendProfileViewModel(profiles, FakeModerationGateway())

        vm.load("u1")
        val s = vm.state.value
        assertEquals("Alpha", s.name)
        assertEquals("Mage", s.profile?.selectedClass)
    }

    @Test
    fun `blocking closes the screen and records the block`() {
        val moderation = FakeModerationGateway()
        val vm = FriendProfileViewModel(FakeFriendProfileGateway(), moderation)

        vm.block("u1")
        assertTrue(RemoteUserId("u1") in moderation.blocked)
        assertTrue(vm.state.value.closed)
    }

    @Test
    fun `reporting records the report and confirms`() {
        val moderation = FakeModerationGateway()
        val vm = FriendProfileViewModel(FakeFriendProfileGateway(), moderation)

        vm.report("u1", ReportReason.SPAM, "spammy")
        assertEquals(1, moderation.reports.size)
        assertEquals("Report submitted for review.", vm.state.value.message)
    }
}
