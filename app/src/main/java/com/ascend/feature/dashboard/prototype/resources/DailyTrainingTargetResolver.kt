package com.ascend.feature.dashboard.prototype.resources

import kotlin.math.roundToInt

/*
 * Derives today's MP (training-duration) target from training readiness — a stand-in for a
 * first-class "recommended minutes" field, which does not yet exist. No persistence: pure mapping
 * from readiness + a recent baseline to a target and its prescribed shape. A prescribed rest (or a
 * blocked readiness) yields a REST target that the HUD renders as a Recovery day, not 0/0.
 */

/** Coarse readiness input (maps from the real TrainingReadinessCalculator state when wired later). */
enum class ReadinessLevel { READY, LIMITING, LOW, RECOVERY, BLOCKED }

data class TrainingTargetInput(
    val readiness: ReadinessLevel,
    val baselineMinutes: Int,
    val prescribedRest: Boolean,
)

object DailyTrainingTargetResolver {
    private const val MAINTAIN_FACTOR = 0.8f
    private const val DELOAD_FACTOR = 0.6f
    private const val RECOVERY_FACTOR = 0.4f

    fun resolve(input: TrainingTargetInput): DailyMpTarget {
        if (input.prescribedRest || input.readiness == ReadinessLevel.BLOCKED) {
            return DailyMpTarget(0, DailyMpTargetType.REST, "Rest prescribed", isRestDay = true)
        }
        val baseline = input.baselineMinutes.coerceAtLeast(0)
        return when (input.readiness) {
            ReadinessLevel.READY -> DailyMpTarget(baseline, DailyMpTargetType.TRAIN, "Train to plan", false)
            ReadinessLevel.LIMITING ->
                DailyMpTarget((baseline * MAINTAIN_FACTOR).roundToInt(), DailyMpTargetType.MAINTAIN, "Maintain — hold steady", false)
            ReadinessLevel.LOW ->
                DailyMpTarget((baseline * DELOAD_FACTOR).roundToInt(), DailyMpTargetType.DELOAD, "Deload — lighter than usual", false)
            ReadinessLevel.RECOVERY ->
                DailyMpTarget((baseline * RECOVERY_FACTOR).roundToInt(), DailyMpTargetType.RECOVERY, "Light recovery", false)
            ReadinessLevel.BLOCKED -> DailyMpTarget(0, DailyMpTargetType.REST, "Rest prescribed", isRestDay = true)
        }
    }
}
