package com.ascend.core.domain.training

import com.ascend.core.common.newId
import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.QuestTemplate
import com.ascend.core.model.RecentBaseline
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Equipment‑specific weight increments (smallest practical). Configurable so no gym's
 * plates/stack are assumed. Increments are balancing defaults, not constants.
 */
data class WeightIncrementConfig(
    val upperBodyBarbell: Double = 5.0,
    val lowerBodyBarbell: Double = 10.0,
    val dumbbell: Double = 5.0,
    val machine: Double = 5.0,
    val cable: Double = 5.0,
    val weightedBodyweight: Double = 2.5,
    val default: Double = 5.0,
) {
    fun incrementFor(equipmentKey: String?): Double =
        when (equipmentKey) {
            "upper_body_barbell" -> upperBodyBarbell
            "lower_body_barbell" -> lowerBodyBarbell
            "dumbbell" -> dumbbell
            "machine" -> machine
            "cable" -> cable
            "weighted_bodyweight" -> weightedBodyweight
            else -> default
        }
}

/**
 * Double progression (the default resistance strategy). Load only increases once all
 * working sets reach the top of the rep range; the new prescription uses the
 * **smallest** increment and resets reps toward the bottom of the range.
 */
class LoadProgressionCalculator
    @Inject
    constructor(private val increments: WeightIncrementConfig) {
        constructor() : this(WeightIncrementConfig())

        fun nextLoadPrescription(
            current: ExercisePrescription,
            equipmentKey: String? = null,
            explicitIncrement: Double? = null,
        ): ExercisePrescription {
            val increment = explicitIncrement ?: increments.incrementFor(equipmentKey)
            val newWeight = (current.targetWeight ?: 0.0) + increment
            return current.copy(
                id = newId(),
                targetWeight = newWeight,
                // Reset reps toward the bottom of the range after a load increase.
                effectiveFrom = 0,
                status = "PROPOSED",
            )
        }

        fun incrementFor(equipmentKey: String?): Double = increments.incrementFor(equipmentKey)
    }

/**
 * Daily Quest target progression. Steps the representative baseline up/down by the
 * template's preferred set size, always clamped to the template's configurable
 * [QuestTemplate.minimumTarget]..[QuestTemplate.maximumTarget] range.
 */
class DailyQuestTargetProgressionCalculator
    @Inject
    constructor() {
        fun increasedTarget(
            baseline: RecentBaseline,
            template: QuestTemplate,
        ): Int =
            roundToStep(baseline.representativeTarget + template.defaultPreferredSetSize, template)
                .coerceIn(template.minimumTarget, template.maximumTarget)

        fun reducedTarget(
            baseline: RecentBaseline,
            template: QuestTemplate,
        ): Int =
            roundToStep(baseline.representativeTarget - template.defaultPreferredSetSize, template)
                .coerceIn(template.minimumTarget, template.maximumTarget)

        private fun roundToStep(
            value: Int,
            template: QuestTemplate,
        ): Int {
            val step = template.targetStep.coerceAtLeast(1)
            return (value.toDouble() / step).roundToInt() * step
        }
    }
