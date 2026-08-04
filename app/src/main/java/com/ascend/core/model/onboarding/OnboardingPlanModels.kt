package com.ascend.core.model.onboarding

/*
 * Outputs of the onboarding domain logic: the class-affinity recommendation, the provisional
 * initial quest plan, and optional Assessment-Quest suggestions. All advisory and provisional —
 * none of these award anything or mark data as verified.
 */

/**
 * An explainable, non-locking class-affinity result. [classScores] holds every candidate class's
 * score (not just the winner) so the recommendation is transparent; the user may still choose any
 * available class regardless of [recommendedClassId]. [evidenceKeys] are the answer keys that
 * contributed — always enjoyment/goal/experience signals, never protected or physiological traits.
 */
data class ClassAffinityResult(
    val recommendedClassId: String?,
    val classScores: Map<String, Double>,
    val rationale: String,
    val evidenceKeys: List<String>,
)

/** Conservative-first starting difficulty. New/returning and minors start at [FOUNDATION]. */
enum class DifficultyBand { FOUNDATION, DEVELOPING, STEADY }

/**
 * One provisional starting quest, derived from an existing [com.ascend.core.model.QuestTemplate] and
 * its safe ranges (never a second quest engine). It is a *descriptor* only — it is realised into a
 * real quest via the existing quest infrastructure in a later checkpoint, and never awards anything.
 */
data class InitialQuestDefinition(
    val templateId: String,
    val name: String,
    val unit: String,
    val target: Int,
    val preferredSetSize: Int?,
    val rationale: String,
    val equipmentCompatible: Boolean = true,
    val scheduleCompatible: Boolean = true,
    val safetyAdjusted: Boolean = false,
    val provisional: Boolean = true,
)

/**
 * An optional, comfortable Assessment Quest suggested when information was skipped or too uncertain
 * to plan confidently. It only *establishes* information once real activity is completed — it never
 * requires maximum effort or failure, and never creates verified history from onboarding answers.
 */
data class AssessmentQuestSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val establishes: String,
    val relatedDomain: ActivityDomain? = null,
    val requiresMaximumEffort: Boolean = false,
)

/**
 * The provisional initial plan. [provisional] is always true; verified evidence and the existing
 * adaptive-training system may modify it later. [safetyAdjustments] are the human-readable reasons
 * the plan was made conservative (new/returning, minor, limitation substitutions, …).
 */
data class InitialQuestPlan(
    val userId: String,
    val questDefinitions: List<InitialQuestDefinition>,
    val assessmentSuggestions: List<AssessmentQuestSuggestion>,
    val rationale: String,
    val difficultyBand: DifficultyBand,
    val safetyAdjustments: List<String>,
    val provisional: Boolean = true,
    val createdAt: Long = 0L,
)
