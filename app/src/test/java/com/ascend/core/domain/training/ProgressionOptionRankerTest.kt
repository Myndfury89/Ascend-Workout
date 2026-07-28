package com.ascend.core.domain.training

import com.ascend.core.domain.training.ranking.ClassProgressionPreferenceResolver
import com.ascend.core.domain.training.ranking.ProgressionOptionRanker
import com.ascend.core.domain.training.ranking.RankingContext
import com.ascend.core.model.FatigueCost
import com.ascend.core.model.ProgressionCandidate
import com.ascend.core.model.ProgressionDimension
import com.ascend.core.model.ProgressionRecommendationType
import com.ascend.core.model.ProgressionSafetyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionOptionRankerTest {
    private val ranker = ProgressionOptionRanker(ClassProgressionPreferenceResolver())

    private fun candidate(
        type: ProgressionRecommendationType,
        dimension: ProgressionDimension,
        fatigue: FatigueCost = FatigueCost.LOW,
        safety: ProgressionSafetyState = ProgressionSafetyState.OK,
    ) = ProgressionCandidate(
        recommendationType = type,
        dimension = dimension,
        summary = dimension.name,
        fatigueCost = fatigue,
        safetyState = safety,
    )

    private val load = candidate(ProgressionRecommendationType.INCREASE_WEIGHT, ProgressionDimension.LOAD)
    private val reps = candidate(ProgressionRecommendationType.INCREASE_REPS, ProgressionDimension.REPS)
    private val variation = candidate(ProgressionRecommendationType.ADVANCE_VARIATION, ProgressionDimension.VARIATION, FatigueCost.HIGH)
    private val cardioDuration = candidate(ProgressionRecommendationType.INCREASE_DURATION, ProgressionDimension.CARDIO_DURATION)

    @Test
    fun `berserker ranks load progression first when safe`() {
        val ranked = ranker.rank(listOf(reps, cardioDuration, load), RankingContext(primaryClassId = "berserker"))
        assertEquals(ProgressionDimension.LOAD, ranked.primary!!.candidate.dimension)
    }

    @Test
    fun `monk ranks rep or variation progression first when safe`() {
        val ranked = ranker.rank(listOf(cardioDuration, variation, reps), RankingContext(primaryClassId = "monk"))
        assertTrue(ranked.primary!!.candidate.dimension in setOf(ProgressionDimension.REPS, ProgressionDimension.VARIATION))
    }

    @Test
    fun `magician ranks cardio progression first when safe`() {
        val ranked = ranker.rank(listOf(load, reps, cardioDuration), RankingContext(primaryClassId = "magician"))
        assertEquals(ProgressionDimension.CARDIO_DURATION, ranked.primary!!.candidate.dimension)
    }

    @Test
    fun `a secondary class has lower influence than the primary`() {
        // Primary berserker (load), secondary magician (cardio): load still wins.
        val ranked = ranker.rank(listOf(load, cardioDuration), RankingContext(primaryClassId = "berserker", secondaryClassId = "magician"))
        assertEquals(ProgressionDimension.LOAD, ranked.primary!!.candidate.dimension)
        // Swapping the slots flips the primary — proving primary dominates.
        val swapped = ranker.rank(listOf(load, cardioDuration), RankingContext(primaryClassId = "magician", secondaryClassId = "berserker"))
        assertEquals(ProgressionDimension.CARDIO_DURATION, swapped.primary!!.candidate.dimension)
    }

    @Test
    fun `a class cannot promote an unsafe option`() {
        val unsafeLoad =
            candidate(ProgressionRecommendationType.INCREASE_WEIGHT, ProgressionDimension.LOAD, safety = ProgressionSafetyState.BLOCKED)
        val ranked = ranker.rank(listOf(unsafeLoad, reps), RankingContext(primaryClassId = "berserker"))
        assertFalse(ranked.all.any { it.candidate.dimension == ProgressionDimension.LOAD })
        assertEquals(ProgressionDimension.REPS, ranked.primary!!.candidate.dimension)
    }

    @Test
    fun `off-class safe options remain available as alternatives`() {
        val ranked = ranker.rank(listOf(load, cardioDuration), RankingContext(primaryClassId = "berserker"))
        // Cardio is off-class for a berserker but must still be offered, just lower.
        assertTrue(ranked.all.any { it.candidate.dimension == ProgressionDimension.CARDIO_DURATION })
    }

    @Test
    fun `no class yields a neutral ranking with no class influence`() {
        val ranked = ranker.rank(listOf(variation, reps), RankingContext())
        // Lowest fatigue wins (reps LOW over variation HIGH); no class attributed.
        assertEquals(ProgressionDimension.REPS, ranked.primary!!.candidate.dimension)
        assertTrue(ranked.all.all { it.classInfluence == null })
    }

    @Test
    fun `an empty safe set yields no recommendation`() {
        val maintain = candidate(ProgressionRecommendationType.MAINTAIN_PRESCRIPTION, ProgressionDimension.MAINTAIN)
        val ranked = ranker.rank(listOf(maintain), RankingContext(primaryClassId = "monk"))
        assertNull(ranked.primary)
        assertFalse(ranked.hasRecommendation)
    }
}
