package com.ascend.feature.dashboard

import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEvent
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.Rank
import com.ascend.core.model.XpSourceType
import com.ascend.feature.dashboard.prototype.StatusClassVariant
import com.ascend.feature.dashboard.prototype.StatusOverlayKind
import com.ascend.feature.dashboard.prototype.isRewardOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The quest-completion presentation contract (pure, no Compose / DB): a quest-sourced batch is
 * identified by `sourceType` (never by XP/attribute types), presents Quest Complete first carrying
 * the real title/identity + a deltas-only reward summary, then the resulting Ascension beat — and a
 * non-quest batch stays a single-step plan identical to [StatusComposition.overlayFor].
 */
class QuestCompletionPresentationTest {
    private fun ev(
        type: ProgressionEventType,
        sourceType: XpSourceType = XpSourceType.QUEST_COMPLETION,
        attribute: AttributeType? = null,
        from: Long = 0,
        to: Long = 0,
        label: String? = "200 Push-Ups",
        sourceId: String = "q1",
        seq: Int = 0,
    ) = ProgressionEvent(
        id = "e$seq-${type.name}",
        userId = "u",
        batchId = "b1",
        sequence = seq,
        type = type,
        sourceType = sourceType,
        sourceId = sourceId,
        attribute = attribute,
        fromValue = from,
        toValue = to,
        label = label,
    )

    @Test
    fun `a quest-completion source produces a Quest Complete presentation`() {
        val plan = StatusComposition.planFor(listOf(ev(ProgressionEventType.XP_GAINED, from = 0, to = 350)))
        assertNotNull("a quest source must present Quest Complete", plan.questComplete)
    }

    @Test
    fun `a non-quest xp gain does not produce a Quest Complete presentation`() {
        val batch = listOf(ev(ProgressionEventType.XP_GAINED, sourceType = XpSourceType.WORKOUT_COMPLETION, from = 0, to = 120))
        val plan = StatusComposition.planFor(batch)
        assertNull(plan.questComplete)
        assertEquals(StatusOverlayKind.PLAYER_XP, plan.panelOverlay.kind)
    }

    @Test
    fun `the quest title comes from the event label`() {
        val plan = StatusComposition.planFor(listOf(ev(ProgressionEventType.XP_GAINED, label = "Morning Mobility")))
        assertEquals("Morning Mobility", plan.questComplete!!.title)
    }

    @Test
    fun `a blank or missing label falls back to a default quest name`() {
        val plan = StatusComposition.planFor(listOf(ev(ProgressionEventType.XP_GAINED, label = null)))
        assertEquals("Daily Quest", plan.questComplete!!.title)
    }

    @Test
    fun `the quest identity comes from sourceId`() {
        val plan = StatusComposition.planFor(listOf(ev(ProgressionEventType.XP_GAINED, sourceId = "quest-77")))
        assertEquals("quest-77", plan.questComplete!!.questId)
    }

    @Test
    fun `the reward summary is read only from batch deltas, never recomputed`() {
        val batch =
            listOf(
                ev(ProgressionEventType.XP_GAINED, from = 100, to = 450, seq = 0),
                ev(ProgressionEventType.ATTRIBUTE_CHANGED, attribute = AttributeType.STRENGTH, from = 10, to = 40, seq = 1),
            )
        val summary = StatusComposition.planFor(batch).questComplete!!
        assertEquals(350, summary.xpGained)
        assertEquals(listOf("Strength" to 30), summary.attributeGains.map { it.name to it.amount })
    }

    @Test
    fun `quest completion is presented before the resulting player level up`() {
        val batch =
            listOf(
                ev(ProgressionEventType.XP_GAINED, from = 0, to = 350, seq = 0),
                ev(ProgressionEventType.LEVEL_UP, from = 1, to = 2, seq = 1),
            )
        val plan = StatusComposition.planFor(batch)
        assertEquals(2, plan.steps.size)
        assertNotNull("Quest Complete is the first step", plan.steps[0].questComplete)
        assertEquals(StatusOverlayKind.PLAYER_LEVEL_UP, plan.steps[1].overlay.kind)
        assertEquals(StatusOverlayKind.PLAYER_LEVEL_UP, plan.ascensionOverlay!!.kind)
        // The panel renders the Ascension (its burst/flash), so the level-up is NOT suppressed.
        assertEquals(StatusOverlayKind.PLAYER_LEVEL_UP, plan.panelOverlay.kind)
    }

    @Test
    fun `a rank up is not suppressed by the quest window`() {
        val batch =
            listOf(
                ev(ProgressionEventType.XP_GAINED, seq = 0),
                ev(ProgressionEventType.RANK_UP, from = 2, to = 3, seq = 1),
            )
        val plan = StatusComposition.planFor(batch)
        assertNotNull(plan.questComplete)
        assertEquals(StatusOverlayKind.RANK_PROMOTION, plan.ascensionOverlay!!.kind)
    }

    @Test
    fun `only one ascension beat is emitted for a quest batch, the highest tier present`() {
        val batch =
            listOf(
                ev(ProgressionEventType.XP_GAINED, seq = 0),
                ev(ProgressionEventType.CLASS_LEVEL_UP, seq = 1),
                ev(ProgressionEventType.LEVEL_UP, seq = 2),
                ev(ProgressionEventType.RANK_UP, seq = 3),
            )
        val plan = StatusComposition.planFor(batch)
        // Quest Complete + exactly one ascension — no duplicate visual events from one batch.
        assertEquals(2, plan.steps.size)
        assertEquals(StatusOverlayKind.RANK_PROMOTION, plan.ascensionOverlay!!.kind)
    }

    @Test
    fun `ordinary attribute and xp beats are not replayed separately for a quest batch`() {
        val batch =
            listOf(
                ev(ProgressionEventType.XP_GAINED, from = 0, to = 120, seq = 0),
                ev(ProgressionEventType.ATTRIBUTE_CHANGED, attribute = AttributeType.STRENGTH, from = 0, to = 6, seq = 1),
            )
        val plan = StatusComposition.planFor(batch)
        // A single step (Quest Complete) — the attribute/xp are summarised inside the window.
        assertEquals(1, plan.steps.size)
        assertEquals(StatusOverlayKind.QUEST_COMPLETE, plan.panelOverlay.kind)
    }

    @Test
    fun `a quest-only batch presents a reward overlay so the reduced-motion cue is never silent`() {
        val plan = StatusComposition.planFor(listOf(ev(ProgressionEventType.XP_GAINED)))
        assertTrue(isRewardOverlay(plan.panelOverlay.kind))
    }

    @Test
    fun `partial and over-completion quest sources also present as quest complete`() {
        listOf(XpSourceType.QUEST_PARTIAL, XpSourceType.QUEST_OVER_COMPLETION).forEach { source ->
            val plan = StatusComposition.planFor(listOf(ev(ProgressionEventType.XP_GAINED, sourceType = source)))
            assertNotNull("$source presents Quest Complete", plan.questComplete)
        }
    }

    @Test
    fun `a non-quest batch is a single-step plan identical to overlayFor`() {
        val cases =
            listOf(
                listOf(ev(ProgressionEventType.LEVEL_UP, sourceType = XpSourceType.WORKOUT_COMPLETION)),
                listOf(ev(ProgressionEventType.RANK_UP, sourceType = XpSourceType.WORKOUT_COMPLETION)),
                listOf(
                    ev(
                        ProgressionEventType.ATTRIBUTE_CHANGED,
                        sourceType = XpSourceType.WORKOUT_COMPLETION,
                        attribute = AttributeType.AGILITY,
                    ),
                ),
            )
        cases.forEach { batch ->
            val plan = StatusComposition.planFor(batch)
            assertNull(plan.questComplete)
            assertEquals(1, plan.steps.size)
            assertEquals(StatusComposition.overlayFor(batch).kind, plan.panelOverlay.kind)
            assertEquals(StatusComposition.overlayFor(batch).emphasizedAttribute, plan.panelOverlay.emphasizedAttribute)
        }
    }

    @Test
    fun `map exposes the quest completion and preserves the emphasized medallion`() {
        val domain =
            StatusDomain(
                level = 7,
                rank = Rank.GOLD,
                lifetimeXp = 9999,
                currentLevelXp = 300,
                xpToNextLevel = 500,
                progressFraction = 0.6f,
                attributes = mapOf(AttributeType.STRENGTH to 40L),
            )
        val data =
            StatusComposition.map(
                hunterName = "Kaiden",
                domain = domain,
                classInfo = StatusClassInfo.NEUTRAL,
                batch =
                    listOf(
                        ev(ProgressionEventType.XP_GAINED, from = 0, to = 350, seq = 0),
                        ev(ProgressionEventType.ATTRIBUTE_CHANGED, attribute = AttributeType.STRENGTH, from = 0, to = 8, seq = 1),
                    ),
                reducedMotion = false,
            )
        assertEquals(StatusClassVariant.NEUTRAL, data.variant)
        assertNotNull(data.questComplete)
        assertEquals(StatusOverlayKind.QUEST_COMPLETE, data.overlay.kind)
        // Strength is index 0 in canonical order and stays the emphasized medallion.
        assertEquals(0, data.activeMedallionIndex)
        assertTrue(data.attributes[0].emphasized)
    }
}
