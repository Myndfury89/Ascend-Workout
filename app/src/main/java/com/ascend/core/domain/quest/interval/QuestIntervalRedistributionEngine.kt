package com.ascend.core.domain.quest.interval

import com.ascend.core.model.AdaptiveIntervalAction
import com.ascend.core.model.AdaptiveIntervalContext
import com.ascend.core.model.AdaptiveIntervalRecommendation
import com.ascend.core.model.FutureInterval
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.RedistributionPreference
import javax.inject.Inject

/** New future‑interval targets after redistributing leftover work, plus anything left over. */
data class RedistributionResult(
    val newTargets: List<Int>,
    val unassigned: Int,
)

/**
 * Rebalances the [remaining] amount from a missed/partial interval across the
 * [futureTargets], honoring the user's [RedistributionPreference] and the maximum
 * allowed set size (overflow that can't be placed is returned as [unassigned], never
 * silently dropped). Future intervals are never increased without this being invoked.
 */
class QuestIntervalRedistributionEngine
    @Inject
    constructor() {
        fun redistribute(
            remaining: Int,
            futureTargets: List<Int>,
            preference: RedistributionPreference,
            maxSetSize: Int? = null,
        ): RedistributionResult {
            if (remaining <= 0 || futureTargets.isEmpty()) {
                return RedistributionResult(futureTargets, remaining.coerceAtLeast(0))
            }
            // The user must be asked; preserve the plan and keep the remainder flexible.
            if (preference == RedistributionPreference.ASK_EVERY_TIME ||
                preference == RedistributionPreference.PRESERVE_ORIGINAL
            ) {
                return RedistributionResult(futureTargets, remaining)
            }

            val additions =
                when (preference) {
                    RedistributionPreference.HEAVIER_FINAL -> onlyLast(remaining, futureTargets.size)
                    RedistributionPreference.LIGHTER_NEXT -> backLoaded(remaining, futureTargets.size)
                    else -> even(remaining, futureTargets.size)
                }

            val result = futureTargets.toMutableList()
            var leftover = 0
            additions.forEachIndexed { i, add ->
                val proposed = result[i] + add
                if (maxSetSize != null && proposed > maxSetSize) {
                    result[i] = maxSetSize
                    leftover += proposed - maxSetSize
                } else {
                    result[i] = proposed
                }
            }
            return RedistributionResult(result, leftover)
        }

        /**
         * The adaptive decision for a partially-completed day. Honours the daily total as
         * authoritative, every hard constraint (max interval target / set size, quiet hours,
         * fatigue, safety), and the opt-in nature of automatic adaptation. It **never**
         * increases the daily total or auto-applies under any blocked constraint.
         */
        fun adaptiveRecommend(context: AdaptiveIntervalContext): AdaptiveIntervalRecommendation {
            // The daily total is authoritative — a met target makes interval timing irrelevant.
            if (context.dailyTargetMet) {
                return plan(
                    AdaptiveIntervalAction.MAINTAIN_PLAN,
                    "Daily target already met — interval timing doesn't matter",
                    listOf("Completed ${context.currentDailyProgress}/${context.dailyTarget}"),
                    requiresConfirmation = false,
                )
            }
            if (context.safetyState == ProgressionSafetyState.BLOCKED ||
                context.safetyState == ProgressionSafetyState.STOP_AND_SEEK_GUIDANCE
            ) {
                return plan(
                    AdaptiveIntervalAction.REQUEST_USER_CHOICE,
                    "A safety flag is set — decide how to adjust today",
                    listOf("Not auto-adapting while a safety flag is present"),
                    safety = context.safetyState,
                )
            }
            // Over-scheduled: future intervals total more than what's left → offer to shrink them.
            if (context.leftover <= 0 && context.futureIntervals.sumOf { it.currentTarget } > context.remainingDailyTarget) {
                return reduceIntervalSizes(context)
            }
            if (context.leftover <= 0) {
                return plan(AdaptiveIntervalAction.MAINTAIN_PLAN, "Nothing to redistribute", emptyList(), requiresConfirmation = false)
            }
            // Preference to be asked / to preserve the plan → leave the remainder flexible.
            if (context.redistributionPreference == RedistributionPreference.ASK_EVERY_TIME) {
                return plan(
                    AdaptiveIntervalAction.REQUEST_USER_CHOICE,
                    "Choose how to handle the ${context.leftover} left over",
                    listOf("Your preference is to be asked each time"),
                    leftoverFlexible = context.leftover,
                )
            }
            if (context.redistributionPreference == RedistributionPreference.PRESERVE_ORIGINAL) {
                return preserveFlexible(context, "You prefer to keep future intervals unchanged")
            }
            if (context.highFatigue) {
                return preserveFlexible(context, "Fatigue is high — not adding to future intervals")
            }

            val eligible = context.futureIntervals.filter { !inQuietHours(it.scheduledStart, context.quietHours) }
            if (eligible.isEmpty()) {
                // No interval can take the work: with none scheduled, add one; if the only
                // future intervals fall in quiet hours, go flexible rather than disturb them.
                return if (context.futureIntervals.isEmpty()) {
                    noEligibleIntervalPlan(context)
                } else {
                    AdaptiveIntervalRecommendation(
                        action = AdaptiveIntervalAction.CONVERT_TO_FLEXIBLE,
                        summary = "Upcoming intervals fall in quiet hours — finish the ${context.leftover} flexibly",
                        reasons = listOf("Respecting quiet hours"),
                        leftoverFlexible = context.leftover,
                        requiresConfirmation = true,
                        constraintNotes = listOf("future intervals fall in quiet hours"),
                    )
                }
            }
            return distributeAcross(context, eligible)
        }

        private fun distributeAcross(
            context: AdaptiveIntervalContext,
            eligible: List<FutureInterval>,
        ): AdaptiveIntervalRecommendation {
            val additions =
                when (context.redistributionPreference) {
                    RedistributionPreference.HEAVIER_FINAL -> onlyLast(context.leftover, eligible.size)
                    RedistributionPreference.LIGHTER_NEXT -> backLoaded(context.leftover, eligible.size)
                    else -> even(context.leftover, eligible.size)
                }
            val action =
                when (context.redistributionPreference) {
                    RedistributionPreference.HEAVIER_FINAL -> AdaptiveIntervalAction.HEAVIER_FINAL_INTERVAL
                    RedistributionPreference.LIGHTER_NEXT -> AdaptiveIntervalAction.LIGHTER_NEXT_INTERVAL
                    else -> AdaptiveIntervalAction.REDISTRIBUTE_EVENLY
                }

            val proposed = mutableMapOf<String, Int>()
            val notes = mutableListOf<String>()
            var leftover = 0
            eligible.forEachIndexed { i, interval ->
                val want = (interval.currentTarget + additions[i]).toLong()
                // Never exceed the max interval target, nor grow one interval by more than a set.
                // Long math avoids overflow when a cap is left at Int.MAX_VALUE (no limit).
                val capByInterval = context.maximumIntervalTarget.toLong()
                val capBySet = interval.currentTarget.toLong() + context.maximumSetSize
                val capped = minOf(want, capByInterval, capBySet).toInt()
                if (capped < want) {
                    leftover += (want - capped).toInt()
                    if (want > capByInterval) notes += "capped at the max interval target"
                    if (want > capBySet) notes += "capped at the max set size"
                }
                if (capped != interval.currentTarget) proposed[interval.intervalId] = capped
            }
            if (context.futureIntervals.size > eligible.size) notes += "skipped intervals inside quiet hours"

            val blocked = leftover > 0 || notes.isNotEmpty()
            val auto = context.autoSafeAdaptationEnabled && !blocked
            return AdaptiveIntervalRecommendation(
                action = action,
                summary = "Redistribute ${context.leftover - leftover} across ${proposed.size} upcoming interval(s)",
                reasons = listOf("Rebalancing missed work while keeping the daily total the same"),
                proposedTargets = proposed,
                leftoverFlexible = leftover,
                requiresConfirmation = !auto,
                autoApplied = auto,
                constraintNotes = notes.distinct(),
            )
        }

        private fun reduceIntervalSizes(context: AdaptiveIntervalContext): AdaptiveIntervalRecommendation {
            val futures = context.futureIntervals
            val target = context.remainingDailyTarget
            val currentSum = futures.sumOf { it.currentTarget }.coerceAtLeast(1)
            val proposed =
                futures.associate { it.intervalId to (it.currentTarget.toLong() * target / currentSum).toInt().coerceAtLeast(0) }
            return AdaptiveIntervalRecommendation(
                action = AdaptiveIntervalAction.REDUCE_INTERVAL_SIZES,
                summary = "Shrink upcoming intervals to match the ${context.remainingDailyTarget} still needed",
                reasons = listOf("You're ahead of plan — future intervals can be lighter"),
                proposedTargets = proposed,
                requiresConfirmation = true,
            )
        }

        private fun noEligibleIntervalPlan(context: AdaptiveIntervalContext): AdaptiveIntervalRecommendation {
            if (context.timeRemainingMillis <= 0) {
                return AdaptiveIntervalRecommendation(
                    action = AdaptiveIntervalAction.REDUCE_DAILY_TOTAL,
                    summary = "Out of time — accept ${context.currentDailyProgress} for today",
                    reasons = listOf("No time or eligible intervals remain"),
                    proposedDailyTotal = context.currentDailyProgress,
                    requiresConfirmation = true,
                )
            }
            val newTarget = minOf(context.leftover, context.maximumSetSize, context.maximumIntervalTarget)
            return AdaptiveIntervalRecommendation(
                action = AdaptiveIntervalAction.ADD_NEW_INTERVAL,
                summary = "Add a new interval for $newTarget",
                reasons = listOf("No existing interval is available outside quiet hours"),
                newIntervalTarget = newTarget,
                leftoverFlexible = (context.leftover - newTarget).coerceAtLeast(0),
                requiresConfirmation = true,
            )
        }

        private fun preserveFlexible(
            context: AdaptiveIntervalContext,
            reason: String,
        ) = AdaptiveIntervalRecommendation(
            action = AdaptiveIntervalAction.PRESERVE_FUTURE_LEAVE_FLEXIBLE,
            summary = "Keep future intervals as planned and leave ${context.leftover} flexible",
            reasons = listOf(reason),
            leftoverFlexible = context.leftover,
            requiresConfirmation = true,
        )

        private fun plan(
            action: AdaptiveIntervalAction,
            summary: String,
            reasons: List<String>,
            leftoverFlexible: Int = 0,
            requiresConfirmation: Boolean = true,
            safety: ProgressionSafetyState = ProgressionSafetyState.OK,
        ) = AdaptiveIntervalRecommendation(
            action = action,
            summary = summary,
            reasons = reasons,
            leftoverFlexible = leftoverFlexible,
            requiresConfirmation = requiresConfirmation,
            safetyState = safety,
        )

        private fun inQuietHours(
            time: Long,
            quietHours: List<Pair<Long, Long>>,
        ): Boolean = quietHours.any { (start, end) -> time in start until end }

        private fun even(
            total: Int,
            count: Int,
        ): List<Int> {
            val base = total / count
            val rem = total % count
            return (0 until count).map { base + if (it < rem) 1 else 0 }
        }

        private fun onlyLast(
            total: Int,
            count: Int,
        ): List<Int> = (0 until count).map { if (it == count - 1) total else 0 }

        private fun backLoaded(
            total: Int,
            count: Int,
        ): List<Int> {
            if (count == 1) return listOf(total)
            val weights = (0 until count).map { it + 1 }
            val sum = weights.sum()
            val out = weights.map { (total.toLong() * it / sum).toInt() }.toMutableList()
            out[count - 1] += total - out.sum()
            return out
        }
    }
