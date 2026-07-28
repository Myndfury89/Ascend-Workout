package com.ascend.core.domain.quest.interval

import com.ascend.core.model.IntervalDistributionStrategy
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Splits a daily target across intervals per a [IntervalDistributionStrategy]. Every
 * strategy returns amounts that sum **exactly** to the target, so the daily objective
 * is always fully assigned. Uneven splits are first‑class.
 */
class QuestIntervalDistributionCalculator
    @Inject
    constructor() {
        fun distribute(
            total: Int,
            count: Int,
            strategy: IntervalDistributionStrategy,
            preferredSetSize: Int? = null,
        ): List<Int> {
            if (total <= 0) return emptyList()
            return when (strategy) {
                IntervalDistributionStrategy.EQUAL, IntervalDistributionStrategy.CUSTOM -> even(total, count)
                IntervalDistributionStrategy.PREFERRED_SET_SIZE -> bySize(total, (preferredSetSize ?: total).coerceAtLeast(1))
                IntervalDistributionStrategy.FRONT_LOADED -> weighted(total, count, frontLoaded = true)
                IntervalDistributionStrategy.BACK_LOADED -> weighted(total, count, frontLoaded = false)
            }
        }

        private fun even(
            total: Int,
            count: Int,
        ): List<Int> {
            if (count <= 0) return emptyList()
            val base = total / count
            val remainder = total % count
            return (0 until count).map { base + if (it < remainder) 1 else 0 }
        }

        private fun bySize(
            total: Int,
            size: Int,
        ): List<Int> {
            val count = ceil(total.toDouble() / size).toInt().coerceAtLeast(1)
            val out = ArrayList<Int>(count)
            var left = total
            repeat(count) {
                val amount = minOf(size, left)
                out += amount
                left -= amount
            }
            return out
        }

        private fun weighted(
            total: Int,
            count: Int,
            frontLoaded: Boolean,
        ): List<Int> {
            if (count <= 0) return emptyList()
            if (count == 1) return listOf(total)
            val weights = (0 until count).map { if (frontLoaded) count - it else it + 1 }
            val weightSum = weights.sum()
            val amounts = weights.map { (total.toLong() * it / weightSum).toInt() }.toMutableList()
            // Assign the rounding remainder to the heaviest slot so the sum is exact.
            val assigned = amounts.sum()
            val heaviest = if (frontLoaded) 0 else count - 1
            amounts[heaviest] += total - assigned
            return amounts
        }
    }
