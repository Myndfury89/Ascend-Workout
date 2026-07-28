package com.ascend.feature.dashboard.prototype

import com.ascend.core.designsystem.motion.AscendMotionTokens
import com.ascend.core.designsystem.motion.MotionSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure (no-Compose) tests for the prototype's review knobs, motion profiles, and state machine. */
class PrototypeReviewLogicTest {
    // ---- speed profiles derive from the shared MotionSpec ----

    @Test
    fun `speed profiles scale the shared motion tokens`() {
        val standard = MotionSpec(speedScale = MotionSpeed.STANDARD.scale)
        val fast = MotionSpec(speedScale = MotionSpeed.FAST.scale)
        val cinematic = MotionSpec(speedScale = MotionSpeed.CINEMATIC.scale)

        assertEquals(AscendMotionTokens.STANDARD, standard.duration(AscendMotionTokens.STANDARD))
        assertTrue("fast is quicker than standard", fast.duration(AscendMotionTokens.STANDARD) < AscendMotionTokens.STANDARD)
        assertTrue("cinematic is slower than standard", cinematic.duration(AscendMotionTokens.STANDARD) > AscendMotionTokens.STANDARD)
    }

    @Test
    fun `reduced motion is separate from fast and collapses to zero`() {
        // Fast is a real, non-zero duration; reduced motion is a different axis entirely.
        assertTrue(MotionSpec(speedScale = MotionSpeed.FAST.scale).duration(AscendMotionTokens.STANDARD) > 0)
        assertEquals(0, MotionSpec(reducedMotion = true, speedScale = MotionSpeed.FAST.scale).duration(AscendMotionTokens.STANDARD))
        assertEquals(0, MotionSpec(reducedMotion = true, speedScale = MotionSpeed.CINEMATIC.scale).stagger(3))
    }

    // ---- effects quality ----

    @Test
    fun `effects quality maps to the expected switches`() {
        val full = EffectsQuality.FULL.toConfig()
        assertTrue(full.sigilIdle && full.scan && full.glowPulses)
        assertEquals(1f, full.particleQuality, 0f)

        val minimal = EffectsQuality.MINIMAL.toConfig()
        assertFalse(minimal.sigilIdle)
        assertFalse(minimal.scan)
        assertFalse(minimal.glowPulses)
        assertEquals(0f, minimal.particleQuality, 0f)
        assertEquals(0, particleCountFor(minimal.particleQuality))
    }

    @Test
    fun `simplified reduces but does not remove particles`() {
        val simplified = EffectsQuality.SIMPLIFIED.toConfig()
        assertTrue(simplified.particleQuality in 0.01f..0.99f)
        assertTrue(particleCountFor(simplified.particleQuality) in 1..25)
    }

    // ---- controller state machine ----

    @Test
    fun `next and previous cycle the states and restart returns to the first`() {
        val c = PrototypeReviewController()
        c.stateId = StatusPrototypeStateId.STANDARD
        c.nextState()
        assertEquals(StatusPrototypeStateId.entries[1], c.stateId)
        c.previousState()
        assertEquals(StatusPrototypeStateId.STANDARD, c.stateId)
        c.nextState()
        c.restartSequence()
        assertEquals(StatusPrototypeStateId.entries.first(), c.stateId)
    }

    @Test
    fun `the loop is inactive until started and pause suspends advancement`() {
        val c = PrototypeReviewController()
        assertFalse(c.loopActive)
        c.startLoop()
        assertTrue(c.loopActive)
        c.pauseLoop()
        assertFalse("paused loop must not advance", c.loopActive)
        c.resumeLoop()
        assertTrue(c.loopActive)
        c.stopLoop()
        assertFalse(c.loopActive)
    }

    @Test
    fun `everyday-open replays are counted for repeated-use review`() {
        val c = PrototypeReviewController()
        c.entranceMode = EntranceMode.MAJOR_EVENT
        c.replay()
        assertEquals(0, c.everydayReplays)
        c.entranceMode = EntranceMode.EVERYDAY_OPEN
        c.replay()
        c.replay()
        assertEquals(2, c.everydayReplays)
    }

    @Test
    fun `selecting a state bumps the replay key so the entrance re-runs`() {
        val c = PrototypeReviewController()
        val before = c.replayKey
        c.selectState(StatusPrototypeStateId.RANK_PROMOTION)
        assertTrue(c.replayKey > before)
        assertEquals(StatusPrototypeStateId.RANK_PROMOTION, c.stateId)
    }
}
