package com.ascend.core.domain.training.ranking

import com.ascend.core.model.ActivityTags
import com.ascend.core.model.ClassProgressionPreference
import com.ascend.core.model.ProgressionDimension
import com.ascend.core.model.ProgressionRecommendationType

/**
 * The initial class progression preferences as **configurable seed data**. Ranking reads a
 * preference; it never branches on a class id. Adding a class means adding a row here (or,
 * later, a DB entry), with no change to the ranker.
 */
object ClassProgressionPreferenceCatalog {
    val BERSERKER =
        ClassProgressionPreference(
            classId = "berserker",
            // Load first, then strength-oriented reps, compound sets, longer rest, PR opportunities.
            favoredDimensions =
                listOf(
                    ProgressionDimension.LOAD,
                    ProgressionDimension.EXTERNAL_LOAD,
                    ProgressionDimension.REPS,
                    ProgressionDimension.SETS,
                    ProgressionDimension.REST,
                ),
            // Heavy compound work should keep its rest — don't favour cutting it.
            discouragedRecommendationTypes = setOf(ProgressionRecommendationType.REDUCE_REST),
            favoredTags = setOf(ActivityTags.HEAVY_STRENGTH, ActivityTags.HYPERTROPHY),
            rewardModifier = 1.0,
        )

    val MONK =
        ClassProgressionPreference(
            classId = "monk",
            favoredDimensions =
                listOf(
                    ProgressionDimension.REPS,
                    ProgressionDimension.VARIATION,
                    ProgressionDimension.ASSISTANCE,
                    ProgressionDimension.TEMPO,
                    ProgressionDimension.RANGE_OF_MOTION,
                    ProgressionDimension.SETS,
                    ProgressionDimension.REST,
                ),
            favoredTags = setOf(ActivityTags.BODYWEIGHT, ActivityTags.MOBILITY, ActivityTags.BALANCE),
            rewardModifier = 1.0,
        )

    val MAGE =
        ClassProgressionPreference(
            classId = "mage",
            favoredDimensions =
                listOf(
                    ProgressionDimension.CARDIO_DURATION,
                    ProgressionDimension.CARDIO_PACE,
                    ProgressionDimension.CARDIO_DISTANCE,
                    ProgressionDimension.CARDIO_INTERVAL,
                    ProgressionDimension.REST,
                ),
            favoredTags =
                setOf(
                    ActivityTags.STEADY_STATE_CARDIO,
                    ActivityTags.HIGH_INTENSITY_CARDIO,
                    ActivityTags.BREATH_CONTROL,
                    ActivityTags.RECOVERY,
                ),
            rewardModifier = 1.0,
        )

    val ALL: List<ClassProgressionPreference> = listOf(BERSERKER, MONK, MAGE)

    fun byId(classId: String?): ClassProgressionPreference? = ALL.firstOrNull { it.classId == classId }
}
