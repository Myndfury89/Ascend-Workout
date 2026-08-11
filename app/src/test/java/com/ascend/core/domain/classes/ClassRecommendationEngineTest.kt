package com.ascend.core.domain.classes

import com.ascend.core.model.ActivityTags
import com.ascend.core.model.ClassRecommendationInput
import org.junit.Assert.assertEquals
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
}
