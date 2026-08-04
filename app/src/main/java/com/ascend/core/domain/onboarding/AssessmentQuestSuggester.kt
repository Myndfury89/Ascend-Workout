package com.ascend.core.domain.onboarding

import com.ascend.core.model.onboarding.ActivityDomain
import com.ascend.core.model.onboarding.AssessmentQuestSuggestion
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.OnboardingStep
import javax.inject.Inject

/**
 * Suggests optional, comfortable Assessment Quests when onboarding information was skipped or too
 * uncertain to plan confidently. Suggestions only *establish* information after real activity is
 * completed — they never require maximum effort or failure, never create a PR/verified history from
 * onboarding answers, and never award XP for merely answering questions.
 */
class AssessmentQuestSuggester
    @Inject
    constructor() {
        fun suggest(
            assessment: InitialAssessment,
            skippedSteps: Set<OnboardingStep> = emptySet(),
        ): List<AssessmentQuestSuggestion> {
            val suggestions = mutableListOf<AssessmentQuestSuggestion>()
            val snapshot = assessment.abilitySnapshot
            val abilitySkipped = OnboardingStep.ABILITY_SNAPSHOT in skippedSteps || snapshot == null || snapshot.isEmpty

            if (abilitySkipped || snapshot?.recentStrengthTraining == null) {
                suggestions +=
                    AssessmentQuestSuggestion(
                        id = "assess-strength",
                        title = "Record a normal strength workout",
                        description = "Log a comfortable session at your usual effort — no maxing out.",
                        establishes = "A verified strength baseline to tune your plan.",
                        relatedDomain = ActivityDomain.STRENGTH_TRAINING,
                    )
                suggestions +=
                    AssessmentQuestSuggestion(
                        id = "assess-bodyweight",
                        title = "Complete a comfortable push-up set",
                        description = "Do a set that leaves a couple of reps in reserve — stop before failure.",
                        establishes = "A verified bodyweight baseline.",
                        relatedDomain = ActivityDomain.CALISTHENICS,
                    )
            }
            if (abilitySkipped || snapshot?.longestRecentCardioMinutes == null) {
                suggestions +=
                    AssessmentQuestSuggestion(
                        id = "assess-cardio",
                        title = "Take a comfortable 15-minute walk",
                        description = "An easy, conversational pace — this is not a test.",
                        establishes = "A verified cardio baseline.",
                        relatedDomain = ActivityDomain.CARDIO,
                    )
            }
            if (abilitySkipped || snapshot?.averageDailySteps == null) {
                suggestions +=
                    AssessmentQuestSuggestion(
                        id = "assess-steps",
                        title = "Connect step history",
                        description = "Optionally connect Health Connect so step history can personalize your plan.",
                        establishes = "Recent daily step activity (once connected).",
                        relatedDomain = ActivityDomain.CARDIO,
                    )
            }
            return suggestions
        }
    }
