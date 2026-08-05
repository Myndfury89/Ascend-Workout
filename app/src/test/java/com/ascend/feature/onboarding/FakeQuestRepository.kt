package com.ascend.feature.onboarding

import com.ascend.core.domain.repository.AddProgressResult
import com.ascend.core.domain.repository.CompleteQuestResult
import com.ascend.core.domain.repository.NewQuestSpec
import com.ascend.core.domain.repository.QuestRepository
import com.ascend.core.model.ProgressSource
import com.ascend.core.model.Quest
import com.ascend.core.model.QuestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Minimal in-memory QuestRepository for onboarding tests: stores created quests, ignores the rest. */
class FakeQuestRepository : QuestRepository {
    private val quests = MutableStateFlow<List<Quest>>(emptyList())

    override fun observeQuestsForUser(userId: String): Flow<List<Quest>> = quests.map { list -> list.filter { it.userId == userId } }

    override fun observeQuest(questId: String): Flow<Quest?> = quests.map { list -> list.firstOrNull { it.id == questId } }

    override suspend fun getQuest(questId: String): Quest? = quests.value.firstOrNull { it.id == questId }

    override suspend fun createQuest(spec: NewQuestSpec): String {
        val id = "quest-${quests.value.size}"
        quests.value =
            quests.value +
            Quest(
                id = id,
                userId = spec.userId,
                title = spec.title,
                description = spec.description,
                type = spec.type,
                status = QuestStatus.ACTIVE,
                scheduledDate = spec.scheduledDate,
                deadline = spec.deadline,
                difficulty = spec.difficulty,
                baseRewardXp = spec.baseRewardXp,
                partialRewardEnabled = spec.partialRewardEnabled,
                overCompletionEnabled = spec.overCompletionEnabled,
                objectives = emptyList(),
            )
        return id
    }

    override suspend fun addProgress(
        objectiveId: String,
        value: Double,
        source: ProgressSource,
        note: String?,
        perceivedEffort: Int?,
        sourceApplication: String?,
        externalRecordId: String?,
    ): AddProgressResult = throw NotImplementedError("unused in onboarding tests")

    override suspend fun editProgress(
        entryId: String,
        newValue: Double,
    ): Boolean = throw NotImplementedError("unused in onboarding tests")

    override suspend fun deleteProgress(entryId: String): Boolean = throw NotImplementedError("unused in onboarding tests")

    override suspend fun updateTarget(
        objectiveId: String,
        newTarget: Double,
    ): Boolean = throw NotImplementedError("unused in onboarding tests")

    override suspend fun completeQuest(questId: String): CompleteQuestResult = throw NotImplementedError("unused in onboarding tests")
}
