package com.ascend.core.domain.community

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharePolicyTest {
    private val target = RemoteUserId("target")

    private fun ctx(
        relationship: RelationshipState = RelationshipState.Friends,
        visibility: ShareVisibility = ShareVisibility.FRIENDS,
        shareBuild: Boolean = false,
        shareClass: Boolean = false,
        targetEligible: Boolean = true,
        viewerEligible: Boolean = true,
    ) = ShareContext(
        relationship = relationship,
        targetSettings = ShareSettings(visibility, shareBuild, shareClass),
        targetSocialEligible = targetEligible,
        viewerSocialEligible = viewerEligible,
    )

    private val fullProfile =
        SharedProfile(
            userId = target,
            selectedClass = "Mage",
            classLevel = 12,
            rank = "Gold",
            overallLevel = 40,
            buildIdentity = "Mage-leaning",
            topAffinities = listOf(SharedAffinity("mage", 0.82)),
            avatarBodyBase = "MALE",
        )

    // ---- block precedence ----

    @Test
    fun `a user I blocked is never viewable`() {
        val c = ctx(relationship = RelationshipState.Blocked)
        assertFalse(SharePolicy.canViewProfile(c))
        assertNull(SharePolicy.redact(fullProfile, c))
    }

    @Test
    fun `a user who blocked me is never viewable`() {
        val c = ctx(relationship = RelationshipState.BlockedBy)
        assertNull(SharePolicy.redact(fullProfile, c))
    }

    // ---- relationship / visibility gates ----

    @Test
    fun `non-friends cannot see a profile`() {
        listOf(RelationshipState.None, RelationshipState.OutgoingRequest, RelationshipState.IncomingRequest).forEach {
            assertNull("relationship $it must not expose", SharePolicy.redact(fullProfile, ctx(relationship = it)))
        }
    }

    @Test
    fun `PRIVATE visibility never exposes even to a friend`() {
        assertNull(SharePolicy.redact(fullProfile, ctx(visibility = ShareVisibility.PRIVATE)))
    }

    @Test
    fun `an ineligible target or viewer (minor) is not viewable`() {
        assertNull(SharePolicy.redact(fullProfile, ctx(targetEligible = false)))
        assertNull(SharePolicy.redact(fullProfile, ctx(viewerEligible = false)))
    }

    // ---- consent respected ----

    @Test
    fun `friends with no toggles see only the cosmetic baseline`() {
        val out = SharePolicy.redact(fullProfile, ctx(shareBuild = false, shareClass = false))!!
        assertEquals("MALE", out.avatarBodyBase)
        assertNull(out.selectedClass)
        assertNull(out.classLevel)
        assertNull(out.rank)
        assertNull(out.buildIdentity)
        assertTrue(out.topAffinities.isEmpty())
    }

    @Test
    fun `class progress toggle exposes class fields only`() {
        val out = SharePolicy.redact(fullProfile, ctx(shareClass = true, shareBuild = false))!!
        assertEquals("Mage", out.selectedClass)
        assertEquals(12, out.classLevel)
        assertEquals("Gold", out.rank)
        assertEquals(40, out.overallLevel)
        assertNull("build identity must stay hidden without its toggle", out.buildIdentity)
        assertTrue(out.topAffinities.isEmpty())
    }

    @Test
    fun `build identity toggle exposes identity and affinities only`() {
        val out = SharePolicy.redact(fullProfile, ctx(shareBuild = true, shareClass = false))!!
        assertEquals("Mage-leaning", out.buildIdentity)
        assertEquals(1, out.topAffinities.size)
        assertNull("class progress must stay hidden without its toggle", out.selectedClass)
        assertNull(out.classLevel)
    }

    @Test
    fun `both toggles expose the permitted friend-facing fields`() {
        val out = SharePolicy.redact(fullProfile, ctx(shareBuild = true, shareClass = true))!!
        assertEquals("Mage", out.selectedClass)
        assertEquals("Mage-leaning", out.buildIdentity)
        assertEquals("MALE", out.avatarBodyBase)
    }

    // ---- fail-closed ----

    @Test
    fun `no field is exposable when the profile is not viewable`() {
        val blocked = ctx(relationship = RelationshipState.Blocked)
        ShareableField.entries.forEach {
            assertFalse("$it must not be exposable when blocked", SharePolicy.isExposable(it, blocked))
        }
    }

    @Test
    fun `the shareable allowlist contains no health or evidence field`() {
        val prohibited =
            listOf(
                "WEIGHT", "BODYFAT", "BODYCOMP", "SLEEP", "HRV", "HEART", "CALOR", "LOCATION",
                "HEALTH", "SAMSUNG", "EVIDENCE", "COVERAGE", "RECOVERY", "STEPS",
            )
        ShareableField.entries.forEach { field ->
            prohibited.forEach { bad ->
                assertFalse("ShareableField.$field must not reference $bad", field.name.contains(bad))
            }
        }
    }

    @Test
    fun `the shared profile type declares no health or raw-evidence fields`() {
        val prohibited =
            listOf(
                "weight", "bodyfat", "bodycomp", "sleep", "hrv", "heart", "calor", "location",
                "health", "samsung", "evidence", "coverage", "recovery", "steps",
            )
        val members = SharedProfile::class.java.declaredFields.map { it.name.lowercase() }
        members.forEach { m ->
            prohibited.forEach { bad ->
                assertFalse("SharedProfile.$m must not exist", m.contains(bad))
            }
        }
    }
}
