package com.ascend.core.data.repository

import androidx.room.withTransaction
import com.ascend.core.common.newId
import com.ascend.core.data.mapper.toDomain
import com.ascend.core.data.mapper.toEntity
import com.ascend.core.data.mapper.toSelection
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.ClassDao
import com.ascend.core.database.entity.ClassHistoryEntity
import com.ascend.core.database.entity.ClassProficiencyTransactionEntity
import com.ascend.core.database.entity.ClassXpTransactionEntity
import com.ascend.core.database.entity.PlayerClassEntity
import com.ascend.core.domain.classes.ClassCatalog
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.repository.ClassRepository
import com.ascend.core.domain.repository.ClassXpAward
import com.ascend.core.model.ClassChangeSource
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassHistory
import com.ascend.core.model.ClassProgress
import com.ascend.core.model.ClassSlot
import com.ascend.core.model.PlayerClassSelection
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassRepositoryImpl
    @Inject
    constructor(
        private val db: AscendDatabase,
        private val classDao: ClassDao,
        private val levelCalculator: LevelCalculator,
    ) : ClassRepository {
        private fun now() = System.currentTimeMillis()

        override suspend fun seedDefinitions() {
            if (classDao.countDefinitions() > 0) return
            val ts = now()
            classDao.upsertDefinitions(ClassCatalog.ALL.map { it.toEntity(ts) })
        }

        override suspend fun definitions(): List<ClassDefinition> =
            classDao.getDefinitions().takeIf { it.isNotEmpty() }?.map { it.toDomain() } ?: ClassCatalog.ALL

        override fun observeDefinitions(): Flow<List<ClassDefinition>> =
            classDao.observeDefinitions().map { list -> list.takeIf { it.isNotEmpty() }?.map { it.toDomain() } ?: ClassCatalog.ALL }

        override suspend fun definition(classId: String?): ClassDefinition? {
            if (classId == null) return null
            // DB is authoritative once seeded; fall back to the code catalog otherwise.
            return classDao.getDefinition(classId)?.toDomain() ?: ClassCatalog.byId(classId)
        }

        override suspend fun getSelection(userId: String): PlayerClassSelection = classDao.getSelection(userId).toSelection(userId)

        override fun observeSelection(userId: String): Flow<PlayerClassSelection> =
            classDao.observeSelection(userId).map { it.toSelection(userId) }

        override suspend fun setClasses(
            userId: String,
            primaryClassId: String?,
            secondaryClassId: String?,
            selectionReason: String?,
            changeSource: ClassChangeSource,
        ) = db.withTransaction {
            val current = classDao.getSelection(userId)
            val ts = now()

            fun changed(
                old: String?,
                new: String?,
            ) = old != new

            // Ledgers are never touched here — earned Class XP / proficiency survive a switch.
            if (changed(current?.primaryClassId, primaryClassId)) {
                recordSwitch(userId, ClassSlot.PRIMARY, primaryClassId, selectionReason, changeSource, ts)
            }
            if (changed(current?.secondaryClassId, secondaryClassId)) {
                recordSwitch(userId, ClassSlot.SECONDARY, secondaryClassId, selectionReason, changeSource, ts)
            }

            classDao.upsertSelection(
                PlayerClassEntity(
                    userId = userId,
                    primaryClassId = primaryClassId,
                    secondaryClassId = secondaryClassId,
                    primaryStartedAt = if (changed(current?.primaryClassId, primaryClassId)) ts else current?.primaryStartedAt,
                    secondaryStartedAt = if (changed(current?.secondaryClassId, secondaryClassId)) ts else current?.secondaryStartedAt,
                    selectionReason = selectionReason,
                    changeSource = changeSource.name,
                    cooldownUntil = current?.cooldownUntil,
                    respecQuestId = current?.respecQuestId,
                    updatedAt = ts,
                ),
            )
        }

        private suspend fun recordSwitch(
            userId: String,
            slot: ClassSlot,
            newClassId: String?,
            reason: String?,
            source: ClassChangeSource,
            ts: Long,
        ) {
            classDao.closeOpenHistory(userId, slot.name, ts)
            if (newClassId != null) {
                classDao.insertHistory(
                    ClassHistoryEntity(
                        id = newId(),
                        userId = userId,
                        classId = newClassId,
                        slot = slot.name,
                        startedAt = ts,
                        selectionReason = reason,
                        changeSource = source.name,
                        createdAt = ts,
                    ),
                )
            }
        }

        override suspend fun getHistory(userId: String): List<ClassHistory> = classDao.getHistory(userId).map { it.toDomain() }

        override suspend fun awardClassXp(
            userId: String,
            classId: String,
            amount: Long,
            sourceType: XpSourceType,
            sourceId: String,
        ): ClassXpAward =
            db.withTransaction {
                val row =
                    classDao.insertClassXp(
                        ClassXpTransactionEntity(
                            id = newId(), userId = userId, classId = classId, amount = amount,
                            transactionType = "AWARD", sourceType = sourceType.name, sourceId = sourceId,
                            rewardType = REWARD_CLASS_XP, createdAt = now(),
                        ),
                    )
                val total = classDao.totalClassXp(userId, classId)
                val newLevel = levelCalculator.resolve(total).level
                if (row == -1L) {
                    ClassXpAward(classId, 0, newLevel, leveledUp = false, duplicate = true)
                } else {
                    val oldLevel = levelCalculator.resolve((total - amount).coerceAtLeast(0)).level
                    ClassXpAward(classId, amount, newLevel, leveledUp = newLevel > oldLevel, duplicate = false)
                }
            }

        override suspend fun awardProficiency(
            userId: String,
            proficiencyKey: String,
            amount: Long,
            sourceType: XpSourceType,
            sourceId: String,
        ) {
            if (amount == 0L) return
            classDao.insertProficiency(
                ClassProficiencyTransactionEntity(
                    id = newId(), userId = userId, proficiencyKey = proficiencyKey, amount = amount,
                    transactionType = "AWARD", sourceType = sourceType.name, sourceId = sourceId,
                    rewardType = REWARD_UNIQUE_PROFICIENCY, createdAt = now(),
                ),
            )
        }

        override suspend fun classLevel(
            userId: String,
            classId: String,
        ): Int = levelCalculator.resolve(classDao.totalClassXp(userId, classId)).level

        override suspend fun classProgress(
            userId: String,
            classId: String,
        ): ClassProgress {
            val xp = classDao.totalClassXp(userId, classId)
            val state = levelCalculator.resolve(xp)
            val def = definition(classId)
            val profKey = def?.uniqueProficiencyKey ?: ""
            return ClassProgress(
                classId = classId,
                classLevel = state.level,
                classXp = xp,
                currentLevelXp = state.currentLevelXp,
                xpToNextLevel = state.xpToNextLevel,
                uniqueProficiencyKey = profKey,
                uniqueProficiency = if (profKey.isEmpty()) 0 else classDao.totalProficiency(userId, profKey),
            )
        }

        override suspend fun totalClassXp(
            userId: String,
            classId: String,
        ): Long = classDao.totalClassXp(userId, classId)

        override suspend fun totalProficiency(
            userId: String,
            proficiencyKey: String,
        ): Long = classDao.totalProficiency(userId, proficiencyKey)

        private companion object {
            const val REWARD_CLASS_XP = "CLASS_XP"
            const val REWARD_UNIQUE_PROFICIENCY = "UNIQUE_PROFICIENCY"
        }
    }
