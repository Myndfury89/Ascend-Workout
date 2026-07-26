package com.ascend.core.domain.repository

import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.ProgressSource
import com.ascend.core.model.Quest
import com.ascend.core.model.QuestType
import com.ascend.core.model.RewardBreakdown
import kotlinx.coroutines.flow.Flow

data class NewObjectiveSpec(
    val title: String,
    val type: ObjectiveType,
    val target: Double,
    val unit: String,
    val exerciseId: String? = null,
    val preferredSetSize: Int? = null,
    val minimumSetSize: Int? = null,
    val maximumSetSize: Int? = null,
    val primaryAttribute: AttributeType? = null,
    val orderIndex: Int = 0,
)

data class NewQuestSpec(
    val userId: String,
    val title: String,
    val type: QuestType,
    val objectives: List<NewObjectiveSpec>,
    val description: String? = null,
    val scheduledDate: Long? = null,
    val deadline: Long? = null,
    val difficulty: Difficulty = Difficulty.MODERATE,
    val baseRewardXp: Long = 0,
    val partialRewardEnabled: Boolean = true,
    val overCompletionEnabled: Boolean = true,
)

sealed interface AddProgressResult {
    data class Added(val entryId: String, val objectiveCurrent: Double, val objectiveComplete: Boolean) : AddProgressResult

    data object Duplicate : AddProgressResult

    data object NotFound : AddProgressResult
}

sealed interface CompleteQuestResult {
    data class Completed(
        val xpAwarded: Long,
        val newLevel: Int,
        val leveledUp: Boolean,
        val attributeDeltas: Map<AttributeType, Long>,
        val rewardBreakdown: RewardBreakdown,
    ) : CompleteQuestResult

    data object AlreadyCompleted : CompleteQuestResult

    data object NotFound : CompleteQuestResult
}

interface QuestRepository {
    fun observeQuestsForUser(userId: String): Flow<List<Quest>>

    fun observeQuest(questId: String): Flow<Quest?>

    suspend fun getQuest(questId: String): Quest?

    suspend fun createQuest(spec: NewQuestSpec): String

    /** Log a contribution toward an objective; recomputes cumulative progress. */
    suspend fun addProgress(
        objectiveId: String,
        value: Double,
        source: ProgressSource = ProgressSource.MANUAL,
        note: String? = null,
        perceivedEffort: Int? = null,
        sourceApplication: String? = null,
        externalRecordId: String? = null,
    ): AddProgressResult

    suspend fun editProgress(
        entryId: String,
        newValue: Double,
    ): Boolean

    suspend fun deleteProgress(entryId: String): Boolean

    /** Complete a quest, awarding XP + attributes exactly once (idempotent). */
    suspend fun completeQuest(questId: String): CompleteQuestResult
}
