package com.ascend.core.data.repository

import androidx.room.withTransaction
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.common.WeightUnit
import com.ascend.core.data.mapper.toDomain
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.PlayerDao
import com.ascend.core.database.entity.PlayerProgressEntity
import com.ascend.core.database.entity.PlayerStatsEntity
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl
    @Inject
    constructor(
        private val db: AscendDatabase,
        private val playerDao: PlayerDao,
        private val levelCalculator: LevelCalculator,
    ) : PlayerRepository {
        override suspend fun ensureLocalPlayer(displayName: String): String {
            db.withTransaction {
                if (playerDao.getProfile(LOCAL_USER_ID) == null) {
                    val now = System.currentTimeMillis()
                    playerDao.upsertProfile(
                        UserProfileEntity(
                            id = LOCAL_USER_ID,
                            displayName = displayName,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                    playerDao.upsertProgress(PlayerProgressEntity(userId = LOCAL_USER_ID, updatedAt = now))
                    playerDao.upsertStats(PlayerStatsEntity(userId = LOCAL_USER_ID, updatedAt = now))
                }
            }
            return LOCAL_USER_ID
        }

        override suspend fun displayName(userId: String): String =
            playerDao.getProfile(userId)?.displayName?.takeIf { it.isNotBlank() } ?: "Hunter"

        override fun observeProgress(userId: String): Flow<PlayerProgress?> =
            playerDao.observeProgress(userId).map { it?.toDomain(levelCalculator) }

        override fun observeStats(userId: String): Flow<PlayerStats?> = playerDao.observeStats(userId).map { it?.toDomain() }

        override suspend fun weightUnit(userId: String): WeightUnit = WeightUnit.fromNameOrDefault(playerDao.getProfile(userId)?.weightUnit)

        override fun observeWeightUnit(userId: String): Flow<WeightUnit> =
            playerDao.observeProfile(userId).map { WeightUnit.fromNameOrDefault(it?.weightUnit) }

        override suspend fun setWeightUnit(
            userId: String,
            unit: WeightUnit,
        ) {
            // Persist the display preference only — no stored weight is ever rewritten.
            playerDao.updateWeightUnit(userId, unit.name, System.currentTimeMillis())
        }

        override fun observeAvatarBodyBase(userId: String): Flow<String?> = playerDao.observeAvatarBodyBase(userId)

        override suspend fun setAvatarBodyBase(
            userId: String,
            value: String?,
        ) {
            // Cosmetic presentation preference only — never touches progression/physiology.
            playerDao.updateAvatarBodyBase(userId, value, System.currentTimeMillis())
        }
    }
