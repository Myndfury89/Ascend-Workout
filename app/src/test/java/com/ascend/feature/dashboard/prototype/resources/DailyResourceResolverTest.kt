package com.ascend.feature.dashboard.prototype.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** HP resolution, progress bands, MP target derivation, and the read-only (no-mutation) guarantee. */
class DailyResourceResolverTest {
    // --- bands ---
    @Test
    fun `bands follow the neutral percentage thresholds`() {
        assertEquals(ResourceBand.LOW, resourceBand(0f))
        assertEquals(ResourceBand.LOW, resourceBand(0.24f))
        assertEquals(ResourceBand.BUILDING, resourceBand(0.25f))
        assertEquals(ResourceBand.BUILDING, resourceBand(0.49f))
        assertEquals(ResourceBand.ACTIVE, resourceBand(0.50f))
        assertEquals(ResourceBand.ACTIVE, resourceBand(0.74f))
        assertEquals(ResourceBand.STRONG, resourceBand(0.75f))
        assertEquals(ResourceBand.STRONG, resourceBand(0.99f))
        assertEquals(ResourceBand.FULL, resourceBand(1.0f))
        assertEquals(ResourceBand.FULL, resourceBand(1.4f))
    }

    // --- HP ---
    @Test
    fun `HP is goal-relative and does not assume 10k`() {
        val hp = DailyHpResolver.resolve(MovementInput(4_000, MovementSource.STEP_RECORDS, 8_000))
        assertEquals(0.5f, hp.progressFraction!!, 0.0001f)
        // A different player with a smaller goal can be Full at fewer steps.
        val other = DailyHpResolver.resolve(MovementInput(6_000, MovementSource.STEP_RECORDS, 6_000))
        assertEquals(1.0f, other.progressFraction!!, 0.0001f)
        assertEquals(ResourceBand.FULL, resourceBand(other.progressFraction!!))
    }

    @Test
    fun `zero steps with a working source is available zero, not unavailable`() {
        val hp = DailyHpResolver.resolve(MovementInput(0, MovementSource.STEP_RECORDS, 8_000))
        assertEquals(ResourceAvailability.AVAILABLE, hp.availability)
        assertEquals(0f, hp.progressFraction!!, 0.0001f)
        assertEquals(ResourceBand.LOW, resourceBand(hp.progressFraction!!))
    }

    @Test
    fun `no movement source is unavailable`() {
        val noSteps = DailyHpResolver.resolve(MovementInput(null, MovementSource.NONE, 8_000))
        assertEquals(ResourceAvailability.UNAVAILABLE, noSteps.availability)
        assertNull(noSteps.progressFraction)
    }

    @Test
    fun `HP with no resolved target has no fraction but is still available`() {
        val hp = DailyHpResolver.resolve(MovementInput(3_000, MovementSource.STEP_RECORDS, null))
        assertEquals(ResourceAvailability.AVAILABLE, hp.availability)
        assertNull(hp.progressFraction)
    }

    // --- MP target ---
    @Test
    fun `a prescribed rest or blocked readiness yields a rest target`() {
        assertTrue(DailyTrainingTargetResolver.resolve(TrainingTargetInput(ReadinessLevel.READY, 45, prescribedRest = true)).isRestDay)
        assertTrue(DailyTrainingTargetResolver.resolve(TrainingTargetInput(ReadinessLevel.BLOCKED, 45, prescribedRest = false)).isRestDay)
    }

    @Test
    fun `readiness maps to the training-target shape`() {
        assertEquals(
            DailyMpTargetType.TRAIN,
            DailyTrainingTargetResolver.resolve(TrainingTargetInput(ReadinessLevel.READY, 45, false)).type,
        )
        assertEquals(DailyMpTargetType.DELOAD, DailyTrainingTargetResolver.resolve(TrainingTargetInput(ReadinessLevel.LOW, 45, false)).type)
        val recovery = DailyTrainingTargetResolver.resolve(TrainingTargetInput(ReadinessLevel.RECOVERY, 45, false))
        assertEquals(DailyMpTargetType.RECOVERY, recovery.type)
        assertTrue("recovery is a lighter, non-rest target", recovery.targetMinutes in 1 until 45)
    }

    // --- read-only guarantee ---
    @Test
    fun `the resource layer contains no progression mutation calls`() {
        val dir = File("src/main/java/com/ascend/feature/dashboard/prototype/resources")
        assertTrue("resolver source dir not found from ${File(".").absolutePath}", dir.exists())
        val forbidden = listOf("awardXp", "awardAttributes", "unlockSkill", "completeQuest", "completeWorkout")
        dir.walk().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            forbidden.forEach { symbol ->
                assertTrue("${file.name} must not reference $symbol (read-only layer)", !text.contains(symbol))
            }
        }
    }

    // --- XP mirror ---
    @Test
    fun `XP state mirrors PlayerProgress values and computes level fraction`() {
        val xp = XpState(level = 27, currentLevelXp = 4_148, xpForNextLevel = 6_200)
        assertEquals(27, xp.level)
        assertEquals(4_148L, xp.currentLevelXp)
        assertEquals(6_200L, xp.xpForNextLevel)
        assertEquals(4_148f / 6_200f, xp.progressFraction, 0.0001f)
    }
}
