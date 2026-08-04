package com.ascend.core.domain.onboarding

import com.ascend.core.model.ActivityTags
import com.ascend.core.model.onboarding.ActivityDomain
import com.ascend.core.model.onboarding.ActivityPreference
import com.ascend.core.model.onboarding.PrimaryGoal

/**
 * Maps onboarding *choices* (goals, enjoyed activities, experienced domains) onto the existing
 * [ActivityTags] vocabulary the class-affinity engine scores against. This is the ONLY thing class
 * affinity is allowed to consider — what the user wants to train and enjoys — never protected or
 * physiological traits (sex, gender, weight, body-fat, disability, limitations, age, height, race,
 * pregnancy). Those are simply not inputs here.
 */
object GoalAffinityMapping {
    fun goalTags(goal: PrimaryGoal): Set<String> =
        when (goal) {
            PrimaryGoal.FAT_LOSS -> setOf(ActivityTags.STEADY_STATE_CARDIO, ActivityTags.HIGH_INTENSITY_CARDIO)
            PrimaryGoal.MUSCLE_GAIN -> setOf(ActivityTags.HYPERTROPHY)
            PrimaryGoal.STRENGTH -> setOf(ActivityTags.HEAVY_STRENGTH)
            PrimaryGoal.ENDURANCE -> setOf(ActivityTags.MUSCULAR_ENDURANCE, ActivityTags.STEADY_STATE_CARDIO)
            PrimaryGoal.SPEED_AGILITY -> setOf(ActivityTags.EXPLOSIVE, ActivityTags.HIGH_INTENSITY_CARDIO)
            PrimaryGoal.MOBILITY -> setOf(ActivityTags.MOBILITY, ActivityTags.BALANCE)
            PrimaryGoal.GENERAL_HEALTH -> setOf(ActivityTags.STEADY_STATE_CARDIO, ActivityTags.RECOVERY)
            PrimaryGoal.SPORT_PERFORMANCE -> setOf(ActivityTags.EXPLOSIVE, ActivityTags.COMBAT)
            PrimaryGoal.MAINTENANCE -> setOf(ActivityTags.RECOVERY)
        }

    fun goalKeywords(goal: PrimaryGoal): Set<String> = goal.displayName.lowercase().split(Regex("[^a-z]+")).filter { it.length > 3 }.toSet()

    fun preferenceTags(pref: ActivityPreference): Set<String> =
        when (pref) {
            ActivityPreference.WEIGHTLIFTING -> setOf(ActivityTags.HEAVY_STRENGTH, ActivityTags.HYPERTROPHY)
            ActivityPreference.BODYWEIGHT -> setOf(ActivityTags.BODYWEIGHT, ActivityTags.MUSCULAR_ENDURANCE)
            ActivityPreference.WALKING -> setOf(ActivityTags.STEADY_STATE_CARDIO)
            ActivityPreference.RUNNING -> setOf(ActivityTags.STEADY_STATE_CARDIO, ActivityTags.HIGH_INTENSITY_CARDIO)
            ActivityPreference.CYCLING -> setOf(ActivityTags.STEADY_STATE_CARDIO)
            ActivityPreference.ELLIPTICAL -> setOf(ActivityTags.STEADY_STATE_CARDIO)
            ActivityPreference.ROWING -> setOf(ActivityTags.MUSCULAR_ENDURANCE, ActivityTags.STEADY_STATE_CARDIO)
            ActivityPreference.SWIMMING -> setOf(ActivityTags.AQUATIC, ActivityTags.STEADY_STATE_CARDIO)
            ActivityPreference.BOXING -> setOf(ActivityTags.COMBAT, ActivityTags.EXPLOSIVE)
            ActivityPreference.MARTIAL_ARTS -> setOf(ActivityTags.COMBAT, ActivityTags.BALANCE)
            ActivityPreference.MOBILITY -> setOf(ActivityTags.MOBILITY)
            ActivityPreference.STRETCHING -> setOf(ActivityTags.MOBILITY, ActivityTags.RECOVERY)
            ActivityPreference.SPORTS_TRAINING -> setOf(ActivityTags.EXPLOSIVE, ActivityTags.COMBAT)
            ActivityPreference.MIXED_TRAINING -> emptySet()
        }

    fun domainTags(domain: ActivityDomain): Set<String> =
        when (domain) {
            ActivityDomain.STRENGTH_TRAINING -> setOf(ActivityTags.HEAVY_STRENGTH, ActivityTags.HYPERTROPHY)
            ActivityDomain.CARDIO -> setOf(ActivityTags.STEADY_STATE_CARDIO, ActivityTags.HIGH_INTENSITY_CARDIO)
            ActivityDomain.CALISTHENICS -> setOf(ActivityTags.BODYWEIGHT, ActivityTags.MUSCULAR_ENDURANCE)
            ActivityDomain.MOBILITY -> setOf(ActivityTags.MOBILITY, ActivityTags.BALANCE)
            ActivityDomain.RUNNING -> setOf(ActivityTags.STEADY_STATE_CARDIO, ActivityTags.HIGH_INTENSITY_CARDIO)
            ActivityDomain.SWIMMING -> setOf(ActivityTags.AQUATIC)
            ActivityDomain.COMBAT_SPORTS -> setOf(ActivityTags.COMBAT, ActivityTags.EXPLOSIVE)
            ActivityDomain.SPORT_TRAINING -> setOf(ActivityTags.EXPLOSIVE, ActivityTags.COMBAT)
        }
}
