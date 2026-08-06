package com.ascend.feature.dashboard.prototype

import com.ascend.core.designsystem.motion.AscendVerb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The reduced-motion recognition cue fires for Reward-tier events (never a silent PR). */
class RecognitionCueTest {
    @Test
    fun `reward-tier overlays qualify for the recognition cue`() {
        listOf(
            StatusOverlayKind.ATTRIBUTE,
            StatusOverlayKind.PROFICIENCY,
            StatusOverlayKind.QUEST_PROGRESS,
            StatusOverlayKind.QUEST_COMPLETE,
            StatusOverlayKind.PERSONAL_RECORD,
        ).forEach { assertTrue("$it is a reward event", isRewardOverlay(it)) }
    }

    @Test
    fun `a personal record is never silent under reduced motion`() {
        assertTrue(isRewardOverlay(StatusOverlayKind.PERSONAL_RECORD))
    }

    @Test
    fun `entrance and ascension-only overlays do not trigger the reward cue`() {
        assertFalse(isRewardOverlay(StatusOverlayKind.NONE))
        assertFalse(isRewardOverlay(StatusOverlayKind.PLAYER_LEVEL_UP))
        assertFalse(isRewardOverlay(StatusOverlayKind.RANK_PROMOTION))
    }

    @Test
    fun `the recognition cue uses the documented brief duration`() {
        assertEquals(320, AscendVerb.RECOGNITION_CUE_MS)
    }
}
