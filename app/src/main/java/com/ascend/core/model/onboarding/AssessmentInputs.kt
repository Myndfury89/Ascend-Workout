package com.ascend.core.model.onboarding

/*
 * Typed onboarding inputs. Enums rather than free text so the class-affinity and initial-plan logic
 * is deterministic and testable. Body-related fields are optional, private estimates. Nothing here
 * is verified — it is all provisional self-report (see [Provenance]).
 */

/** A training goal. One primary + optional secondaries; no somatotype/"body type" categories. */
enum class PrimaryGoal(val displayName: String) {
    FAT_LOSS("Fat loss"),
    MUSCLE_GAIN("Muscle gain"),
    STRENGTH("Strength"),
    ENDURANCE("Endurance"),
    SPEED_AGILITY("Speed & agility"),
    MOBILITY("Mobility"),
    GENERAL_HEALTH("General health"),
    SPORT_PERFORMANCE("Sport performance"),
    MAINTENANCE("Maintenance"),
}

/** Self-reported recent consistency — informs conservative starting volume, never verified evidence. */
enum class TrainingFrequency {
    NEW_OR_RETURNING,
    LESS_THAN_ONCE_WEEKLY,
    ONE_TO_TWO_WEEKLY,
    THREE_TO_FOUR_WEEKLY,
    FIVE_PLUS_WEEKLY,
}

/** Activity domains experience is recorded against, separately. */
enum class ActivityDomain {
    STRENGTH_TRAINING,
    CARDIO,
    CALISTHENICS,
    MOBILITY,
    RUNNING,
    SWIMMING,
    COMBAT_SPORTS,
    SPORT_TRAINING,
}

/** Experience never hard-gates a class/Skill/rank/attribute — it only shapes starting recommendations. */
enum class ExperienceLevel { NONE, SOME, EXPERIENCED }

/** Activities the user enjoys or wants to explore. Influences recommendations, not safety or access. */
enum class ActivityPreference {
    WEIGHTLIFTING,
    BODYWEIGHT,
    WALKING,
    RUNNING,
    CYCLING,
    ELLIPTICAL,
    ROWING,
    SWIMMING,
    BOXING,
    MARTIAL_ARTS,
    MOBILITY,
    STRETCHING,
    SPORTS_TRAINING,
    MIXED_TRAINING,
}

/** Available equipment. The initial plan only prescribes activities compatible with these. */
enum class Equipment {
    NONE,
    DUMBBELLS,
    ADJUSTABLE_DUMBBELLS,
    BARBELL_PLATES,
    WEIGHT_MACHINES,
    RESISTANCE_BANDS,
    PULL_UP_BAR,
    BENCH,
    CARDIO_MACHINES,
    POOL,
    BOXING_EQUIPMENT,
    KETTLEBELLS,
    OUTDOOR_SPACE,
}

/** Where the user trains. */
enum class TrainingEnvironment { HOME, GYM, OUTDOORS, MIXED }

/** Typical session length band. [upperMinutes] is used to keep initial prescriptions within time. */
enum class SessionDuration(val upperMinutes: Int) {
    UNDER_20(20),
    MIN_20_30(30),
    MIN_30_45(45),
    MIN_45_60(60),
    OVER_60(90),
}

/** Training days per week; [count] is null for FLEXIBLE (never assume the user can train every day). */
enum class TrainingDaysPerWeek(val count: Int?) {
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    FLEXIBLE(null),
}

/** Preferred time-of-day windows (coarse). */
enum class TimeWindow { EARLY_MORNING, MORNING, MIDDAY, AFTERNOON, EVENING, NIGHT, FLEXIBLE }

/** Days of the week the user prefers to train (coarse scheduling hint). */
enum class Weekday { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

/**
 * Exercise-filtering signals — NOT diagnoses. Used only to avoid incompatible suggestions, offer
 * substitutions, reduce starting intensity, and show safety messaging. Never reduces class access
 * and is never publicly exposed.
 */
enum class Limitation {
    NONE_REPORTED,
    UPPER_BODY,
    LOWER_BODY,
    BACK_SENSITIVITY,
    LIMITED_MOBILITY,
    RETURNING_FROM_INJURY,
    PREGNANCY_POSTPARTUM,
    OTHER,
    PREFER_NOT_TO_ANSWER,
}

/**
 * Optional physiological sex — improves certain energy-use / body-composition estimates only. It
 * MUST NOT affect class/Skill/exercise access, attribute/rank potential, Dungeon access, social
 * visibility, or progression multipliers. Never inferred from name/weight/appearance/goals.
 */
enum class OptionalSex { FEMALE, MALE, PREFER_NOT_TO_ANSWER, NOT_SET }

/** Comfortable (not maximal) pull-up capability band — never a failure/max test. */
enum class PullUpCapability { NONE, ASSISTED, FEW, MANY }

/**
 * Optional, comfortable recent-performance snapshot. Every value is provisional self-report and must
 * NOT create PRs, Skill unlocks, XP, progression events, Dungeon readiness, or verified history.
 * Requests only comfortable recent performance — never 1RM, failure, or max tests.
 */
data class AbilitySnapshot(
    val comfortablePushUps: Int? = null,
    val comfortableSquats: Int? = null,
    val pullUpCapability: PullUpCapability? = null,
    val longestRecentCardioMinutes: Int? = null,
    val averageDailySteps: Int? = null,
    val recentStrengthTraining: Boolean? = null,
    val preferredCardioType: ActivityPreference? = null,
    val typicalWorkoutDuration: SessionDuration? = null,
) {
    val isEmpty: Boolean
        get() =
            comfortablePushUps == null && comfortableSquats == null && pullUpCapability == null &&
                longestRecentCardioMinutes == null && averageDailySteps == null &&
                recentStrengthTraining == null && preferredCardioType == null && typicalWorkoutDuration == null
}

/** When and how often the user can train. The initial plan must fit within this. */
data class Availability(
    val trainingDays: TrainingDaysPerWeek = TrainingDaysPerWeek.FLEXIBLE,
    val sessionDuration: SessionDuration = SessionDuration.MIN_20_30,
    val preferredDays: Set<Weekday> = emptySet(),
    val preferredTimeWindows: Set<TimeWindow> = emptySet(),
    val restDayPreferences: Set<Weekday> = emptySet(),
) {
    /** Effective number of training days to plan for; FLEXIBLE assumes a conservative 3. */
    val effectiveTrainingDays: Int get() = trainingDays.count ?: FLEXIBLE_DEFAULT_DAYS

    private companion object {
        const val FLEXIBLE_DEFAULT_DAYS = 3
    }
}

/** Optional, private physiology estimates. All optional; clearly marked as estimates. */
data class PhysiologyProfile(
    val sex: OptionalSex = OptionalSex.NOT_SET,
    val waistCircumferenceCm: Double? = null,
    val estimatedBodyFatPercent: Double? = null,
)

/** Optional, private body metrics. Canonical storage is centimetres / kilograms. */
data class BodyMetrics(
    val heightCm: Double? = null,
    val currentWeightKg: Double? = null,
    val goalWeightKg: Double? = null,
)
