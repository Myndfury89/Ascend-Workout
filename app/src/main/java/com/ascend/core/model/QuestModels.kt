package com.ascend.core.model

/** How a quest is completed. */
enum class QuestType {
    SINGLE_ACTION,
    ACCUMULATION,
    SCHEDULED_WORKOUT,
    QUEST_CHAIN,
    RECURRING,
    PROGRESSIVE,
    RECOVERY,
    HEALTH_DATA,
    PERSONAL_RECORD,
    EXPEDITION,
}

/** Lifecycle status of a quest. */
enum class QuestStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    IN_PROGRESS,
    COMPLETED,
    OVER_COMPLETED,
    PARTIALLY_COMPLETED,
    SKIPPED,
    EXPIRED,
    RESCHEDULED,
    CANCELLED,
}

/** What an objective measures. */
enum class ObjectiveType {
    REPETITIONS,
    WEIGHT_AND_REPS,
    DURATION,
    DISTANCE,
    CALORIES,
    STEPS,
    ROUNDS,
    CUSTOM,
}

/** How a multi-objective chain is considered complete. */
enum class CompletionRule {
    ALL_REQUIRED,
    POINTS_BASED,
}

/** Where a progress entry / import originated. */
enum class ProgressSource {
    MANUAL,
    HEALTH_CONNECT,
    IMPORT_FILE,
    PARTNER_API,
    SYSTEM,
}

enum class Difficulty(val displayName: String) {
    EASY("Easy"),
    MODERATE("Moderate"),
    HARD("Hard"),
    EXTREME("Extreme"),
}

enum class MeasurementSystem {
    METRIC,
    IMPERIAL,
}
