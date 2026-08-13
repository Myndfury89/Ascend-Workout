package com.ascend.core.domain.build

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildAffinityCalculatorTest {
    private val calc = BuildAffinityCalculator()

    private fun cs(
        c: BuildCharacteristic,
        score: Double,
        state: EvidenceState,
        confidence: Double = 0.8,
    ): CharacteristicScore = CharacteristicScore(c, score, state, BuildTrend.UNKNOWN, confidence)

    private fun profile(vararg scores: CharacteristicScore): BuildProfile =
        BuildProfile(
            scores = scores.associateBy { it.characteristic },
            overallConfidence = 0.8,
            windowDays = 28,
        )

    private fun BuildAffinityResult.of(buildClass: BuildClass): ClassAffinity = ranked.first { it.buildClass == buildClass }

    @Test
    fun `all seven classes are ranked`() {
        val result = calc.calculate(profile(cs(BuildCharacteristic.STRENGTH, 80.0, EvidenceState.OK)))
        assertEquals(7, result.ranked.size)
    }

    @Test
    fun `a confident lifter is dominant Berserker and never a dominant Ranger`() {
        val lifter =
            profile(
                cs(BuildCharacteristic.STRENGTH, 85.0, EvidenceState.OK, confidence = 0.9),
                cs(BuildCharacteristic.VERSATILITY, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.ENDURANCE, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.SPEED, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.DISTANCE, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.RECOVERY, 0.0, EvidenceState.UNAVAILABLE, confidence = 0.0),
            )
        val result = calc.calculate(lifter)
        assertNotNull(result.dominant)
        assertEquals(BuildClass.BERSERKER, result.dominant!!.buildClass)
        assertTrue(result.of(BuildClass.BERSERKER).dominantEligible)
        // Ranger's defining characteristics (Endurance/Distance) are only confident ZEROs -> not dominant.
        assertFalse(result.of(BuildClass.RANGER).dominantEligible)
    }

    @Test
    fun `a runner is dominant toward a cardio class and never a dominant Berserker`() {
        val runner =
            profile(
                cs(BuildCharacteristic.STRENGTH, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.VERSATILITY, 55.0, EvidenceState.OK),
                cs(BuildCharacteristic.ENDURANCE, 70.0, EvidenceState.OK),
                cs(BuildCharacteristic.SPEED, 60.0, EvidenceState.OK),
                cs(BuildCharacteristic.DISTANCE, 65.0, EvidenceState.OK),
                cs(BuildCharacteristic.RECOVERY, 0.0, EvidenceState.UNAVAILABLE, confidence = 0.0),
            )
        val result = calc.calculate(runner)
        assertNotNull(result.dominant)
        assertTrue(result.dominant!!.buildClass in setOf(BuildClass.RANGER, BuildClass.MAGE))
        assertFalse(result.of(BuildClass.BERSERKER).dominantEligible)
    }

    @Test
    fun `a single logged run (insufficient cardio) cannot crown a cardio class`() {
        val spike =
            profile(
                cs(BuildCharacteristic.STRENGTH, 85.0, EvidenceState.OK, confidence = 0.9),
                cs(BuildCharacteristic.VERSATILITY, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.ENDURANCE, 90.0, EvidenceState.INSUFFICIENT, confidence = 0.2),
                cs(BuildCharacteristic.SPEED, 90.0, EvidenceState.INSUFFICIENT, confidence = 0.2),
                cs(BuildCharacteristic.DISTANCE, 95.0, EvidenceState.INSUFFICIENT, confidence = 0.2),
                cs(BuildCharacteristic.RECOVERY, 0.0, EvidenceState.UNAVAILABLE, confidence = 0.0),
            )
        val result = calc.calculate(spike)
        assertEquals(BuildClass.BERSERKER, result.dominant!!.buildClass)
        assertFalse(result.of(BuildClass.RANGER).dominantEligible)
        assertFalse(result.of(BuildClass.MAGE).dominantEligible)
        // Coverage collapses because the defining cardio characteristics are excluded.
        assertTrue(result.of(BuildClass.RANGER).coverage < 0.5)
    }

    @Test
    fun `at the modeled lifter density, high coverage is still gated by the defining characteristic`() {
        // Confirms the two gates against P0-density personas: an active lifter's absent cardio reads as
        // ZERO (evidenced), so Ranger's coverage is high — but its defining axis (Endurance) is only a
        // ZERO, not OK, so the defining-characteristic gate keeps it off the dominant slot. This is the
        // check behind the Coverage >= 0.5 default, encoded rather than asserted in prose.
        val activeLifter =
            profile(
                cs(BuildCharacteristic.STRENGTH, 85.0, EvidenceState.OK, confidence = 0.9),
                cs(BuildCharacteristic.VERSATILITY, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.ENDURANCE, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.SPEED, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.DISTANCE, 0.0, EvidenceState.ZERO),
                cs(BuildCharacteristic.RECOVERY, 0.0, EvidenceState.UNAVAILABLE, confidence = 0.0),
            )
        val ranger = calc.calculate(activeLifter).of(BuildClass.RANGER)
        assertEquals(0.875, ranger.coverage, 0.0001) // well above the 0.5 gate...
        assertFalse(ranger.dominantEligible) // ...yet blocked, because Endurance is a ZERO, not OK
    }

    @Test
    fun `missing evidence neither inflates nor deflates affinity - it lowers coverage`() {
        // Only Strength evidenced (OK); everything else UNAVAILABLE (excluded from affinity).
        val sparse =
            profile(
                cs(BuildCharacteristic.STRENGTH, 80.0, EvidenceState.OK),
                cs(BuildCharacteristic.VERSATILITY, 0.0, EvidenceState.UNAVAILABLE, confidence = 0.0),
                cs(BuildCharacteristic.ENDURANCE, 0.0, EvidenceState.UNAVAILABLE, confidence = 0.0),
                cs(BuildCharacteristic.SPEED, 0.0, EvidenceState.UNAVAILABLE, confidence = 0.0),
                cs(BuildCharacteristic.DISTANCE, 0.0, EvidenceState.UNAVAILABLE, confidence = 0.0),
                cs(BuildCharacteristic.RECOVERY, 0.0, EvidenceState.UNAVAILABLE, confidence = 0.0),
            )
        val guardian = calc.calculate(sparse).of(BuildClass.GUARDIAN)
        // Renormalized affinity = pure Strength score (0.8); coverage is only Strength's slice of the signature.
        assertEquals(0.8, guardian.affinity, 0.0001)
        assertTrue("coverage should be well under full", guardian.coverage < 0.5)
        assertFalse("low coverage cannot be a dominant affinity", guardian.dominantEligible)
    }
}
