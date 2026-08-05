package com.ascend.core.domain.onboarding

import com.ascend.core.domain.quest.CreateFromTemplateResult
import com.ascend.core.domain.quest.CreateQuestFromTemplateUseCase
import com.ascend.core.domain.quest.QuestFromTemplateRequest
import com.ascend.core.domain.repository.QuestRepository
import com.ascend.core.model.onboarding.InitialQuestPlan
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Realizes a provisional [InitialQuestPlan] into real starting Daily Quests using the **existing**
 * quest infrastructure ([CreateQuestFromTemplateUseCase]) — never a second quest engine and never
 * any adaptive-training reward math. The plan's targets are already conservative, equipment/
 * environment/availability-filtered, and minor-capped by the generator, so each definition creates a
 * normal accumulation quest that awards nothing until the user actually completes it.
 *
 * Idempotent: a quest whose title already exists for the user is skipped, so re-running never
 * duplicates the starting schedule. Assessment-Quest *suggestions* are not created here — they only
 * become real (and verified) once the user performs the activity.
 */
class CreateInitialQuestScheduleUseCase
    @Inject
    constructor(
        private val createFromTemplate: CreateQuestFromTemplateUseCase,
        private val questRepository: QuestRepository,
    ) {
        suspend fun create(
            userId: String,
            plan: InitialQuestPlan,
        ): List<String> {
            val existingTitles = questRepository.observeQuestsForUser(userId).first().map { it.title }.toSet()
            val created = mutableListOf<String>()
            plan.questDefinitions.forEach { def ->
                if (def.name in existingTitles) return@forEach
                val result =
                    createFromTemplate.create(
                        QuestFromTemplateRequest(
                            userId = userId,
                            templateId = def.templateId,
                            target = def.target,
                            preferredSetSize = def.preferredSetSize,
                            confirmedHighTarget = false,
                        ),
                    )
                if (result is CreateFromTemplateResult.Created) created += result.questId
            }
            return created
        }
    }
