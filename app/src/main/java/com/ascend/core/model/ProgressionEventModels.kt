package com.ascend.core.model

/**
 * A single atomic thing that happened to the player's progression, recorded so the
 * *presentation* of it (the Status‑screen animation) is decoupled from the
 * transaction that *earned* it. Events are persisted the moment they're earned and
 * consumed later by whatever is on screen — so a reward can be closed mid‑animation
 * and replayed on next open, exactly once.
 */
enum class ProgressionEventType {
    XP_GAINED,
    ATTRIBUTE_CHANGED,
    LEVEL_UP,
    RANK_UP,
    CLASS_XP_GAINED,
    CLASS_LEVEL_UP,
    PROFICIENCY_GAINED,
}

/**
 * One event within a [batchId] (all the events earned by a single source — e.g. one
 * quest completion — share a batch and play as a chained sequence in [sequence]
 * order). [fromValue] → [toValue] is the animation script: XP lifetime totals,
 * attribute totals, level numbers, or rank ordinals depending on [type].
 */
data class ProgressionEvent(
    val id: String,
    val userId: String,
    val batchId: String,
    val sequence: Int,
    val type: ProgressionEventType,
    val sourceType: XpSourceType,
    val sourceId: String,
    val attribute: AttributeType? = null,
    // Class id or proficiency key for class events (null for player events).
    val subjectKey: String? = null,
    val fromValue: Long = 0,
    val toValue: Long = 0,
    val label: String? = null,
    val createdAt: Long = 0,
    val consumedAt: Long? = null,
) {
    val delta: Long get() = toValue - fromValue
    val isConsumed: Boolean get() = consumedAt != null
}

/**
 * An immutable snapshot of the progression facts the animation needs, taken before
 * and after an earning transaction. Maps directly from [PlayerProgress] + the
 * attribute totals, so production can build events from the same data the
 * quest/workout completion already computes.
 */
data class ProgressionSnapshot(
    val level: Int,
    val lifetimeXp: Long,
    val rank: Rank,
    val attributes: Map<AttributeType, Long>,
)
