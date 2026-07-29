package com.ascend.core.domain.dungeon

import com.ascend.core.domain.skill.SkillCatalog
import com.ascend.core.model.ContributionType
import com.ascend.core.model.DifficultyRating
import com.ascend.core.model.DungeonEnemy
import com.ascend.core.model.DungeonParty
import com.ascend.core.model.DungeonPartyMember
import com.ascend.core.model.EnemyPhase
import com.ascend.core.model.MemberActivityState
import com.ascend.core.model.PartyScaling
import com.ascend.core.model.WorkoutActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DungeonCombatTest {
    private val mapper = WorkoutContributionMapper()
    private val engine = DungeonCombatEngine()
    private val phaseEngine = EnemyPhaseEngine()
    private val rewards = DungeonRewardCalculator()

    private fun action(
        type: WorkoutActionType,
        metric: Long,
        skills: Set<String> = emptySet(),
    ) = WorkoutActionInput("m1", type, metric, "WORKOUT", "s1", skills)

    // ---- deterministic combat ----

    @Test
    fun `applying the same contribution to the same state is deterministic`() {
        val enemy = DungeonCatalog.TUNNEL_SERPENT
        val start = engine.initialState(2000)
        val c = mapper.map(action(WorkoutActionType.STRENGTH_SET, 200))
        val a = engine.apply(start, enemy, c)
        val b = engine.apply(start, enemy, c)
        assertEquals(a.enemyHealth, b.enemyHealth)
        assertEquals(a.totalDamageDealt, b.totalDamageDealt)
    }

    // ---- contribution types map + deal the expected kind of damage ----

    @Test
    fun `a strength set becomes a heavy attack that deals damage`() {
        val enemy = DungeonCatalog.TUNNEL_SERPENT
        val c = mapper.map(action(WorkoutActionType.STRENGTH_SET, 200))
        assertEquals(ContributionType.HEAVY_ATTACK, c.type)
        assertTrue(engine.apply(engine.initialState(2000), enemy, c).totalDamageDealt > 0)
    }

    @Test
    fun `cardio duration becomes sustained damage`() {
        val c = mapper.map(action(WorkoutActionType.CARDIO_DURATION, 900))
        assertEquals(ContributionType.SUSTAINED_DAMAGE, c.type)
        assertTrue(c.magnitude > 0)
    }

    @Test
    fun `a quest interval becomes a combo contribution`() {
        assertEquals(ContributionType.COMBO, mapper.map(action(WorkoutActionType.QUEST_INTERVAL, 100)).type)
    }

    @Test
    fun `a personal record becomes a stronger critical strike than a combo of the same size`() {
        val enemy = plainEnemy()
        val crit = engine.apply(engine.initialState(5000), enemy, mapper.map(action(WorkoutActionType.PERSONAL_RECORD, 100)))
        val combo = engine.apply(engine.initialState(5000), enemy, mapper.map(action(WorkoutActionType.QUEST_INTERVAL, 100)))
        assertTrue("critical strike out-damages a same-size combo", crit.totalDamageDealt > combo.totalDamageDealt)
    }

    @Test
    fun `a recovery action shields and restores stamina without dealing damage`() {
        val enemy = DungeonCatalog.IRON_BEHEMOTH
        val start = engine.initialState(3000)
        val after = engine.apply(start, enemy, mapper.map(action(WorkoutActionType.RECOVERY, 0)))
        assertEquals(start.enemyHealth, after.enemyHealth)
        assertTrue(after.partyShield > 0)
        assertTrue(after.partyStamina > 0)
    }

    @Test
    fun `a weakness match increases damage`() {
        val weak = plainEnemy().copy(weaknessTags = setOf(DungeonElements.PHYSICAL))
        val notWeak = plainEnemy().copy(weaknessTags = emptySet())
        val c = mapper.map(action(WorkoutActionType.STRENGTH_SET, 200))
        val weakDmg = engine.apply(engine.initialState(5000), weak, c).totalDamageDealt
        val plainDmg = engine.apply(engine.initialState(5000), notWeak, c).totalDamageDealt
        assertTrue("weakness deals more", weakDmg > plainDmg)
    }

    @Test
    fun `armor-breaking bypasses armor for more damage`() {
        val armored = plainEnemy().copy(armor = 400)
        val plain = mapper.map(action(WorkoutActionType.STRENGTH_SET, 200))
        val breaking = mapper.map(action(WorkoutActionType.STRENGTH_SET, 200, setOf(SkillCatalog.STRENGTH_BOOST)))
        val plainDmg = engine.apply(engine.initialState(5000), armored, plain).totalDamageDealt
        val breakDmg = engine.apply(engine.initialState(5000), armored, breaking).totalDamageDealt
        assertTrue("armor-break out-damages a normal hit on an armored foe", breakDmg > plainDmg)
    }

    @Test
    fun `strength boost increases contribution magnitude`() {
        val plain = mapper.map(action(WorkoutActionType.STRENGTH_SET, 200))
        val boosted = mapper.map(action(WorkoutActionType.STRENGTH_SET, 200, setOf(SkillCatalog.STRENGTH_BOOST)))
        assertTrue(boosted.magnitude > plain.magnitude)
    }

    // ---- phases ----

    @Test
    fun `enemy phase resolves from health fraction`() {
        val enemy = DungeonCatalog.ABYSSAL_LEVIATHAN
        assertEquals(0, phaseEngine.phaseFor(enemy, 1.0f).index)
        assertEquals(1, phaseEngine.phaseFor(enemy, 0.5f).index)
        assertEquals(2, phaseEngine.phaseFor(enemy, 0.2f).index)
    }

    @Test
    fun `combat ends in victory when health reaches zero`() {
        val enemy = plainEnemy()
        val start = engine.initialState(50)
        val after = engine.apply(start, enemy, mapper.map(action(WorkoutActionType.PERSONAL_RECORD, 10_000)))
        assertTrue(after.defeated)
    }

    // ---- rewards ----

    @Test
    fun `reward splits by earned contribution and is idempotent`() {
        val party =
            DungeonParty(
                members =
                    listOf(
                        member("a", earned = 100),
                        member("b", earned = 300),
                    ),
                solo = false,
            )
        val scaling = PartyScaling(4000, 1000, 300, DifficultyRating.MODERATE, rewardMultiplier = 1.0)
        val def = DungeonCatalog.DUNGEONS.first()

        val first = rewards.award("enc-1", def, party, scaling, alreadyAwarded = false)
        assertTrue(first.playerXp > 0)
        assertTrue("b earned more, so b is rewarded more", first.perMemberXp.getValue("b") > first.perMemberXp.getValue("a"))

        val second = rewards.award("enc-1", def, party, scaling, alreadyAwarded = true)
        assertTrue(second.alreadyAwarded)
        assertEquals(0L, second.playerXp)
    }

    private fun plainEnemy() =
        DungeonEnemy(
            id = "enemy-plain",
            name = "Plain",
            baseHealth = 5000,
            armor = 0,
            weaknessTags = emptySet(),
            phases = listOf(EnemyPhase(0, "Only", 1.0f, 0.0f)),
            description = "",
        )

    private fun member(
        id: String,
        earned: Long,
    ) = DungeonPartyMember(
        id,
        id,
        trainingTier = 3,
        classId = null,
        skillIds = emptySet(),
        activityState = MemberActivityState.ACTIVE,
        earnedContribution = earned,
        lastActivityAt = 0,
    )
}
