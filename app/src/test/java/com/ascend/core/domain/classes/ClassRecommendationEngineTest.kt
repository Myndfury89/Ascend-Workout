package com.ascend.core.domain.classes

import com.ascend.core.domain.onboarding.GoalAffinityMapping
import com.ascend.core.model.ActivityTags
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassRecommendationInput
import com.ascend.core.model.onboarding.PrimaryGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recommendation is data‑driven (scored against the class definitions) and
 * explainable — it never locks a class in.
 */
class ClassRecommendationEngineTest {
    private val engine = ClassRecommendationEngine()
    private val defs = ClassCatalog.ALL

    @Test
    fun `powerful muscular goals recommend the Berserker with reasons`() {
        val input =
            ClassRecommendationInput.fromGoals(
                goalPhrases = listOf("Powerful and muscular"),
                goalTags = setOf(ActivityTags.HEAVY_STRENGTH),
            )
        val rec = engine.recommend(input, defs)!!

        assertEquals("berserker", rec.recommended.classId)
        assertTrue("has explainable reasons", rec.recommended.reasons.isNotEmpty())
        // Non-locking: alternatives are offered, ranked below the top.
        assertTrue(rec.alternatives.isNotEmpty())
        assertTrue(rec.recommended.score >= rec.alternatives.first().score)
    }

    @Test
    fun `bodyweight goals recommend the Monk`() {
        val input =
            ClassRecommendationInput.fromGoals(
                goalPhrases = listOf("Skilled with bodyweight movement and calisthenics"),
                goalTags = setOf(ActivityTags.BODYWEIGHT, ActivityTags.BALANCE),
            )
        assertEquals("monk", engine.recommend(input, defs)!!.recommended.classId)
    }

    @Test
    fun `endurance cardio goals recommend the Mage`() {
        val input =
            ClassRecommendationInput.fromGoals(
                goalPhrases = listOf("High endurance and cardio efficiency, running"),
                goalTags = setOf(ActivityTags.STEADY_STATE_CARDIO),
            )
        assertEquals("mage", engine.recommend(input, defs)!!.recommended.classId)
    }

    @Test
    fun `tie between starter and earlier non-starter prefers the starter`() {
        val aegis =
            tieBreakDefinition(
                id = "aegis",
                name = "Aegis",
                tags = setOf(ActivityTags.RECOVERY),
            )
        val input = ClassRecommendationInput(goalTags = setOf(ActivityTags.RECOVERY))
        val rec = engine.recommend(input, listOf(aegis) + defs)!!

        assertEquals("mage", rec.recommended.classId)
        val aegisOption = rec.alternatives.first { it.classId == "aegis" }
        assertEquals(rec.recommended.score, aegisOption.score, 0.0)
    }

    @Test
    fun `strictly higher score wins regardless of starter flag`() {
        val aegis =
            tieBreakDefinition(
                id = "aegis",
                name = "Aegis",
                tags = setOf(ActivityTags.RECOVERY, ActivityTags.COMBAT),
            )
        val input = ClassRecommendationInput(goalTags = setOf(ActivityTags.RECOVERY, ActivityTags.COMBAT))
        val rec = engine.recommend(input, defs + aegis)!!

        assertEquals("aegis", rec.recommended.classId)
        assertTrue(rec.recommended.score > rec.alternatives.first().score)
    }

    @Test
    fun `starter ties resolve by name for catalog order and its reverse`() {
        val input =
            ClassRecommendationInput(
                goalTags = setOf(ActivityTags.MOBILITY, ActivityTags.STEADY_STATE_CARDIO),
            )
        listOf(defs, defs.reversed()).forEach { definitions ->
            val rec = engine.recommend(input, definitions)!!
            assertEquals("mage", rec.recommended.classId)
            assertEquals(listOf("monk", "berserker"), rec.alternatives.map { it.classId })
        }
    }

    @Test
    fun `ranking is independent of input order`() {
        val aegis = tieBreakDefinition(id = "aegis", name = "Aegis")
        val input =
            ClassRecommendationInput(
                goalTags = setOf(ActivityTags.MOBILITY, ActivityTags.STEADY_STATE_CARDIO),
            )
        val expected = listOf("mage", "monk", "berserker", "aegis")
        (defs + aegis).permutations().forEach { definitions ->
            assertEquals(expected, engine.rankedIds(input, definitions))
        }
    }

    @Test
    fun `id breaks a name tie`() {
        val later = tieBreakDefinition(id = "twin-b", name = "Twin", tags = setOf(ActivityTags.COMBAT))
        val earlier = tieBreakDefinition(id = "twin-a", name = "Twin", tags = setOf(ActivityTags.COMBAT))
        val input = ClassRecommendationInput(goalTags = setOf(ActivityTags.COMBAT))
        val rec = engine.recommend(input, listOf(later, earlier))!!

        assertEquals("twin-a", rec.recommended.classId)
        assertEquals(listOf("twin-b"), rec.alternatives.map { it.classId })
        assertEquals(rec.recommended.score, rec.alternatives.single().score, 0.0)
    }

    @Test
    fun `alternatives follow score then starter then name`() {
        val quartz =
            tieBreakDefinition(
                id = "quartz",
                name = "Quartz",
                tags = setOf(ActivityTags.RECOVERY, ActivityTags.COMBAT),
            )
        val aegis = tieBreakDefinition(id = "aegis", name = "Aegis", tags = setOf(ActivityTags.RECOVERY))
        val cedar = tieBreakDefinition(id = "cedar", name = "Cedar")
        val yarrow = tieBreakDefinition(id = "yarrow", name = "Yarrow")
        val input = ClassRecommendationInput(goalTags = setOf(ActivityTags.RECOVERY, ActivityTags.COMBAT))
        val scrambled =
            listOf(
                yarrow,
                cedar,
                ClassCatalog.MONK,
                aegis,
                ClassCatalog.BERSERKER,
                quartz,
                ClassCatalog.MAGE,
            )

        assertEquals(
            listOf("quartz", "mage", "aegis", "berserker", "monk", "cedar", "yarrow"),
            engine.rankedIds(input, scrambled),
        )
    }

    @Test
    fun `three class goldens are unchanged for each primary goal`() {
        val dbOrder = defs.sortedBy { it.name }
        val expected =
            listOf(
                PrimaryGoal.FAT_LOSS to "mage",
                PrimaryGoal.MUSCLE_GAIN to "berserker",
                PrimaryGoal.STRENGTH to "berserker",
                PrimaryGoal.ENDURANCE to "mage",
                PrimaryGoal.SPEED_AGILITY to "berserker",
                PrimaryGoal.MOBILITY to "monk",
                PrimaryGoal.GENERAL_HEALTH to "mage",
                PrimaryGoal.SPORT_PERFORMANCE to "berserker",
                PrimaryGoal.MAINTENANCE to "mage",
            )
        listOf(dbOrder, dbOrder.reversed()).forEach { definitions ->
            expected.forEach { (goal, classId) ->
                val input =
                    ClassRecommendationInput(
                        goalTags = GoalAffinityMapping.goalTags(goal),
                        goalKeywords = GoalAffinityMapping.goalKeywords(goal),
                    )
                assertEquals(goal.name, classId, engine.recommend(input, definitions)!!.recommended.classId)
            }
        }
    }

    @Test
    fun `seven class maintenance tie recommends mage over guardian`() {
        val newcomers =
            listOf(
                tieBreakDefinition(id = "assassin", name = "Assassin", tags = setOf(ActivityTags.EXPLOSIVE)),
                tieBreakDefinition(id = "fighter", name = "Fighter", tags = setOf(ActivityTags.COMBAT)),
                tieBreakDefinition(id = "ranger", name = "Ranger", tags = setOf(ActivityTags.HIGH_INTENSITY_CARDIO)),
                tieBreakDefinition(id = "guardian", name = "Guardian", tags = setOf(ActivityTags.RECOVERY)),
            )
        val seven = newcomers + defs
        val input =
            ClassRecommendationInput(
                goalTags = GoalAffinityMapping.goalTags(PrimaryGoal.MAINTENANCE),
                goalKeywords = GoalAffinityMapping.goalKeywords(PrimaryGoal.MAINTENANCE),
            )
        val expected = listOf("mage", "guardian", "berserker", "monk", "assassin", "fighter", "ranger")
        listOf(seven, seven.sortedBy { it.name }, seven.reversed()).forEach { definitions ->
            val ranked = engine.rankedIds(input, definitions)
            assertEquals(expected, ranked)
            val rec = engine.recommend(input, definitions)!!
            assertEquals(
                rec.recommended.score,
                rec.alternatives.first { it.classId == "guardian" }.score,
                0.0,
            )
        }
    }

    @Test
    fun `disabled definitions are excluded`() {
        val input = ClassRecommendationInput(goalTags = setOf(ActivityTags.RECOVERY))
        val mageDisabled = defs.map { if (it.id == "mage") it.copy(enabled = false) else it }

        assertEquals("berserker", engine.recommend(input, mageDisabled)!!.recommended.classId)
        assertNull(engine.recommend(input, defs.map { it.copy(enabled = false) }))
    }
}

private fun ClassRecommendationEngine.rankedIds(
    input: ClassRecommendationInput,
    definitions: List<ClassDefinition>,
): List<String> {
    val rec = recommend(input, definitions)!!
    return listOf(rec.recommended.classId) + rec.alternatives.map { it.classId }
}

private fun tieBreakDefinition(
    id: String,
    name: String,
    tags: Set<String> = emptySet(),
): ClassDefinition =
    ClassCatalog.MAGE.copy(
        id = id,
        name = name,
        classTitle = name,
        description = "Synthetic class used to test recommendation tie-breaks.",
        fitnessIdentity = "Placeholder role",
        favoredWorkoutCategories = listOf("Placeholder"),
        favoredTags = tags,
        startsAwake = false,
    )

private fun <T> List<T>.permutations(): List<List<T>> =
    if (size <= 1) {
        listOf(this)
    } else {
        flatMap { item ->
            minus(item).permutations().map { rest -> listOf(item) + rest }
        }
    }
