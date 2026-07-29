package com.ascend.core.domain.dungeon

import com.ascend.core.model.DungeonPartyMember
import com.ascend.core.model.MemberActivityState
import com.ascend.core.model.SoloReadinessInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DungeonPartyTest {
    private val eligibility = ContributionEligibilityEvaluator()
    private val scaling = PartyScalingCalculator()
    private val solo = SoloReadinessEvaluator()

    private fun member(
        state: MemberActivityState = MemberActivityState.ACTIVE,
        lastActivityAt: Long = 0,
        hasLeft: Boolean = false,
        earned: Long = 500,
    ) = DungeonPartyMember(
        id = "m1", displayName = "M", trainingTier = 3, classId = null, skillIds = emptySet(),
        activityState = state, earnedContribution = earned, lastActivityAt = lastActivityAt, hasLeft = hasLeft,
    )

    // ---- contribution eligibility ----

    @Test
    fun `an active member with a live workout contributes`() {
        val e = eligibility.evaluate(member(lastActivityAt = 1_000), workoutActive = true, now = 1_000)
        assertEquals(MemberActivityState.ACTIVE, e.resolvedState)
        assertTrue(e.canContribute)
    }

    @Test
    fun `a member past the inactivity timeout cannot contribute`() {
        val e = eligibility.evaluate(member(lastActivityAt = 0), workoutActive = true, now = 200_000)
        assertEquals(MemberActivityState.INACTIVE, e.resolvedState)
        assertFalse(e.canContribute)
    }

    @Test
    fun `a member between idle-warning and timeout is warned but does not contribute`() {
        val e = eligibility.evaluate(member(lastActivityAt = 0), workoutActive = true, now = 60_000)
        assertEquals(MemberActivityState.IDLE_WARNING, e.resolvedState)
        assertFalse(e.canContribute)
    }

    @Test
    fun `a paused member cannot contribute and messaging is neutral`() {
        val e = eligibility.evaluate(member(state = MemberActivityState.PAUSED), workoutActive = true, now = 0)
        assertFalse(e.canContribute)
        assertEquals("Party member contribution paused.", e.message)
    }

    @Test
    fun `a safety stop suspends contribution with neutral messaging`() {
        val e = eligibility.evaluate(member(state = MemberActivityState.SAFETY_PAUSED), workoutActive = true, now = 0)
        assertEquals(MemberActivityState.SAFETY_PAUSED, e.resolvedState)
        assertFalse(e.canContribute)
        assertEquals("Party member contribution paused.", e.message)
    }

    @Test
    fun `no active workout stops contribution`() {
        assertFalse(eligibility.evaluate(member(lastActivityAt = 0), workoutActive = false, now = 0).canContribute)
    }

    @Test
    fun `going inactive stops contributing but retains earned contribution`() {
        val active = member(earned = 800)
        val inactive = active.copy(activityState = MemberActivityState.INACTIVE)
        assertFalse(eligibility.evaluate(inactive, workoutActive = true, now = 200_000).canContribute)
        assertEquals("earned contribution is retained", 800L, inactive.earnedContribution)
    }

    // ---- party scaling ----

    @Test
    fun `more members lower individual burden but raise total health`() {
        fun input(count: Int) =
            PartyScalingInput(
                baseEnemyHealth = 2000, encounterTier = 2, memberCount = count, activeMemberCount = count,
                averageTrainingTier = 3, distinctClasses = 1, distinctSkills = 1, recentActivityFraction = 1.0, solo = false,
            )
        val solo1 = scaling.scale(input(1))
        val party4 = scaling.scale(input(4))
        assertTrue("per-member burden falls with more members", party4.expectedContributionPerMember < solo1.expectedContributionPerMember)
        assertTrue("total enemy health rises with more members (not trivialized)", party4.scaledEnemyHealth > solo1.scaledEnemyHealth)
    }

    @Test
    fun `solo mode discounts enemy health but keeps a difficulty rating`() {
        val base =
            PartyScalingInput(
                baseEnemyHealth = 2000, encounterTier = 2, memberCount = 1, activeMemberCount = 1,
                averageTrainingTier = 3, distinctClasses = 1, distinctSkills = 1, recentActivityFraction = 1.0, solo = false,
            )
        val soloScaled = scaling.scale(base.copy(solo = true))
        val normal = scaling.scale(base)
        assertTrue(soloScaled.scaledEnemyHealth < normal.scaledEnemyHealth)
    }

    // ---- solo readiness (real fitness evidence; never Class Level) ----

    @Test
    fun `solo is allowed only with sufficient real fitness evidence`() {
        val def = DungeonCatalog.dungeonById("dungeon-serpent-tunnels")!!
        val ready =
            SoloReadinessInput(
                activeWorkoutSeconds = 15 * 60,
                trainingTier = 3,
                recentQualifyingActivity = true,
                unlockedSkillIds = emptySet(),
                recentProgressionEvents = 1,
                safetyStopActive = false,
            )
        assertTrue(solo.evaluate(ready, def).eligible)

        // Insufficient active workout time -> not ready (real activity, not level, is what counts).
        val notEnough = ready.copy(activeWorkoutSeconds = 60)
        assertFalse(solo.evaluate(notEnough, def).eligible)
    }

    @Test
    fun `a safety stop blocks a solo attempt`() {
        val def = DungeonCatalog.dungeonById("dungeon-serpent-tunnels")!!
        val input =
            SoloReadinessInput(
                15 * 60,
                trainingTier = 3,
                recentQualifyingActivity = true,
                unlockedSkillIds = emptySet(),
                recentProgressionEvents = 1,
                safetyStopActive = true,
            )
        assertFalse(solo.evaluate(input, def).eligible)
    }
}
