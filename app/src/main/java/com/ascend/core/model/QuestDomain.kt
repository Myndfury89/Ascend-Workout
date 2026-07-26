package com.ascend.core.model

/** A single logged contribution toward an objective (e.g. one set of push-ups). */
data class QuestProgressEntry(
    val id: String,
    val objectiveId: String,
    val value: Double,
    val source: ProgressSource,
    val note: String?,
    val completedAt: Long,
    val sourceApplication: String? = null,
    val externalRecordId: String? = null,
)

data class QuestObjective(
    val id: String,
    val questId: String,
    val exerciseId: String?,
    val title: String,
    val type: ObjectiveType,
    val target: Double,
    val current: Double,
    val unit: String,
    val preferredSetSize: Int?,
    val minimumSetSize: Int?,
    val maximumSetSize: Int?,
    val primaryAttribute: AttributeType?,
    val status: QuestStatus,
    val orderIndex: Int,
    val entries: List<QuestProgressEntry>,
) {
    val remaining: Double get() = (target - current).coerceAtLeast(0.0)
    val fraction: Float get() = if (target <= 0.0) 1f else (current / target).toFloat().coerceIn(0f, 1f)
    val isComplete: Boolean get() = current >= target
}

data class Quest(
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val type: QuestType,
    val status: QuestStatus,
    val scheduledDate: Long?,
    val deadline: Long?,
    val difficulty: Difficulty,
    val baseRewardXp: Long,
    val partialRewardEnabled: Boolean,
    val overCompletionEnabled: Boolean,
    val objectives: List<QuestObjective>,
) {
    val totalTarget: Double get() = objectives.sumOf { it.target }
    val totalCurrent: Double get() = objectives.sumOf { it.current }
    val remaining: Double get() = (totalTarget - totalCurrent).coerceAtLeast(0.0)
    val fraction: Float get() = if (totalTarget <= 0.0) 1f else (totalCurrent / totalTarget).toFloat().coerceIn(0f, 1f)
    val isComplete: Boolean get() = objectives.isNotEmpty() && objectives.all { it.isComplete }
    val primaryObjective: QuestObjective? get() = objectives.minByOrNull { it.orderIndex }
}
