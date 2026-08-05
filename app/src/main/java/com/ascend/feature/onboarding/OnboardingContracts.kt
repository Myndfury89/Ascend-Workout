package com.ascend.feature.onboarding

import com.ascend.core.common.WeightUnit
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.onboarding.AbilitySnapshot
import com.ascend.core.model.onboarding.ActivityDomain
import com.ascend.core.model.onboarding.ActivityPreference
import com.ascend.core.model.onboarding.AgeRange
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.Availability
import com.ascend.core.model.onboarding.BodyMetrics
import com.ascend.core.model.onboarding.ClassAffinityResult
import com.ascend.core.model.onboarding.Equipment
import com.ascend.core.model.onboarding.ExperienceLevel
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.InitialQuestPlan
import com.ascend.core.model.onboarding.Limitation
import com.ascend.core.model.onboarding.OnboardingStep
import com.ascend.core.model.onboarding.OptionalSex
import com.ascend.core.model.onboarding.PhysiologyProfile
import com.ascend.core.model.onboarding.PrimaryGoal
import com.ascend.core.model.onboarding.Provenance
import com.ascend.core.model.onboarding.SessionDuration
import com.ascend.core.model.onboarding.TimeWindow
import com.ascend.core.model.onboarding.TrainingDaysPerWeek
import com.ascend.core.model.onboarding.TrainingEnvironment
import com.ascend.core.model.onboarding.TrainingFrequency
import com.ascend.core.model.onboarding.Weekday

/**
 * The in-progress onboarding answers. Weight is held canonically in kilograms (converted at the
 * input boundary); [weightUnit] is display-only. Nothing here is verified — it becomes a
 * [Provenance.SELF_REPORTED] [InitialAssessment] on completion.
 */
data class OnboardingDraft(
    val displayName: String = "",
    val ageRange: AgeRange? = null,
    val heightCm: Double? = null,
    val currentWeightKg: Double? = null,
    val goalWeightKg: Double? = null,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    val primaryGoal: PrimaryGoal? = null,
    val secondaryGoals: Set<PrimaryGoal> = emptySet(),
    val trainingFrequency: TrainingFrequency? = null,
    val experience: Map<ActivityDomain, ExperienceLevel> = emptyMap(),
    val preferences: Set<ActivityPreference> = emptySet(),
    val equipment: Set<Equipment> = emptySet(),
    val environment: TrainingEnvironment? = null,
    val trainingDays: TrainingDaysPerWeek? = null,
    val sessionDuration: SessionDuration? = null,
    val preferredDays: Set<Weekday> = emptySet(),
    val preferredWindows: Set<TimeWindow> = emptySet(),
    val restDays: Set<Weekday> = emptySet(),
    val ability: AbilitySnapshot = AbilitySnapshot(),
    val sex: OptionalSex = OptionalSex.NOT_SET,
    val waistCircumferenceCm: Double? = null,
    val estimatedBodyFatPercent: Double? = null,
    val limitations: Set<Limitation> = emptySet(),
    val safetyAcknowledged: Boolean = false,
    val selectedClassId: String? = null,
) {
    fun toAssessment(
        userId: String,
        category: AgeSafetyCategory,
        now: Long,
    ): InitialAssessment {
        val availability =
            if (trainingDays == null && sessionDuration == null) {
                null
            } else {
                Availability(
                    trainingDays = trainingDays ?: TrainingDaysPerWeek.FLEXIBLE,
                    sessionDuration = sessionDuration ?: SessionDuration.MIN_20_30,
                    preferredDays = preferredDays,
                    preferredTimeWindows = preferredWindows,
                    restDayPreferences = restDays,
                )
            }
        return InitialAssessment(
            userId = userId,
            primaryGoal = primaryGoal,
            secondaryGoals = secondaryGoals.toList(),
            trainingFrequency = trainingFrequency,
            activityExperience = experience,
            activityPreferences = preferences,
            abilitySnapshot = ability.takeUnless { it.isEmpty },
            equipment = equipment,
            environment = environment,
            availability = availability,
            physiology =
                PhysiologyProfile(
                    sex = sex,
                    waistCircumferenceCm = waistCircumferenceCm,
                    estimatedBodyFatPercent = estimatedBodyFatPercent,
                ),
            bodyMetrics = BodyMetrics(heightCm = heightCm, currentWeightKg = currentWeightKg, goalWeightKg = goalWeightKg),
            limitations = limitations,
            ageSafetyCategory = category,
            provenance = Provenance.SELF_REPORTED,
            selfReportedAt = now,
        )
    }
}

/** Rebuild an editable draft from a persisted (in-progress) assessment, for resume. */
fun InitialAssessment.toDraft(): OnboardingDraft =
    OnboardingDraft(
        heightCm = bodyMetrics.heightCm,
        currentWeightKg = bodyMetrics.currentWeightKg,
        goalWeightKg = bodyMetrics.goalWeightKg,
        primaryGoal = primaryGoal,
        secondaryGoals = secondaryGoals.toSet(),
        trainingFrequency = trainingFrequency,
        experience = activityExperience,
        preferences = activityPreferences,
        equipment = equipment,
        environment = environment,
        trainingDays = availability?.trainingDays,
        sessionDuration = availability?.sessionDuration,
        preferredDays = availability?.preferredDays ?: emptySet(),
        preferredWindows = availability?.preferredTimeWindows ?: emptySet(),
        restDays = availability?.restDayPreferences ?: emptySet(),
        ability = abilitySnapshot ?: AbilitySnapshot(),
        sex = physiology.sex,
        waistCircumferenceCm = physiology.waistCircumferenceCm,
        estimatedBodyFatPercent = physiology.estimatedBodyFatPercent,
        limitations = limitations,
    )

/** Immutable UI state for the onboarding stepper. */
data class OnboardingUiState(
    val loading: Boolean = true,
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val draft: OnboardingDraft = OnboardingDraft(),
    val ageSafetyCategory: AgeSafetyCategory = AgeSafetyCategory.NOT_PROVIDED,
    val ageIneligible: Boolean = false,
    val reducedMotion: Boolean = false,
    val validationError: String? = null,
    val affinity: ClassAffinityResult? = null,
    val classDefinitions: List<ClassDefinition> = emptyList(),
    val plan: InitialQuestPlan? = null,
    val completed: Boolean = false,
) {
    /** The ordered content steps shown in the progress indicator (Welcome/Completion excluded). */
    val progressSteps: List<OnboardingStep>
        get() = OnboardingStep.entries.filter { it != OnboardingStep.WELCOME && it != OnboardingStep.COMPLETION }

    /** 1-based position of the current step among content steps, or 0 for Welcome/Completion. */
    val progressIndex: Int get() = progressSteps.indexOf(currentStep) + 1

    val progressTotal: Int get() = progressSteps.size
}
