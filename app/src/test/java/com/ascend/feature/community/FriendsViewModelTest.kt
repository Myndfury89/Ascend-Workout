package com.ascend.feature.community

import com.ascend.core.domain.community.FriendEdge
import com.ascend.core.domain.community.FriendshipId
import com.ascend.core.domain.community.FriendshipStatus
import com.ascend.core.domain.community.ProfileCard
import com.ascend.core.domain.community.RemoteUserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FriendsViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun edge(
        id: String,
        other: String,
        status: FriendshipStatus,
        incoming: Boolean,
    ) = FriendEdge(FriendshipId(id), RemoteUserId(other), status, incoming)

    @Test
    fun `refresh merges edges with names and splits requests by direction`() {
        val friends =
            FakeFriendGateway(
                friends = listOf(edge("f1", "u1", FriendshipStatus.ACCEPTED, incoming = false)),
                pending =
                    listOf(
                        edge("f2", "u2", FriendshipStatus.PENDING, incoming = true),
                        edge("f3", "u3", FriendshipStatus.PENDING, incoming = false),
                    ),
            )
        val profiles =
            FakeFriendProfileGateway(
                cards =
                    listOf(
                        ProfileCard(RemoteUserId("u1"), "alpha", "Alpha", null),
                        ProfileCard(RemoteUserId("u2"), "bravo", "Bravo", null),
                        ProfileCard(RemoteUserId("u3"), "charlie", "Charlie", null),
                    ),
            )
        val vm = FriendsViewModel(friends, profiles)
        val s = vm.state.value

        assertEquals(1, s.friends.size)
        assertEquals("Alpha", s.friends.first().displayName)
        assertEquals(listOf("Bravo"), s.incoming.map { it.displayName })
        assertEquals(listOf("Charlie"), s.outgoing.map { it.displayName })
    }

    @Test
    fun `search surfaces a lookup and sending a request confirms`() {
        val profiles = FakeFriendProfileGateway(lookupResult = ProfileCard(RemoteUserId("u9"), "delta", "Delta", null))
        val friends = FakeFriendGateway()
        val vm = FriendsViewModel(friends, profiles)

        vm.search("delta")
        assertEquals(RemoteUserId("u9"), vm.state.value.lookup?.userId)

        vm.sendRequest(RemoteUserId("u9"))
        assertTrue(RemoteUserId("u9") in friends.sent)
        assertEquals("Request sent.", vm.state.value.message)
    }

    @Test
    fun `an unknown handle reports not found`() {
        val vm = FriendsViewModel(FakeFriendGateway(), FakeFriendProfileGateway(lookupResult = null))
        vm.search("nobody")
        assertEquals("No player found with that handle.", vm.state.value.message)
    }
}
