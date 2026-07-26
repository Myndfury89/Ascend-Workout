package com.ascend.core.model

/** Kind of XP ledger entry. */
enum class XpTransactionType {
    AWARD,
    REVERSAL,
    ADJUSTMENT,
}

/**
 * Source category for an XP or attribute award. Combined with a source id, this
 * uniquely identifies a rewardable event and enforces "award XP exactly once".
 */
enum class XpSourceType {
    QUEST_COMPLETION,
    QUEST_PARTIAL,
    QUEST_OVER_COMPLETION,
    WORKOUT_COMPLETION,
    PERSONAL_RECORD,
    STREAK_BONUS,
    EXPEDITION_COMPLETION,
    RECOVERY_ACTIVITY,
    ACHIEVEMENT,
    CONSISTENCY,
    MANUAL_ADJUSTMENT,
}
