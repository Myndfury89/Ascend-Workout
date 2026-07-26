package com.ascend.core.domain.repository

import com.ascend.core.model.AttributeType
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.flow.Flow

/** Outcome of an XP award attempt. Idempotency is a first-class result, not an error. */
sealed interface XpAwardResult {
    data class Awarded(val amount: Long, val newLevel: Int, val leveledUp: Boolean) : XpAwardResult

    data object Duplicate : XpAwardResult
}

/**
 * Owns the immutable XP/attribute ledgers and the derived player state. Every
 * mutation is transactional and idempotent on (transactionType, sourceType, sourceId).
 */
interface ProgressionRepository {
    fun observeProgress(userId: String): Flow<PlayerProgress?>

    fun observeStats(userId: String): Flow<PlayerStats?>

    suspend fun getProgress(userId: String): PlayerProgress?

    /** Award XP exactly once for [sourceType]+[sourceId]; recomputes level & rank. */
    suspend fun awardXp(
        userId: String,
        amount: Long,
        sourceType: XpSourceType,
        sourceId: String,
        description: String,
    ): XpAwardResult

    /** Reverse a previously awarded XP amount (idempotent); recomputes level & rank. */
    suspend fun reverseXp(
        userId: String,
        sourceType: XpSourceType,
        sourceId: String,
    ): XpAwardResult

    /** Grant attribute deltas exactly once each for the given source; recomputes stats. */
    suspend fun awardAttributes(
        userId: String,
        deltas: Map<AttributeType, Long>,
        sourceType: XpSourceType,
        sourceId: String,
    )
}
