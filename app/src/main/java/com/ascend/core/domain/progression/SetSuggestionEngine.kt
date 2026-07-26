package com.ascend.core.domain.progression

import kotlin.math.ceil

data class SetPlan(val sets: List<Int>) {
    val setCount: Int get() = sets.size
    val total: Int get() = sets.sum()
}

data class SetSuggestion(
    val primary: SetPlan,
    val alternatives: List<SetPlan>,
) {
    val suggestedSetCount: Int get() = primary.setCount
}

data class SetSuggestionInput(
    val remaining: Int,
    val preferredSetSize: Int,
    val minimumSetSize: Int = 1,
    val maximumSetSize: Int? = null,
    // When true the user explicitly opted into suggestions above the configured max.
    val allowUnrestricted: Boolean = false,
)

/**
 * Splits a remaining accumulation target into safe, uneven-friendly sets. Never
 * recommends a set larger than [SetSuggestionInput.maximumSetSize] unless
 * [SetSuggestionInput.allowUnrestricted] is set (spec requirement).
 */
class SetSuggestionEngine {

    fun suggest(input: SetSuggestionInput): SetSuggestion {
        val remaining = input.remaining
        if (remaining <= 0) return SetSuggestion(SetPlan(emptyList()), emptyList())

        val hardMax = when {
            input.allowUnrestricted -> Int.MAX_VALUE
            else -> input.maximumSetSize ?: Int.MAX_VALUE
        }
        val minSet = input.minimumSetSize.coerceIn(1, remaining)

        val primary = greedyPreferred(remaining, input.preferredSetSize, minSet, hardMax, input.maximumSetSize)
        val alternatives = buildList {
            // Fewer, larger sets (bounded by the configured max).
            val bigSize = input.maximumSetSize?.takeIf { !input.allowUnrestricted } ?: input.preferredSetSize
            add(evenlyDistributed(remaining, sizeCap = bigSize.coerceAtLeast(1), hardMax = hardMax))
            // Even split by the preferred size.
            add(evenlyDistributed(remaining, sizeCap = input.preferredSetSize.coerceAtLeast(1), hardMax = hardMax))
        }.filter { it.sets.isNotEmpty() && it != primary }.distinct()

        return SetSuggestion(primary, alternatives)
    }

    private fun greedyPreferred(
        remaining: Int,
        preferred: Int,
        minSet: Int,
        hardMax: Int,
        configuredMax: Int?,
    ): SetPlan {
        val size = preferred.coerceIn(1, hardMax)
        val sets = ArrayList<Int>()
        var left = remaining
        while (left >= size) {
            sets.add(size)
            left -= size
        }
        if (left > 0) {
            val foldTarget = if (sets.isEmpty()) null else sets.size - 1
            val canFold = foldTarget != null &&
                left < minSet &&
                (configuredMax == null || sets[foldTarget] + left <= hardMax)
            if (canFold) {
                sets[foldTarget!!] = sets[foldTarget] + left
            } else {
                sets.add(left)
            }
        }
        return SetPlan(sets)
    }

    private fun evenlyDistributed(remaining: Int, sizeCap: Int, hardMax: Int): SetPlan {
        val cap = minOf(sizeCap, hardMax).coerceAtLeast(1)
        val count = ceil(remaining.toDouble() / cap).toInt().coerceAtLeast(1)
        val base = remaining / count
        val extra = remaining % count
        val sets = (0 until count).map { index -> base + if (index < extra) 1 else 0 }
        return SetPlan(sets)
    }
}
