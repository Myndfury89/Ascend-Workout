package com.ascend.feature.dashboard

import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEvent
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.Rank
import com.ascend.core.model.XpSourceType
import com.ascend.feature.dashboard.prototype.RankTier
import com.ascend.feature.dashboard.prototype.StatusClassVariant
import com.ascend.feature.dashboard.prototype.StatusOverlayKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The real→visual contract for the production ornate Status composition (pure, no Compose/DB). */
class StatusCompositionTest {
    private fun domain(
        level: Int = 7,
        rank: Rank = Rank.GOLD,
        currentLevelXp: Long = 300,
        xpToNextLevel: Long = 500,
        attributes: Map<AttributeType, Long> = mapOf(AttributeType.STRENGTH to 40L, AttributeType.RECOVERY to 12L),
    ) = StatusDomain(
        level = level,
        rank = rank,
        lifetimeXp = 9999,
        currentLevelXp = currentLevelXp,
        xpToNextLevel = xpToNextLevel,
        progressFraction = 0.6f,
        attributes = attributes,
    )

    private fun event(
        type: ProgressionEventType,
        attribute: AttributeType? = null,
        sequence: Int = 0,
    ) = ProgressionEvent(
        id = "e$sequence-${type.name}",
        userId = "u",
        batchId = "b1",
        sequence = sequence,
        type = type,
        sourceType = XpSourceType.QUEST_COMPLETION,
        sourceId = "q1",
        attribute = attribute,
    )

    @Test
    fun `ranks map one to one onto sigil tiers`() {
        Rank.entries.forEach { rank ->
            assertEquals(RankTier.entries[rank.ordinal], StatusComposition.tierOf(rank))
        }
        assertEquals(RankTier.GOLD, StatusComposition.tierOf(Rank.GOLD))
        assertEquals(RankTier.MYTHIC, StatusComposition.tierOf(Rank.MYTHIC))
    }

    @Test
    fun `class ids resolve to sigil variants, unknown is neutral`() {
        assertEquals(StatusClassVariant.BERSERKER, StatusComposition.variantOf("berserker"))
        assertEquals(StatusClassVariant.MONK, StatusComposition.variantOf("monk"))
        assertEquals(StatusClassVariant.MAGICIAN, StatusComposition.variantOf("magician"))
        assertEquals(StatusClassVariant.NEUTRAL, StatusComposition.variantOf(null))
        assertEquals(StatusClassVariant.NEUTRAL, StatusComposition.variantOf("mystery"))
    }

    @Test
    fun `overlay picks the most celebratory event in the batch`() {
        // Rank up outranks everything else present.
        val overlay =
            StatusComposition.overlayFor(
                listOf(
                    event(ProgressionEventType.XP_GAINED, sequence = 0),
                    event(ProgressionEventType.ATTRIBUTE_CHANGED, AttributeType.STRENGTH, 1),
                    event(ProgressionEventType.LEVEL_UP, sequence = 2),
                    event(ProgressionEventType.RANK_UP, sequence = 3),
                ),
            )
        assertEquals(StatusOverlayKind.RANK_PROMOTION, overlay.kind)
    }

    @Test
    fun `attribute overlay carries the changed attribute`() {
        val overlay =
            StatusComposition.overlayFor(
                listOf(
                    event(ProgressionEventType.XP_GAINED, sequence = 0),
                    event(ProgressionEventType.ATTRIBUTE_CHANGED, AttributeType.RECOVERY, 1),
                ),
            )
        assertEquals(StatusOverlayKind.ATTRIBUTE, overlay.kind)
        assertEquals("Recovery", overlay.emphasizedAttribute)
    }

    @Test
    fun `empty batch yields no overlay and no major unlock`() {
        assertEquals(StatusOverlayKind.NONE, StatusComposition.overlayFor(emptyList()).kind)
        assertFalse(StatusComposition.isMajorUnlock(emptyList()))
    }

    @Test
    fun `level up is a major unlock, plain xp is not`() {
        assertTrue(StatusComposition.isMajorUnlock(listOf(event(ProgressionEventType.LEVEL_UP))))
        assertTrue(StatusComposition.isMajorUnlock(listOf(event(ProgressionEventType.CLASS_LEVEL_UP))))
        assertFalse(StatusComposition.isMajorUnlock(listOf(event(ProgressionEventType.XP_GAINED))))
    }

    @Test
    fun `map projects real identity, xp and attributes in canonical order`() {
        val data =
            StatusComposition.map(
                hunterName = "Kaiden",
                domain = domain(),
                classInfo = StatusClassInfo.NEUTRAL,
                batch = emptyList(),
                reducedMotion = false,
            )
        assertEquals("Kaiden", data.hunterName)
        assertEquals(7, data.level)
        assertEquals("Gold", data.rankLabel)
        assertEquals(RankTier.GOLD, data.rankTier)
        assertEquals(300, data.playerXpInLevel)
        assertEquals(500, data.playerXpForLevel)
        assertEquals(0.6f, data.playerXpFraction, 0.001f)
        // Five attributes, canonical order, real values (missing ⇒ 0).
        assertEquals(AttributeType.entries.map { it.displayName }, data.attributes.map { it.name })
        assertEquals(40, data.attributes.first { it.name == "Strength" }.value)
        assertEquals(0, data.attributes.first { it.name == "Agility" }.value)
        // Neutral: no class layers.
        assertNull(data.secondaryVariant)
        assertFalse(data.showProficiencyMedallion)
        assertEquals(-1, data.activeMedallionIndex)
    }

    @Test
    fun `map surfaces the class ring, proficiency and emphasized medallion`() {
        val classInfo =
            StatusClassInfo(
                variant = StatusClassVariant.MONK,
                classLevel = 4,
                classXpInLevel = 60,
                classXpForLevel = 120,
                uniqueProficiency = 33,
                secondaryVariant = StatusClassVariant.MAGICIAN,
                secondaryXpInLevel = 10,
                secondaryXpForLevel = 100,
            )
        val data =
            StatusComposition.map(
                hunterName = "Kaiden",
                domain = domain(),
                classInfo = classInfo,
                batch = listOf(event(ProgressionEventType.ATTRIBUTE_CHANGED, AttributeType.DISCIPLINE)),
                reducedMotion = false,
            )
        assertEquals(StatusClassVariant.MONK, data.variant)
        assertEquals(4, data.classLevel)
        assertEquals(0.5f, data.classXpFraction, 0.001f)
        assertEquals(33, data.uniqueProficiency)
        assertTrue(data.showProficiencyMedallion)
        assertEquals(StatusClassVariant.MAGICIAN, data.secondaryVariant)
        assertEquals(0.1f, data.secondaryClassXpFraction!!, 0.001f)
        // Discipline is index 3 in canonical order and is the emphasized medallion.
        assertEquals(3, data.activeMedallionIndex)
        assertTrue(data.attributes[3].emphasized)
    }
}
