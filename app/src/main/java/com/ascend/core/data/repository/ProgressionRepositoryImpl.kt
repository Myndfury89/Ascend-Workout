package com.ascend.core.data.repository

import androidx.room.withTransaction
import com.ascend.core.common.newId
import com.ascend.core.data.mapper.toDomain
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.AttributeDao
import com.ascend.core.database.dao.PlayerDao
import com.ascend.core.database.dao.XpDao
import com.ascend.core.database.entity.AttributeTransactionEntity
import com.ascend.core.database.entity.PlayerProgressEntity
import com.ascend.core.database.entity.PlayerStatsEntity
import com.ascend.core.database.entity.XpTransactionEntity
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.repository.ProgressionRepository
import com.ascend.core.domain.repository.XpAwardResult
import com.ascend.core.model.AttributeType
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressionRepositoryImpl
    @Inject
    constructor(
        private val db: AscendDatabase,
        private val playerDao: PlayerDao,
        private val xpDao: XpDao,
        private val attributeDao: AttributeDao,
        private val levelCalculator: LevelCalculator,
        private val rankCalculator: RankCalculator,
    ) : ProgressionRepository {
        private fun now() = System.currentTimeMillis()

        override fun observeProgress(userId: String): Flow<PlayerProgress?> =
            playerDao.observeProgress(userId).map { it?.toDomain(levelCalculator) }

        override fun observeStats(userId: String): Flow<PlayerStats?> = playerDao.observeStats(userId).map { it?.toDomain() }

        override suspend fun getProgress(userId: String): PlayerProgress? = playerDao.getProgress(userId)?.toDomain(levelCalculator)

        override suspend fun getStats(userId: String): PlayerStats? = playerDao.getStats(userId)?.toDomain()

        override suspend fun awardXp(
            userId: String,
            amount: Long,
            sourceType: XpSourceType,
            sourceId: String,
            description: String,
        ): XpAwardResult =
            db.withTransaction {
                val row =
                    xpDao.insertIgnoringDuplicates(
                        XpTransactionEntity(
                            id = newId(),
                            userId = userId,
                            amount = amount,
                            transactionType = "AWARD",
                            sourceType = sourceType.name,
                            sourceId = sourceId,
                            description = description,
                            createdAt = now(),
                        ),
                    )
                if (row == -1L) return@withTransaction XpAwardResult.Duplicate
                val before = playerDao.getProgress(userId)
                val updated = recomputeProgress(userId, before)
                XpAwardResult.Awarded(
                    amount = amount,
                    newLevel = updated.level,
                    leveledUp = updated.level > (before?.level ?: 1),
                )
            }

        override suspend fun reverseXp(
            userId: String,
            sourceType: XpSourceType,
            sourceId: String,
        ): XpAwardResult =
            db.withTransaction {
                val original =
                    xpDao.find("AWARD", sourceType.name, sourceId)
                        ?: return@withTransaction XpAwardResult.Duplicate
                val row =
                    xpDao.insertIgnoringDuplicates(
                        XpTransactionEntity(
                            id = newId(), userId = userId, amount = -original.amount, transactionType = "REVERSAL",
                            sourceType = sourceType.name, sourceId = sourceId,
                            description = "Reversal of ${original.id}", reversedTransactionId = original.id,
                            createdAt = now(),
                        ),
                    )
                if (row == -1L) return@withTransaction XpAwardResult.Duplicate
                val before = playerDao.getProgress(userId)
                val updated = recomputeProgress(userId, before)
                XpAwardResult.Awarded(amount = -original.amount, newLevel = updated.level, leveledUp = false)
            }

        override suspend fun awardAttributes(
            userId: String,
            deltas: Map<AttributeType, Long>,
            sourceType: XpSourceType,
            sourceId: String,
        ) = db.withTransaction {
            deltas.forEach { (attribute, amount) ->
                if (amount != 0L) {
                    attributeDao.insertIgnoringDuplicates(
                        AttributeTransactionEntity(
                            id = newId(),
                            userId = userId,
                            attributeType = attribute.name,
                            amount = amount,
                            transactionType = "AWARD",
                            sourceType = sourceType.name,
                            sourceId = sourceId,
                            createdAt = now(),
                        ),
                    )
                }
            }
            recomputeStats(userId)
        }

        /** Recompute level/rank from the immutable XP ledger. Single source of truth. */
        private suspend fun recomputeProgress(
            userId: String,
            before: PlayerProgressEntity?,
        ): PlayerProgressEntity {
            val lifetimeXp = xpDao.totalXp(userId)
            val state = levelCalculator.resolve(lifetimeXp)
            val streak = before?.activeStreak ?: 0
            val rank = rankCalculator.rankFor(state.level, lifetimeXp, streak)
            val entity =
                (before ?: PlayerProgressEntity(userId = userId, updatedAt = now())).copy(
                    level = state.level,
                    currentLevelXp = state.currentLevelXp,
                    lifetimeXp = lifetimeXp,
                    rank = rank.name,
                    updatedAt = now(),
                )
            playerDao.upsertProgress(entity)
            return entity
        }

        private suspend fun recomputeStats(userId: String) {
            val current = playerDao.getStats(userId)

            suspend fun total(attribute: AttributeType) = attributeDao.totalForAttribute(userId, attribute.name)
            val entity =
                (current ?: PlayerStatsEntity(userId = userId, updatedAt = now())).copy(
                    strength = total(AttributeType.STRENGTH),
                    endurance = total(AttributeType.ENDURANCE),
                    agility = total(AttributeType.AGILITY),
                    discipline = total(AttributeType.DISCIPLINE),
                    recovery = total(AttributeType.RECOVERY),
                    updatedAt = now(),
                )
            playerDao.upsertStats(entity)
        }
    }
