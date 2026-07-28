package com.ascend.core.domain.quest

import com.ascend.core.model.QuestTargetValidation
import com.ascend.core.model.QuestTemplate
import javax.inject.Inject

/**
 * Validates a chosen Daily Quest target against a template's configurable safe
 * ranges. A value outside [min, max] is rejected (with the allowed range so the UI
 * can show it and preserve the entry). A value that is in range but exceeds the
 * safety threshold — or is a large jump over the user's recent baseline — is
 * accepted but flagged for confirmation. Difficulty alone never blocks a target.
 */
class QuestTargetValidator
    @Inject
    constructor() {
        fun validate(
            template: QuestTemplate,
            target: Int,
            recentBaseline: Int? = null,
        ): QuestTargetValidation {
            val range = template.allowedTargetRange
            if (target < template.minimumTarget) {
                return QuestTargetValidation.Rejected(
                    QuestTargetValidation.Rejected.Reason.BELOW_MINIMUM,
                    range,
                    "Minimum is ${template.minimumTarget} ${template.unit}.",
                )
            }
            if (target > template.maximumTarget) {
                return QuestTargetValidation.Rejected(
                    QuestTargetValidation.Rejected.Reason.ABOVE_MAXIMUM,
                    range,
                    "Maximum is ${template.maximumTarget} ${template.unit}.",
                )
            }

            val overSafety = target > template.safetyWarningThreshold
            val bigJump = recentBaseline != null && recentBaseline > 0 && target > recentBaseline * BASELINE_JUMP_FACTOR
            val requiresConfirmation = overSafety || bigJump
            val warning =
                when {
                    !requiresConfirmation -> null
                    else ->
                        "That's above your recent baseline. Consider splitting the work across more sets " +
                            "or reducing the target."
                }
            return QuestTargetValidation.Accepted(target, requiresConfirmation, warning)
        }

        private companion object {
            const val BASELINE_JUMP_FACTOR = 1.5
        }
    }
