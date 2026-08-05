package com.ascend.core.domain.onboarding

import com.ascend.core.domain.classes.ClassAffinityVocabulary
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.onboarding.ExperienceLevel
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.TrainingEnvironment

/**
 * Builds the user-facing "why this class" rationale for a recommendation, phrased from the player's
 * *actual answers* — goals, enjoyed activities, training environment, and experienced domains. It
 * deliberately reads none of the protected or physiological traits (sex, weight, age, limitations,
 * height), so those can never appear in an explanation. Clauses are chosen for relevance to the
 * recommended class's favored tags, so the sentence explains *this* recommendation specifically.
 */
object ClassRecommendationRationale {
    private const val MAX_CLAUSES = 3

    fun build(
        assessment: InitialAssessment,
        recommended: ClassDefinition,
    ): String {
        val favored = recommended.favoredTags
        val clauses = mutableListOf<String>()

        // Goals whose tags overlap the recommended class.
        val goals =
            (listOfNotNull(assessment.primaryGoal) + assessment.secondaryGoals)
                .filter { GoalAffinityMapping.goalTags(it).any { tag -> tag in favored } }
                .map { it.displayName.lowercase() }
                .distinct()
        if (goals.isNotEmpty()) clauses += "prioritized ${ClassAffinityVocabulary.joinReadable(goals)}"

        // Enjoyed activities whose tags overlap the recommended class.
        val prefs =
            assessment.activityPreferences
                .filter { GoalAffinityMapping.preferenceTags(it).any { tag -> tag in favored } }
                .map { deslug(it.name) }
                .distinct()
        if (prefs.isNotEmpty()) clauses += "prefer ${ClassAffinityVocabulary.joinReadable(prefs)}"

        // Training environment, when provided (never itself a class gate).
        assessment.environment?.let { env -> environmentClause(env)?.let { clauses += it } }

        // Experienced domains whose tags overlap the recommended class.
        val domains =
            assessment.activityExperience
                .filterValues { it == ExperienceLevel.EXPERIENCED }
                .keys
                .filter { GoalAffinityMapping.domainTags(it).any { tag -> tag in favored } }
                .map { deslug(it.name) }
                .distinct()
        if (domains.isNotEmpty()) clauses += "have experience with ${ClassAffinityVocabulary.joinReadable(domains)}"

        if (clauses.isEmpty()) {
            return "Recommended as a balanced starting path — you can change it at any time."
        }
        return "Recommended because you ${ClassAffinityVocabulary.joinReadable(clauses.take(MAX_CLAUSES))}."
    }

    private fun environmentClause(env: TrainingEnvironment): String? =
        when (env) {
            TrainingEnvironment.GYM -> "chose gym-based training"
            TrainingEnvironment.HOME -> "chose home training"
            TrainingEnvironment.OUTDOORS -> "chose outdoor training"
            TrainingEnvironment.MIXED -> null
        }

    private fun deslug(name: String): String = name.lowercase().replace('_', ' ')
}
