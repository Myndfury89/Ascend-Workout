package com.ascend.core.domain.progression

import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.ProgressionSnapshot
import com.ascend.core.model.Rank
import com.ascend.core.model.XpSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEventFactoryTest {
    private val factory = ProgressionEventFactory()

    private fun snapshot(
        level: Int,
        lifetimeXp: Long,
        rank: Rank,
        attributes: Map<AttributeType, Long>,
    ) = ProgressionSnapshot(level, lifetimeXp, rank, attributes)

    @Test
    fun `builds an ordered chain of only the things that changed`() {
        val before =
            snapshot(
                5,
                4_600,
                Rank.IRON,
                mapOf(
                    AttributeType.STRENGTH to 48,
                    AttributeType.ENDURANCE to 34,
                    AttributeType.AGILITY to 26,
                    AttributeType.DISCIPLINE to 61,
                    AttributeType.RECOVERY to 20,
                ),
            )
        val after =
            snapshot(
                6,
                5_500,
                Rank.BRONZE,
                mapOf(
                    AttributeType.STRENGTH to 93,
                    AttributeType.ENDURANCE to 49,
                    AttributeType.AGILITY to 26,
                    AttributeType.DISCIPLINE to 81,
                    AttributeType.RECOVERY to 20,
                ),
            )

        val events = factory.build("u1", XpSourceType.QUEST_COMPLETION, "quest-1", before, after, label = "Ascension Trial")

        // XP, the two changed attributes in stable order, then level‑up, then rank‑up.
        assertEquals(
            listOf(
                ProgressionEventType.XP_GAINED,
                ProgressionEventType.ATTRIBUTE_CHANGED,
                ProgressionEventType.ATTRIBUTE_CHANGED,
                ProgressionEventType.ATTRIBUTE_CHANGED,
                ProgressionEventType.LEVEL_UP,
                ProgressionEventType.RANK_UP,
            ),
            events.map { it.type },
        )
        // Sequence is dense and ascending; batch id is the source id.
        assertEquals(listOf(0, 1, 2, 3, 4, 5), events.map { it.sequence })
        assertTrue(events.all { it.batchId == "quest-1" })

        val xp = events[0]
        assertEquals(4_600L, xp.fromValue)
        assertEquals(5_500L, xp.toValue)
        assertEquals(900L, xp.delta)

        val attrs = events.filter { it.type == ProgressionEventType.ATTRIBUTE_CHANGED }
        assertEquals(listOf(AttributeType.STRENGTH, AttributeType.ENDURANCE, AttributeType.DISCIPLINE), attrs.map { it.attribute })
        assertEquals(48L to 93L, attrs[0].fromValue to attrs[0].toValue)

        val levelUp = events.first { it.type == ProgressionEventType.LEVEL_UP }
        assertEquals(5L to 6L, levelUp.fromValue to levelUp.toValue)

        val rankUp = events.first { it.type == ProgressionEventType.RANK_UP }
        assertEquals(Rank.IRON.ordinal.toLong() to Rank.BRONZE.ordinal.toLong(), rankUp.fromValue to rankUp.toValue)
        assertEquals("Ascension Trial", rankUp.label)
    }

    @Test
    fun `no change produces no events`() {
        val same =
            snapshot(3, 900, Rank.INITIATE, mapOf(AttributeType.STRENGTH to 10))
        assertTrue(factory.build("u1", XpSourceType.QUEST_COMPLETION, "q", same, same).isEmpty())
    }

    @Test
    fun `a light gain emits xp and attributes but no level or rank beat`() {
        val before = snapshot(5, 4_600, Rank.IRON, mapOf(AttributeType.STRENGTH to 48, AttributeType.DISCIPLINE to 61))
        val after = snapshot(5, 4_950, Rank.IRON, mapOf(AttributeType.STRENGTH to 60, AttributeType.DISCIPLINE to 71))

        val types = factory.build("u1", XpSourceType.QUEST_COMPLETION, "q2", before, after).map { it.type }

        assertEquals(
            listOf(ProgressionEventType.XP_GAINED, ProgressionEventType.ATTRIBUTE_CHANGED, ProgressionEventType.ATTRIBUTE_CHANGED),
            types,
        )
    }
}
