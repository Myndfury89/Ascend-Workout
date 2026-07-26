package com.ascend.core.data.repository

import androidx.room.withTransaction
import com.ascend.core.common.newId
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.ClassDao
import com.ascend.core.database.entity.ClassProficiencyTransactionEntity
import com.ascend.core.database.entity.ClassXpTransactionEntity
import com.ascend.core.database.entity.PlayerClassEntity
import com.ascend.core.domain.classes.ClassCatalog
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.repository.ClassRepository
import com.ascend.core.domain.repository.ClassXpAward
import com.ascend.core.model.ClassDefinition
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

        override fun definitions(): List<ClassDefinition> = ClassCatalog.ALL

        override fun definition(classId: String?): ClassDefinition? = ClassCatalog.byId(classId)

        override suspend fun getSelection(userId: String): PlayerClassSelection = classDao.getSelection(userId).toSelection(userId)

        override fun observeSelection(userId: String): Flow<PlayerClassSelection> =
            classDao.observeSelection(userId).map { it.toSelection(userId) }

        override suspend fun setClasses(
            userId: String,
            primaryClassId: String?,
            secondaryClassId: String?,
        ) {
            classDao.upsertSelection(
                PlayerClassEntity(
                    userId = userId,
                    primaryClassId = primaryClassId,
                    secondaryClassId = secondaryClassId,
                    updatedAt = now(),
                ),
            )
        }

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
                            id = newId(),
                            userId = userId,
                            classId = classId,
                            amount = amount,
                            transactionType = "AWARD",
                            sourceType = sourceType.name,
                            sourceId = sourceId,
                            createdAt = now(),
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
                    id = newId(),
                    userId = userId,
                    proficiencyKey = proficiencyKey,
                    amount = amount,
                    transactionType = "AWARD",
                    sourceType = sourceType.name,
                    sourceId = sourceId,
                    createdAt = now(),
                ),
            )
        }

        override suspend fun classLevel(
            userId: String,
            classId: String,
        ): Int = levelCalculator.resolve(classDao.totalClassXp(userId, classId)).level

        override suspend fun totalClassXp(
            userId: String,
            classId: String,
        ): Long = classDao.totalClassXp(userId, classId)

        override suspend fun totalProficiency(
            userId: String,
            proficiencyKey: String,
        ): Long = classDao.totalProficiency(userId, proficiencyKey)

        private fun PlayerClassEntity?.toSelection(userId: String): PlayerClassSelection =
            PlayerClassSelection(userId, this?.primaryClassId, this?.secondaryClassId)
    }
