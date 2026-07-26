package com.ascend.core.domain.repository

import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.PlayerClassSelection
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.flow.Flow

/** Outcome of a Class-XP award; idempotency is a first-class result. */
data class ClassXpAward(
    val classId: String,
    val awardedAmount: Long,
    val newLevel: Int,
    val leveledUp: Boolean,
    val duplicate: Boolean,
)

/**
 * Owns class definitions (seed data), the player's class selection, and the
 * class‑XP / unique‑proficiency ledgers. Every award is idempotent on
 * (classId|proficiencyKey, transactionType, sourceType, sourceId) — mirroring the
 * player XP/attribute ledgers.
 */
interface ClassRepository {
    fun definitions(): List<ClassDefinition>

    fun definition(classId: String?): ClassDefinition?

    suspend fun getSelection(userId: String): PlayerClassSelection

    fun observeSelection(userId: String): Flow<PlayerClassSelection>

    suspend fun setClasses(
        userId: String,
        primaryClassId: String?,
        secondaryClassId: String?,
    )

    /** Award Class XP exactly once for the source; recomputes the class level. */
    suspend fun awardClassXp(
        userId: String,
        classId: String,
        amount: Long,
        sourceType: XpSourceType,
        sourceId: String,
    ): ClassXpAward

    /** Award a class‑unique proficiency exactly once for the source. */
    suspend fun awardProficiency(
        userId: String,
        proficiencyKey: String,
        amount: Long,
        sourceType: XpSourceType,
        sourceId: String,
    )

    suspend fun classLevel(
        userId: String,
        classId: String,
    ): Int

    suspend fun totalClassXp(
        userId: String,
        classId: String,
    ): Long

    suspend fun totalProficiency(
        userId: String,
        proficiencyKey: String,
    ): Long
}
