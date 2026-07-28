package com.ascend.core.domain.quest.interval

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
