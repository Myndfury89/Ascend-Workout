package com.ascend.core.model.onboarding

/*
 * Pure domain models for the Ascend onboarding + initial-assessment ("Ascension assessment")
 * system. Nothing here awards XP/Skills/PRs/rewards or touches persistence — these are the typed
 * values the flow collects and the provisional recommendations it derives. Verified workout
 * evidence replaces self-reported estimates in a later reconciliation phase (see [Provenance]).
 */

/** The current onboarding schema version, persisted so a future flow change can migrate answers. */
const val ONBOARDING_VERSION: Int = 1

/**
 * The ordered onboarding screens. Kept coarse (≈10–12 steps) so the flow stays concise; some steps
 * group two related sections (training background = frequency + experience; physiology+limitations).
 */
enum class OnboardingStep {
    WELCOME,
    BASIC_PROFILE,
    GOALS,
    TRAINING_BACKGROUND,
    ACTIVITY_PREFERENCES,
    EQUIPMENT_ENVIRONMENT,
    AVAILABILITY,
    ABILITY_SNAPSHOT,
    PHYSIOLOGY_LIMITATIONS,
    CLASS_AFFINITY,
    PLAN_REVIEW,
    COMPLETION,
}

/** Whether a field must be answered to finish onboarding, is encouraged, or is fully optional. */
enum class FieldRequirement { REQUIRED, RECOMMENDED, OPTIONAL }

/**
 * Where a value came from. Onboarding only ever produces [SELF_REPORTED]. Verified activity, device
 * sync, or a coach can later supersede it — the reconciliation that sets `replacedByEvidenceAt` is
 * a deferred phase; this phase never marks self-reported data as verified.
 */
enum class Provenance { SELF_REPORTED, DEVICE_DERIVED, WORKOUT_VERIFIED, COACH_ENTERED }

/** A value tagged with its origin + when it was recorded, so provisional data is never mistaken for evidence. */
data class ProvenancedValue<T>(
    val value: T,
    val provenance: Provenance,
    val recordedAt: Long,
)

/** High-level onboarding lifecycle state. */
enum class OnboardingStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, AGE_INELIGIBLE }

/**
 * Resumable onboarding progress. Persisted so an interrupted session (restart, recreation, process
 * death, navigating away) resumes at [currentStep] rather than restarting. [skippedSteps] records
 * optional sections the user chose to skip (which may drive Assessment-Quest suggestions later).
 */
data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val completedSteps: Set<OnboardingStep> = emptySet(),
    val skippedSteps: Set<OnboardingStep> = emptySet(),
    val startedAt: Long = 0L,
    val completedAt: Long? = null,
    val version: Int = ONBOARDING_VERSION,
) {
    val status: OnboardingStatus
        get() =
            when {
                completedAt != null -> OnboardingStatus.COMPLETED
                completedSteps.isEmpty() && currentStep == OnboardingStep.WELCOME -> OnboardingStatus.NOT_STARTED
                else -> OnboardingStatus.IN_PROGRESS
            }
}
