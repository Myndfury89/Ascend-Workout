package com.ascend.core.data.community

import com.ascend.core.domain.community.FriendshipStatus
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.ReportReason
import com.ascend.core.domain.community.ShareSettings
import com.ascend.core.domain.community.ShareVisibility
import com.ascend.core.domain.community.SharedAffinity
import com.ascend.core.domain.community.SharedProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityDtoMappingTest {
    @Test
    fun `friendship maps to an outgoing edge when I am the requester`() {
        val edge = FriendshipDto("f1", requesterId = "me", addresseeId = "you", status = "PENDING").toEdge("me")
        assertEquals(RemoteUserId("you"), edge.other)
        assertEquals(FriendshipStatus.PENDING, edge.status)
        assertFalse("I sent it, so it is not incoming", edge.incoming)
    }

    @Test
    fun `friendship maps to an incoming edge when I am the addressee`() {
        val edge = FriendshipDto("f2", requesterId = "you", addresseeId = "me", status = "PENDING").toEdge("me")
        assertEquals(RemoteUserId("you"), edge.other)
        assertTrue("they sent it to me, so it is incoming", edge.incoming)
    }

    @Test
    fun `share settings round-trip through the dto`() {
        val settings = ShareSettings(ShareVisibility.FRIENDS, shareBuildIdentity = true, shareClassProgress = false)
        val restored = ShareSettingsDto.from("u1", settings).toDomain()
        assertEquals(settings, restored)
    }

    @Test
    fun `private is the safe default when the visibility token is unknown`() {
        assertEquals(ShareVisibility.PRIVATE, ShareSettingsDto(visibility = "WHATEVER").toDomain().visibility)
    }

    @Test
    fun `shared profile round-trips including affinities`() {
        val profile =
            SharedProfile(
                userId = RemoteUserId("u1"),
                selectedClass = "Mage",
                classLevel = 10,
                rank = "Gold",
                overallLevel = 33,
                buildIdentity = "Mage-leaning",
                topAffinities = listOf(SharedAffinity("mage", 0.9), SharedAffinity("monk", 0.4)),
                avatarBodyBase = "FEMALE",
            )
        assertEquals(profile, SharedProfileDto.from(profile).toDomain())
    }

    @Test
    fun `profile card maps to the minimal domain card`() {
        val card = ProfileCardDto("u1", "hunter", "Hunter", "MALE").toDomain()
        assertEquals(RemoteUserId("u1"), card.userId)
        assertEquals("hunter", card.handle)
        assertEquals("MALE", card.avatarBodyBase)
    }

    @Test
    fun `report dto carries only reporter, subject, reason, note`() {
        val dto = ReportDto.from(reporterId = "me", subject = RemoteUserId("them"), reason = ReportReason.SPAM, note = "x")
        assertEquals("me", dto.reporterId)
        assertEquals("them", dto.subjectId)
        assertEquals("SPAM", dto.reason)
        assertEquals("x", dto.note)
    }
}
