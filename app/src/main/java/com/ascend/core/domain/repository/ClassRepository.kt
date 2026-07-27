package com.ascend.core.domain.repository

import com.ascend.core.model.ClassChangeSource
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassHistory
import com.ascend.core.model.ClassProgress
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
 * Owns class definitions (DB‑seeded, updateable without touching progression), the
 * player's class selection + history, and the class‑XP / unique‑proficiency ledgers.
 * Awards are idempotent on (classId|proficiencyKey, transactionType, sourceType,
 * sourceId, rewardType). Switching classes never deletes earned Class XP/proficiency.
 */
interface ClassRepository {
    /** Idempotently seed the built‑in class definitions (safe to call repeatedly). */
    suspend fun seedDefinitions()

    suspend fun definitions(): List<ClassDefinition>

    fun observeDefinitions(): Flow<List<ClassDefinition>>

    suspend fun definition(classId: String?): ClassDefinition?

    suspend fun getSelection(userId: String): PlayerClassSelection

    fun observeSelection(userId: String): Flow<PlayerClassSelection>

    /** Set primary/optional‑secondary class; records history and preserves ledgers. */
    suspend fun setClasses(
        userId: String,
        primaryClassId: String?,
        secondaryClassId: String?,
        selectionReason: String? = null,
        changeSource: ClassChangeSource = ClassChangeSource.MANUAL,
    )

    suspend fun getHistory(userId: String): List<ClassHistory>

    suspend fun awardClassXp(
        userId: String,
        classId: String,
        amount: Long,
        sourceType: XpSourceType,
        sourceId: String,
    ): ClassXpAward

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

    suspend fun classProgress(
        userId: String,
        classId: String,
    ): ClassProgress

    suspend fun totalClassXp(
        userId: String,
        classId: String,
    ): Long

    suspend fun totalProficiency(
        userId: String,
        proficiencyKey: String,
    ): Long
}
