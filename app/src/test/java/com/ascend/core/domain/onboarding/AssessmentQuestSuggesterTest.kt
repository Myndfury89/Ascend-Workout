package com.ascend.core.domain.onboarding

import com.ascend.core.model.onboarding.AbilitySnapshot
import com.ascend.core.model.onboarding.ActivityDomain
import com.ascend.core.model.onboarding.AssessmentQuestSuggestion
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.OnboardingStep
import com.ascend.core.model.onboarding.PullUpCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentQuestSuggesterTest {
    private val disabled = AssessmentQuestSuggester(onboardingSuggestionEnabled = false)
    private val enabled = AssessmentQuestSuggester(onboardingSuggestionEnabled = true)

    @Test
    fun `skipping the ability snapshot suggests optional assessment quests`() {
        val assessment = InitialAssessment(userId = "u1", abilitySnapshot = null)
        val skipped = setOf(OnboardingStep.ABILITY_SNAPSHOT)
        val suggestions = disabled.suggest(assessment, skipped)
        assertTrue("suggestions offered", suggestions.isNotEmpty())
        assertHealthConnectSuggestionAbsent(suggestions)
        // The no-arg constructor reads the compile-time flag, which is off for this variant.
        assertHealthConnectSuggestionAbsent(AssessmentQuestSuggester().suggest(assessment, skipped))
    }

    @Test
    fun `no assessment quest requires maximum effort`() {
        val suggestions = disabled.suggest(InitialAssessment(userId = "u1", abilitySnapshot = null))
        assertTrue(suggestions.isNotEmpty())
        assertTrue("none require maximum effort", suggestions.none { it.requiresMaximumEffort })
        assertTrue("each explains what it establishes", suggestions.all { it.establishes.isNotBlank() })
        assertHealthConnectSuggestionAbsent(suggestions)
    }

    @Test
    fun `a complete snapshot needs fewer or no suggestions`() {
        val suggestions = disabled.suggest(InitialAssessment(userId = "u1", abilitySnapshot = fullSnapshot()))
        assertTrue("a full snapshot needs no assessment quests", suggestions.isEmpty())
    }

    @Test
    fun `disabled flag keeps strength bodyweight and cardio when the snapshot is skipped`() {
        val suggestions =
            disabled.suggest(
                InitialAssessment(userId = "u1", abilitySnapshot = null),
                skippedSteps = setOf(OnboardingStep.ABILITY_SNAPSHOT),
            )
        assertTrue(suggestions.any { it.id == "assess-strength" })
        assertTrue(suggestions.any { it.id == "assess-bodyweight" })
        assertTrue(suggestions.any { it.id == "assess-cardio" })
        assertHealthConnectSuggestionAbsent(suggestions)
    }

    @Test
    fun `disabled flag hides assess-steps when average daily steps are missing`() {
        val suggestions = disabled.suggest(InitialAssessment(userId = "u1", abilitySnapshot = snapshotMissingSteps()))
        assertHealthConnectSuggestionAbsent(suggestions)
        assertTrue(suggestions.none { it.id == "assess-strength" })
        assertTrue(suggestions.none { it.id == "assess-bodyweight" })
        assertTrue(suggestions.none { it.id == "assess-cardio" })
    }

    @Test
    fun `enabled flag restores assess-steps copy when the ability snapshot is skipped`() {
        val suggestions =
            enabled.suggest(
                InitialAssessment(userId = "u1", abilitySnapshot = null),
                skippedSteps = setOf(OnboardingStep.ABILITY_SNAPSHOT),
            )
        assertAssessStepsRestored(suggestions)
        assertTrue(suggestions.any { it.id == "assess-strength" })
        assertTrue(suggestions.any { it.id == "assess-bodyweight" })
        assertTrue(suggestions.any { it.id == "assess-cardio" })
    }

    @Test
    fun `enabled flag restores assess-steps copy when average daily steps are missing`() {
        val suggestions = enabled.suggest(InitialAssessment(userId = "u1", abilitySnapshot = snapshotMissingSteps()))
        assertAssessStepsRestored(suggestions)
        assertEquals(listOf("assess-steps"), suggestions.map { it.id })
    }

    @Test
    fun `complete snapshot suggests no assess-steps even when the flag is enabled`() {
        val suggestions = enabled.suggest(InitialAssessment(userId = "u1", abilitySnapshot = fullSnapshot()))
        assertTrue(suggestions.isEmpty())
        assertHealthConnectSuggestionAbsent(suggestions)
    }

    private fun fullSnapshot() =
        AbilitySnapshot(
            comfortablePushUps = 20,
            comfortableSquats = 30,
            pullUpCapability = PullUpCapability.FEW,
            longestRecentCardioMinutes = 30,
            averageDailySteps = 8000,
            recentStrengthTraining = true,
        )

    /** Strength and cardio filled, steps left empty — the live onboarding form path. */
    private fun snapshotMissingSteps() =
        AbilitySnapshot(
            comfortablePushUps = 12,
            comfortableSquats = 20,
            pullUpCapability = PullUpCapability.FEW,
            longestRecentCardioMinutes = 25,
            averageDailySteps = null,
            recentStrengthTraining = true,
        )

    private fun assertHealthConnectSuggestionAbsent(suggestions: List<AssessmentQuestSuggestion>) {
        assertTrue(suggestions.none { it.id == "assess-steps" })
        assertTrue(suggestions.none { it.title == "Connect step history" })
        assertTrue(
            suggestions.none { suggestion ->
                listOf(suggestion.id, suggestion.title, suggestion.description, suggestion.establishes)
                    .any { field -> field.contains("Health Connect", ignoreCase = true) }
            },
        )
    }

    private fun assertAssessStepsRestored(suggestions: List<AssessmentQuestSuggestion>) {
        val steps = suggestions.filter { it.id == "assess-steps" }
        assertEquals(1, steps.size)
        val item = steps.single()
        assertEquals("Connect step history", item.title)
        assertEquals(
            "Optionally connect Health Connect so step history can personalize your plan.",
            item.description,
        )
        assertEquals("Recent daily step activity (once connected).", item.establishes)
        assertEquals(ActivityDomain.CARDIO, item.relatedDomain)
    }
}
