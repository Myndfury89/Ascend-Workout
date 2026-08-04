package com.ascend.core.domain.onboarding

import com.ascend.core.domain.classes.ClassRecommendationEngine
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassRecommendationInput
import com.ascend.core.model.onboarding.ClassAffinityResult
import com.ascend.core.model.onboarding.ExperienceLevel
import com.ascend.core.model.onboarding.InitialAssessment
import javax.inject.Inject

/**
 * Builds an explainable class-affinity recommendation from the onboarding assessment by reusing the
 * existing [ClassRecommendationEngine] (no duplicate class model or scoring). It considers ONLY what
 * the user wants to train and enjoys — stated goals, preferred activities, and experienced domains.
 *
 * Protected and physiological traits (sex, gender, body weight, body-fat, disability, limitations,
 * age, height, race/ethnicity, pregnancy) are deliberately never read, so they cannot influence the
 * result. The recommendation is advisory: the user may still choose any available class.
 */
class ClassAffinityAssessor
    @Inject
    constructor(
        private val engine: ClassRecommendationEngine,
    ) {
        fun assess(
            assessment: InitialAssessment,
            definitions: List<ClassDefinition>,
        ): ClassAffinityResult {
            val tags = mutableSetOf<String>()
            val keywords = mutableSetOf<String>()
            val evidence = mutableListOf<String>()

            assessment.primaryGoal?.let { goal ->
                tags += GoalAffinityMapping.goalTags(goal)
                keywords += GoalAffinityMapping.goalKeywords(goal)
                evidence += "goal:${goal.name}"
            }
            assessment.secondaryGoals.forEach { goal ->
                tags += GoalAffinityMapping.goalTags(goal)
                evidence += "secondaryGoal:${goal.name}"
            }
            assessment.activityPreferences.forEach { pref ->
                val prefTags = GoalAffinityMapping.preferenceTags(pref)
                if (prefTags.isNotEmpty()) {
                    tags += prefTags
                    evidence += "prefers:${pref.name}"
                }
            }
            assessment.activityExperience
                .filterValues { it == ExperienceLevel.EXPERIENCED }
                .keys
                .forEach { domain ->
                    tags += GoalAffinityMapping.domainTags(domain)
                    evidence += "experienced:${domain.name}"
                }

            val input = ClassRecommendationInput(goalTags = tags, goalKeywords = keywords)
            val recommendation = engine.recommend(input, definitions)
                ?: return ClassAffinityResult(
                    recommendedClassId = null,
                    classScores = emptyMap(),
                    rationale = "No class recommendation is available yet — you can choose any class.",
                    evidenceKeys = evidence,
                )

            val scores =
                (listOf(recommendation.recommended) + recommendation.alternatives)
                    .associate { it.classId to it.score }
            return ClassAffinityResult(
                recommendedClassId = recommendation.recommended.classId,
                classScores = scores,
                rationale = recommendation.recommended.reasons.joinToString("; "),
                evidenceKeys = evidence,
            )
        }
    }
