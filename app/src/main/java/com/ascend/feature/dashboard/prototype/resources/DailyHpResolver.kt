package com.ascend.feature.dashboard.prototype.resources

/*
 * Resolves HP (Vitality) from today's movement evidence against a goal-relative target. Never assumes
 * a universal step count; the target comes from the user's movement/steps goal. Distinguishes "zero
 * steps with a working source" (AVAILABLE, 0%) from "no movement source at all" (UNAVAILABLE).
 */

/** Movement input for a day: [steps] null (or [source] NONE) means no movement source is available. */
data class MovementInput(
    val steps: Int?,
    val source: MovementSource,
    val targetSteps: Int?,
)

object DailyHpResolver {
    fun resolve(input: MovementInput): DailyHpState {
        if (input.steps == null || input.source == MovementSource.NONE) {
            return DailyHpState(
                currentSteps = 0,
                targetSteps = input.targetSteps,
                progressFraction = null,
                availability = ResourceAvailability.UNAVAILABLE,
                source = MovementSource.NONE,
            )
        }
        val target = input.targetSteps
        val fraction =
            if (target != null && target > 0) {
                (input.steps.toFloat() / target).coerceAtLeast(0f)
            } else {
                null
            }
        return DailyHpState(
            currentSteps = input.steps,
            targetSteps = target,
            progressFraction = fraction,
            availability = ResourceAvailability.AVAILABLE,
            source = input.source,
        )
    }
}
