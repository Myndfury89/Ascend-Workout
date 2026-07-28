package com.ascend.core.domain.quest

import com.ascend.core.domain.repository.NewObjectiveSpec
import com.ascend.core.domain.repository.NewQuestSpec
import com.ascend.core.domain.repository.QuestRepository
import com.ascend.core.domain.repository.QuestTemplateRepository
import com.ascend.core.model.QuestTargetValidation
import com.ascend.core.model.QuestTemplate
import com.ascend.core.model.QuestType
import javax.inject.Inject

/** The user's choices when creating a Daily Quest from a template. */
data class QuestFromTemplateRequest(
    val userId: String,
    val templateId: String,
    val target: Int,
    val preferredSetSize: Int? = null,
    val variation: String? = null,
    val scheduledDate: Long? = null,
    val recurrenceRule: String? = null,
    val recentBaseline: Int? = null,
    // Set true once the user acknowledges a high‑target warning.
    val confirmedHighTarget: Boolean = false,
)

sealed interface CreateFromTemplateResult {
    data class Created(val questId: String) : CreateFromTemplateResult

    /** In range but above safety/baseline; re‑submit with confirmedHighTarget = true. */
    data class NeedsConfirmation(val warning: String, val target: Int) : CreateFromTemplateResult

    data class Rejected(val validation: QuestTargetValidation.Rejected) : CreateFromTemplateResult

    data object TemplateNotFound : CreateFromTemplateResult
}

/**
 * Creates a Daily Quest from a template + the user's chosen target/set config,
 * validating against the template's configurable safe ranges. Rejects out‑of‑range
 * targets; requires acknowledgment for high targets; otherwise builds a normal
 * accumulation quest (uneven sets fully supported downstream).
 */
class CreateQuestFromTemplateUseCase
    @Inject
    constructor(
        private val questRepository: QuestRepository,
        private val questTemplateRepository: QuestTemplateRepository,
        private val validator: QuestTargetValidator,
    ) {
        suspend fun create(request: QuestFromTemplateRequest): CreateFromTemplateResult {
            val template =
                questTemplateRepository.getTemplate(request.templateId)
                    ?: return CreateFromTemplateResult.TemplateNotFound

            return when (val validation = validator.validate(template, request.target, request.recentBaseline)) {
                is QuestTargetValidation.Rejected -> CreateFromTemplateResult.Rejected(validation)
                is QuestTargetValidation.Accepted -> {
                    if (validation.requiresConfirmation && !request.confirmedHighTarget) {
                        CreateFromTemplateResult.NeedsConfirmation(validation.warning.orEmpty(), request.target)
                    } else {
                        CreateFromTemplateResult.Created(buildQuest(request, template))
                    }
                }
            }
        }

        private suspend fun buildQuest(
            request: QuestFromTemplateRequest,
            template: QuestTemplate,
        ): String {
            val setSize =
                (request.preferredSetSize ?: template.defaultPreferredSetSize)
                    .coerceIn(template.allowedSetSizeRange)
            return questRepository.createQuest(
                NewQuestSpec(
                    userId = request.userId,
                    title = template.name,
                    type = QuestType.ACCUMULATION,
                    description = request.variation?.let { "Variation: $it" },
                    scheduledDate = request.scheduledDate,
                    recurrenceRule = request.recurrenceRule,
                    baseRewardXp = template.baseRewardXp,
                    objectives =
                        listOf(
                            NewObjectiveSpec(
                                title = template.name,
                                type = template.objectiveType,
                                target = request.target.toDouble(),
                                unit = template.unit,
                                exerciseId = template.exerciseId,
                                preferredSetSize = setSize,
                                minimumSetSize = template.minimumAllowedSetSize,
                                maximumSetSize = template.maximumAllowedSetSize,
                                primaryAttribute = template.primaryAttribute,
                            ),
                        ),
                ),
            )
        }
    }
