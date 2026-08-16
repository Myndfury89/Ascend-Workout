package com.ascend.core.domain.community

import org.junit.Assert.assertEquals
import org.junit.Test

class RelationshipResolverTest {
    private fun resolve(
        iBlocked: Boolean = false,
        blockedByMe: Boolean = false,
        friends: Boolean = false,
        incoming: Boolean = false,
        outgoing: Boolean = false,
    ) = RelationshipResolver.resolve(iBlocked, blockedByMe, friends, incoming, outgoing)

    @Test
    fun `no signals resolves to None`() {
        assertEquals(RelationshipState.None, resolve())
    }

    @Test
    fun `outgoing and incoming requests resolve unambiguously`() {
        assertEquals(RelationshipState.OutgoingRequest, resolve(outgoing = true))
        assertEquals(RelationshipState.IncomingRequest, resolve(incoming = true))
    }

    @Test
    fun `friends resolves to Friends`() {
        assertEquals(RelationshipState.Friends, resolve(friends = true))
    }

    @Test
    fun `a block I made takes precedence over friendship and requests`() {
        assertEquals(
            RelationshipState.Blocked,
            resolve(iBlocked = true, friends = true, incoming = true, outgoing = true),
        )
    }

    @Test
    fun `a block against me takes precedence over friendship`() {
        assertEquals(RelationshipState.BlockedBy, resolve(blockedByMe = true, friends = true))
    }

    @Test
    fun `my own block outranks being blocked back`() {
        // Deterministic: if both blocks exist, my-block resolves first (I see them as Blocked).
        assertEquals(RelationshipState.Blocked, resolve(iBlocked = true, blockedByMe = true))
    }

    @Test
    fun `incoming outranks outgoing when both somehow present`() {
        assertEquals(RelationshipState.IncomingRequest, resolve(incoming = true, outgoing = true))
    }
}
