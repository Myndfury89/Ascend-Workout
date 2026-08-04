package com.ascend.core.data.mapper

import com.ascend.core.database.entity.ClassAffinityResultEntity
import com.ascend.core.database.entity.InitialAssessmentEntity
import com.ascend.core.database.entity.InitialQuestPlanEntity
import com.ascend.core.database.entity.InitialQuestPlanItemEntity
import com.ascend.core.database.entity.OnboardingStateEntity
import com.ascend.core.model.onboarding.AbilitySnapshot
import com.ascend.core.model.onboarding.ActivityDomain
import com.ascend.core.model.onboarding.ActivityPreference
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.Availability
import com.ascend.core.model.onboarding.BodyMetrics
import com.ascend.core.model.onboarding.ClassAffinityResult
import com.ascend.core.model.onboarding.DifficultyBand
import com.ascend.core.model.onboarding.Equipment
import com.ascend.core.model.onboarding.ExperienceLevel
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.InitialQuestDefinition
import com.ascend.core.model.onboarding.InitialQuestPlan
import com.ascend.core.model.onboarding.Limitation
import com.ascend.core.model.onboarding.OnboardingState
import com.ascend.core.model.onboarding.OnboardingStep
import com.ascend.core.model.onboarding.OptionalSex
import com.ascend.core.model.onboarding.PhysiologyProfile
import com.ascend.core.model.onboarding.PrimaryGoal
import com.ascend.core.model.onboarding.Provenance
import com.ascend.core.model.onboarding.PullUpCapability
import com.ascend.core.model.onboarding.SessionDuration
import com.ascend.core.model.onboarding.TimeWindow
import com.ascend.core.model.onboarding.TrainingDaysPerWeek
import com.ascend.core.model.onboarding.TrainingEnvironment
import com.ascend.core.model.onboarding.TrainingFrequency
import com.ascend.core.model.onboarding.Weekday

/*
 * Entity <-> domain mapping for onboarding. Collections are stored as delimited strings. The unit
 * separator () delimits lists whose items may themselves contain punctuation (evidence keys,
 * safety-adjustment sentences); simple enum lists/maps use commas. Height is NOT stored here — it is
 * a stable profile attribute (user_profile.heightCm) merged in by the repository.
 */

private const val LIST_SEP = ""

private fun List<String>.joinList(): String = joinToString(LIST_SEP)

private fun String.splitList(): List<String> = if (isEmpty()) emptyList() else split(LIST_SEP)

private inline fun <reified E : Enum<E>> Collection<E>.joinEnums(): String = joinToString(",") { it.name }

private inline fun <reified E : Enum<E>> String.parseEnums(parse: (String) -> E): List<E> =
    if (isBlank()) emptyList() else split(",").filter { it.isNotBlank() }.map(parse)

// --- Onboarding state ---------------------------------------------------------------------------

fun OnboardingState.toEntity(userId: String) =
    OnboardingStateEntity(
        userId = userId,
        currentStep = currentStep.name,
        completedSteps = completedSteps.joinEnums(),
        skippedSteps = skippedSteps.joinEnums(),
        startedAt = startedAt,
        completedAt = completedAt,
        version = version,
    )

fun OnboardingStateEntity.toDomain() =
    OnboardingState(
        currentStep = OnboardingStep.valueOf(currentStep),
        completedSteps = completedSteps.parseEnums { OnboardingStep.valueOf(it) }.toSet(),
        skippedSteps = skippedSteps.parseEnums { OnboardingStep.valueOf(it) }.toSet(),
        startedAt = startedAt,
        completedAt = completedAt,
        version = version,
    )

// --- Initial assessment -------------------------------------------------------------------------

fun InitialAssessment.toEntity() =
    InitialAssessmentEntity(
        userId = userId,
        primaryGoal = primaryGoal?.name,
        secondaryGoals = secondaryGoals.joinEnums(),
        trainingFrequency = trainingFrequency?.name,
        activityExperience = activityExperience.entries.joinToString(",") { "${it.key.name}:${it.value.name}" },
        activityPreferences = activityPreferences.joinEnums(),
        equipment = equipment.joinEnums(),
        environment = environment?.name,
        limitations = limitations.joinEnums(),
        ageSafetyCategory = ageSafetyCategory.name,
        provenance = provenance.name,
        selfReportedAt = selfReportedAt,
        replacedByEvidenceAt = replacedByEvidenceAt,
        abilityPushUps = abilitySnapshot?.comfortablePushUps,
        abilitySquats = abilitySnapshot?.comfortableSquats,
        abilityPullUp = abilitySnapshot?.pullUpCapability?.name,
        abilityCardioMinutes = abilitySnapshot?.longestRecentCardioMinutes,
        abilitySteps = abilitySnapshot?.averageDailySteps,
        abilityRecentStrength = abilitySnapshot?.recentStrengthTraining,
        abilityPreferredCardio = abilitySnapshot?.preferredCardioType?.name,
        abilityTypicalDuration = abilitySnapshot?.typicalWorkoutDuration?.name,
        availabilityTrainingDays = availability?.trainingDays?.name,
        availabilitySessionDuration = availability?.sessionDuration?.name,
        availabilityPreferredDays = availability?.preferredDays.orEmpty().joinEnums(),
        availabilityTimeWindows = availability?.preferredTimeWindows.orEmpty().joinEnums(),
        availabilityRestDays = availability?.restDayPreferences.orEmpty().joinEnums(),
        physiologySex = physiology.sex.name,
        physiologyWaistCm = physiology.waistCircumferenceCm,
        physiologyBodyFatPercent = physiology.estimatedBodyFatPercent,
        currentWeightKg = bodyMetrics.currentWeightKg,
        goalWeightKg = bodyMetrics.goalWeightKg,
    )

/** [heightCm] is merged from the profile (the assessment table does not store height). */
fun InitialAssessmentEntity.toDomain(heightCm: Double?): InitialAssessment {
    val snapshot =
        AbilitySnapshot(
            comfortablePushUps = abilityPushUps,
            comfortableSquats = abilitySquats,
            pullUpCapability = abilityPullUp?.let { PullUpCapability.valueOf(it) },
            longestRecentCardioMinutes = abilityCardioMinutes,
            averageDailySteps = abilitySteps,
            recentStrengthTraining = abilityRecentStrength,
            preferredCardioType = abilityPreferredCardio?.let { ActivityPreference.valueOf(it) },
            typicalWorkoutDuration = abilityTypicalDuration?.let { SessionDuration.valueOf(it) },
        )
    val availability =
        if (availabilityTrainingDays == null && availabilitySessionDuration == null) {
            null
        } else {
            Availability(
                trainingDays = availabilityTrainingDays?.let { TrainingDaysPerWeek.valueOf(it) } ?: TrainingDaysPerWeek.FLEXIBLE,
                sessionDuration = availabilitySessionDuration?.let { SessionDuration.valueOf(it) } ?: SessionDuration.MIN_20_30,
                preferredDays = availabilityPreferredDays.parseEnums { Weekday.valueOf(it) }.toSet(),
                preferredTimeWindows = availabilityTimeWindows.parseEnums { TimeWindow.valueOf(it) }.toSet(),
                restDayPreferences = availabilityRestDays.parseEnums { Weekday.valueOf(it) }.toSet(),
            )
        }
    return InitialAssessment(
        userId = userId,
        primaryGoal = primaryGoal?.let { PrimaryGoal.valueOf(it) },
        secondaryGoals = secondaryGoals.parseEnums { PrimaryGoal.valueOf(it) },
        trainingFrequency = trainingFrequency?.let { TrainingFrequency.valueOf(it) },
        activityExperience =
            activityExperience
                .split(",")
                .filter { it.isNotBlank() }
                .associate {
                    val (d, l) = it.split(":")
                    ActivityDomain.valueOf(d) to ExperienceLevel.valueOf(l)
                },
        activityPreferences = activityPreferences.parseEnums { ActivityPreference.valueOf(it) }.toSet(),
        abilitySnapshot = if (snapshot.isEmpty) null else snapshot,
        equipment = equipment.parseEnums { Equipment.valueOf(it) }.toSet(),
        environment = environment?.let { TrainingEnvironment.valueOf(it) },
        availability = availability,
        physiology =
            PhysiologyProfile(
                sex = OptionalSex.valueOf(physiologySex),
                waistCircumferenceCm = physiologyWaistCm,
                estimatedBodyFatPercent = physiologyBodyFatPercent,
            ),
        bodyMetrics = BodyMetrics(heightCm = heightCm, currentWeightKg = currentWeightKg, goalWeightKg = goalWeightKg),
        limitations = limitations.parseEnums { Limitation.valueOf(it) }.toSet(),
        ageSafetyCategory = AgeSafetyCategory.valueOf(ageSafetyCategory),
        provenance = Provenance.valueOf(provenance),
        selfReportedAt = selfReportedAt,
        replacedByEvidenceAt = replacedByEvidenceAt,
    )
}

// --- Class affinity -----------------------------------------------------------------------------

fun ClassAffinityResult.toEntity(
    userId: String,
    createdAt: Long,
) = ClassAffinityResultEntity(
    userId = userId,
    recommendedClassId = recommendedClassId,
    classScores = classScores.entries.joinToString(",") { "${it.key}:${it.value}" },
    rationale = rationale,
    evidenceKeys = evidenceKeys.joinList(),
    createdAt = createdAt,
)

fun ClassAffinityResultEntity.toDomain() =
    ClassAffinityResult(
        recommendedClassId = recommendedClassId,
        classScores =
            classScores
                .split(",")
                .filter { it.isNotBlank() }
                .associate {
                    val idx = it.lastIndexOf(':')
                    it.substring(0, idx) to it.substring(idx + 1).toDouble()
                },
        rationale = rationale,
        evidenceKeys = evidenceKeys.splitList(),
    )

// --- Initial quest plan -------------------------------------------------------------------------

fun InitialQuestPlan.toEntity() =
    InitialQuestPlanEntity(
        userId = userId,
        difficultyBand = difficultyBand.name,
        rationale = rationale,
        safetyAdjustments = safetyAdjustments.joinList(),
        provisional = provisional,
        createdAt = createdAt,
    )

fun InitialQuestPlan.toItemEntities(): List<InitialQuestPlanItemEntity> =
    questDefinitions.mapIndexed { index, def ->
        InitialQuestPlanItemEntity(
            id = "$userId:${def.templateId}",
            planUserId = userId,
            templateId = def.templateId,
            name = def.name,
            unit = def.unit,
            target = def.target,
            preferredSetSize = def.preferredSetSize,
            rationale = def.rationale,
            equipmentCompatible = def.equipmentCompatible,
            scheduleCompatible = def.scheduleCompatible,
            safetyAdjusted = def.safetyAdjusted,
            provisional = def.provisional,
            sortOrder = index,
        )
    }

fun InitialQuestPlanEntity.toDomain(items: List<InitialQuestPlanItemEntity>) =
    InitialQuestPlan(
        userId = userId,
        questDefinitions =
            items.sortedBy { it.sortOrder }.map {
                InitialQuestDefinition(
                    templateId = it.templateId,
                    name = it.name,
                    unit = it.unit,
                    target = it.target,
                    preferredSetSize = it.preferredSetSize,
                    rationale = it.rationale,
                    equipmentCompatible = it.equipmentCompatible,
                    scheduleCompatible = it.scheduleCompatible,
                    safetyAdjusted = it.safetyAdjusted,
                    provisional = it.provisional,
                )
            },
        assessmentSuggestions = emptyList(),
        rationale = rationale,
        difficultyBand = DifficultyBand.valueOf(difficultyBand),
        safetyAdjustments = safetyAdjustments.splitList(),
        provisional = provisional,
        createdAt = createdAt,
    )
