package com.ascend.core.model.onboarding

/**
 * The provisional, self-reported assessment captured during onboarding. Every value is an estimate
 * ([Provenance.SELF_REPORTED]) that establishes *starting recommendations only*. Verified workout
 * evidence supersedes it in a later reconciliation phase — [replacedByEvidenceAt] is intentionally
 * persisted as null in this phase and no replacement/confidence logic is invented here.
 *
 * Private fields (body metrics, physiology, limitations, age category, ability snapshot) must never
 * be exposed to party/presence/social/discovery models or Bluetooth payloads.
 */
data class InitialAssessment(
    val userId: String,
    val primaryGoal: PrimaryGoal? = null,
    val secondaryGoals: List<PrimaryGoal> = emptyList(),
    val trainingFrequency: TrainingFrequency? = null,
    val activityExperience: Map<ActivityDomain, ExperienceLevel> = emptyMap(),
    val activityPreferences: Set<ActivityPreference> = emptySet(),
    val abilitySnapshot: AbilitySnapshot? = null,
    val equipment: Set<Equipment> = emptySet(),
    val environment: TrainingEnvironment? = null,
    val availability: Availability? = null,
    val physiology: PhysiologyProfile = PhysiologyProfile(),
    val bodyMetrics: BodyMetrics = BodyMetrics(),
    val limitations: Set<Limitation> = emptySet(),
    val ageSafetyCategory: AgeSafetyCategory = AgeSafetyCategory.NOT_PROVIDED,
    val provenance: Provenance = Provenance.SELF_REPORTED,
    val selfReportedAt: Long = 0L,
    // Deferred evidence-reconciliation phase sets this; always null in the onboarding phase.
    val replacedByEvidenceAt: Long? = null,
) {
    /** Effective limitations, treating an empty/none/prefer-not set as "none reported". */
    val effectiveLimitations: Set<Limitation>
        get() = limitations.filter { it != Limitation.NONE_REPORTED && it != Limitation.PREFER_NOT_TO_ANSWER }.toSet()
}
