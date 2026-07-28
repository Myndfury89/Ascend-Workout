package com.ascend.core.model

/**
 * A reusable Daily Quest blueprint with **configurable safe ranges** — the single
 * source of a quest's target/set limits, stored as seed data (never hardcoded into
 * Compose screens or calculators). Users pick any target within
 * [minimumTarget]..[maximumTarget]; values above [safetyWarningThreshold] require an
 * explicit confirmation.
 */
data class QuestTemplate(
    val id: String,
    val name: String,
    val objectiveType: ObjectiveType,
    val unit: String,
    val primaryAttribute: AttributeType?,
    val exerciseId: String?,
    val minimumTarget: Int,
    val maximumTarget: Int,
    val defaultTarget: Int,
    val targetStep: Int,
    val defaultQuickAddValues: List<Int>,
    val defaultPreferredSetSize: Int,
    val minimumAllowedSetSize: Int,
    val maximumAllowedSetSize: Int,
    val supportsAutomaticProgress: Boolean,
    val supportsManualProgress: Boolean,
    val supportedVariations: List<String>,
    val safetyWarningThreshold: Int,
    val baseRewardXp: Long,
    val isBuiltIn: Boolean = true,
) {
    val allowedTargetRange: IntRange get() = minimumTarget..maximumTarget

    val allowedSetSizeRange: IntRange get() = minimumAllowedSetSize..maximumAllowedSetSize
}

/** Result of validating a chosen quest target against a template's safe ranges. */
sealed interface QuestTargetValidation {
    /**
     * The target is within range. [requiresConfirmation] is true when it exceeds the
     * safety threshold or a large jump over the recent baseline — allowed, but the UI
     * must ask for acknowledgment first ([warning] explains why).
     */
    data class Accepted(
        val target: Int,
        val requiresConfirmation: Boolean,
        val warning: String?,
    ) : QuestTargetValidation

    /** The target is outside the allowed range and is rejected. */
    data class Rejected(
        val reason: Reason,
        val allowedRange: IntRange,
        val message: String,
    ) : QuestTargetValidation {
        enum class Reason { BELOW_MINIMUM, ABOVE_MAXIMUM }
    }
}
