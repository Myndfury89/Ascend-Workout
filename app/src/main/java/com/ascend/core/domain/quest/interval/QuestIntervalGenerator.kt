package com.ascend.core.domain.quest.interval

import com.ascend.core.model.IntervalDistributionStrategy
import javax.inject.Inject

/** Inputs for generating an interval schedule. */
data class IntervalGenerationRequest(
    val total: Int,
    val strategy: IntervalDistributionStrategy,
    val windowStart: Long,
    val windowEnd: Long,
    val intervalCount: Int? = null,
    val preferredSetSize: Int? = null,
    // Explicit (start, end) per interval for FIXED / CUSTOM times; overrides tiling.
    val customTimes: List<Pair<Long, Long>>? = null,
)

/** A generated interval blueprint (persisted later as a `QuestInterval`). */
data class GeneratedInterval(
    val orderIndex: Int,
    val scheduledStart: Long,
    val scheduledEnd: Long,
    val target: Int,
)

/**
 * Generates a non‑overlapping interval schedule that covers the active window and
 * whose targets sum to the daily total. Times come from explicit [customTimes] when
 * given, otherwise the window is tiled into equal, contiguous slots.
 */
class QuestIntervalGenerator
    @Inject
    constructor(
        private val distribution: QuestIntervalDistributionCalculator,
    ) {
        fun generate(request: IntervalGenerationRequest): List<GeneratedInterval> {
            if (request.total <= 0) return emptyList()

            val amounts =
                when {
                    request.customTimes != null ->
                        distribution.distribute(request.total, request.customTimes.size, request.strategy, request.preferredSetSize)
                    request.strategy == IntervalDistributionStrategy.PREFERRED_SET_SIZE ->
                        distribution.distribute(request.total, 0, request.strategy, request.preferredSetSize)
                    else ->
                        distribution.distribute(
                            request.total,
                            (request.intervalCount ?: DEFAULT_COUNT).coerceAtLeast(1),
                            request.strategy,
                            request.preferredSetSize,
                        )
                }
            if (amounts.isEmpty()) return emptyList()

            val times = request.customTimes ?: tile(request.windowStart, request.windowEnd, amounts.size)
            return amounts.mapIndexed { index, target ->
                val (start, end) = times[index]
                GeneratedInterval(index, start, end, target)
            }
        }

        private fun tile(
            windowStart: Long,
            windowEnd: Long,
            count: Int,
        ): List<Pair<Long, Long>> {
            val span = (windowEnd - windowStart).coerceAtLeast(0)
            val slot = if (count > 0) span / count else span
            return (0 until count).map { i ->
                val start = windowStart + i * slot
                val end = if (i == count - 1) windowEnd else windowStart + (i + 1) * slot
                start to end
            }
        }

        private companion object {
            const val DEFAULT_COUNT = 4
        }
    }
