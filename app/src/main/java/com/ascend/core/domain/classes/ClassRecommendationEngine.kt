package com.ascend.core.domain.classes

import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassRecommendation
import com.ascend.core.model.ClassRecommendationInput
import com.ascend.core.model.ClassRecommendationOption
import javax.inject.Inject

/**
 * Maps plain‑language goals + preferred training style onto a ranked class
 * suggestion, scoring each definition by tag and keyword overlap — data‑driven, no
 * per‑class conditionals. It only **recommends**: the caller (onboarding) still makes
 * the user choose explicitly.
 */
class ClassRecommendationEngine
    @Inject
    constructor() {
        fun recommend(
            input: ClassRecommendationInput,
            definitions: List<ClassDefinition>,
        ): ClassRecommendation? {
            val ranked =
                definitions
                    .filter { it.enabled }
                    .map { score(input, it) }
                    .sortedByDescending { it.score }
            val top = ranked.firstOrNull() ?: return null
            return ClassRecommendation(recommended = top, alternatives = ranked.drop(1))
        }

        private fun score(
            input: ClassRecommendationInput,
            def: ClassDefinition,
        ): ClassRecommendationOption {
            val reasons = mutableListOf<String>()

            val matchedTags = input.goalTags.intersect(def.favoredTags)
            if (matchedTags.isNotEmpty()) {
                reasons += "Trains ${matchedTags.joinToString(", ") { it.lowercase().replace('_', ' ') }}"
            }

            val vocabulary =
                (
                    listOf(def.name, def.fitnessIdentity) + def.favoredWorkoutCategories
                ).flatMap { it.lowercase().split(Regex("[^a-z]+")) }.filter { it.length > 3 }.toSet()
            val matchedKeywords = input.goalKeywords.intersect(vocabulary)
            if (matchedKeywords.isNotEmpty()) {
                reasons += "Matches your goals: ${matchedKeywords.joinToString(", ")}"
            }

            val score = matchedTags.size * TAG_WEIGHT + matchedKeywords.size * KEYWORD_WEIGHT
            if (reasons.isEmpty()) reasons += "General fit"
            return ClassRecommendationOption(def.id, def.name, score, reasons)
        }

        private companion object {
            const val TAG_WEIGHT = 2.0
            const val KEYWORD_WEIGHT = 1.0
        }
    }
