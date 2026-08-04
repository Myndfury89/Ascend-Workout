package com.ascend.core.domain.onboarding

import com.ascend.core.model.onboarding.AbilitySnapshot
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.OnboardingStep
import com.ascend.core.model.onboarding.PullUpCapability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentQuestSuggesterTest {
    private val suggester = AssessmentQuestSuggester()

    @Test
    fun `skipping the ability snapshot suggests optional assessment quests`() {
        val suggestions =
            suggester.suggest(
                InitialAssessment(userId = "u1", abilitySnapshot = null),
                skippedSteps = setOf(OnboardingStep.ABILITY_SNAPSHOT),
            )
        assertTrue("suggestions offered", suggestions.isNotEmpty())
    }

    @Test
    fun `no assessment quest requires maximum effort`() {
        val suggestions = suggester.suggest(InitialAssessment(userId = "u1", abilitySnapshot = null))
        assertTrue(suggestions.isNotEmpty())
        assertTrue("none require maximum effort", suggestions.none { it.requiresMaximumEffort })
        assertTrue("each explains what it establishes", suggestions.all { it.establishes.isNotBlank() })
    }

    @Test
    fun `a complete snapshot needs fewer or no suggestions`() {
        val full =
            AbilitySnapshot(
                comfortablePushUps = 20,
                comfortableSquats = 30,
                pullUpCapability = PullUpCapability.FEW,
                longestRecentCardioMinutes = 30,
                averageDailySteps = 8000,
                recentStrengthTraining = true,
            )
        val suggestions = suggester.suggest(InitialAssessment(userId = "u1", abilitySnapshot = full))
        assertTrue("a full snapshot needs no assessment quests", suggestions.isEmpty())
    }
}
