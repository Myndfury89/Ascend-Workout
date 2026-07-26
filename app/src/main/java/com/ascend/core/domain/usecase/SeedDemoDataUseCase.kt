package com.ascend.core.domain.usecase

import com.ascend.core.domain.repository.NewObjectiveSpec
import com.ascend.core.domain.repository.NewQuestSpec
import com.ascend.core.domain.repository.QuestRepository
import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.QuestType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Seeds the milestone demo content (idempotent): the flagship 200 Push-Ups quest,
 * so the flow is reachable on first launch. Runs only when the user has no quests.
 */
class SeedDemoDataUseCase @Inject constructor(
    private val questRepository: QuestRepository,
) {
    suspend operator fun invoke(userId: String) {
        if (questRepository.observeQuestsForUser(userId).first().isNotEmpty()) return

        questRepository.createQuest(
            NewQuestSpec(
                userId = userId,
                title = "Upper-Body Trial",
                type = QuestType.ACCUMULATION,
                description = "Reach 200 push-ups today, split however you like.",
                difficulty = Difficulty.MODERATE,
                baseRewardXp = 350,
                objectives = listOf(
                    NewObjectiveSpec(
                        title = "Push-ups",
                        type = ObjectiveType.REPETITIONS,
                        target = 200.0,
                        unit = "reps",
                        preferredSetSize = 25,
                        minimumSetSize = 10,
                        maximumSetSize = 50,
                        primaryAttribute = AttributeType.STRENGTH,
                    ),
                ),
            ),
        )
    }
}
