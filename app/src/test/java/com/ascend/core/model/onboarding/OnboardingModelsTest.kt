package com.ascend.core.model.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Provenance + provisional-evidence invariants and resumable onboarding state. */
class OnboardingModelsTest {
    @Test
    fun `an assessment is self-reported and not yet replaced by evidence`() {
        val assessment = InitialAssessment(userId = "u1", primaryGoal = PrimaryGoal.STRENGTH)
        assertEquals("provisional onboarding data is self-reported", Provenance.SELF_REPORTED, assessment.provenance)
        assertNull("replacedByEvidenceAt is null in the onboarding phase", assessment.replacedByEvidenceAt)
    }

    @Test
    fun `self-reported provenance is distinct from workout-verified`() {
        assertTrue(Provenance.SELF_REPORTED != Provenance.WORKOUT_VERIFIED)
    }

    @Test
    fun `onboarding state reports its lifecycle status`() {
        assertEquals(OnboardingStatus.NOT_STARTED, OnboardingState().status)
        val inProgress = OnboardingState(currentStep = OnboardingStep.GOALS, completedSteps = setOf(OnboardingStep.WELCOME))
        assertEquals(OnboardingStatus.IN_PROGRESS, inProgress.status)
        val done = inProgress.copy(completedAt = 100L)
        assertEquals(OnboardingStatus.COMPLETED, done.status)
        assertEquals(ONBOARDING_VERSION, done.version)
    }
}
