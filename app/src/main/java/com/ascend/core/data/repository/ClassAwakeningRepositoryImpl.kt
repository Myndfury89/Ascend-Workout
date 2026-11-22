package com.ascend.core.data.repository

import com.ascend.core.common.BuildFlags
import com.ascend.core.database.dao.ClassAwakeningDao
import com.ascend.core.database.dao.ClassDao
import com.ascend.core.database.entity.ClassAwakeningEntity
import com.ascend.core.domain.classes.AwakeningSource
import com.ascend.core.domain.classes.ClassAwakeningRepository
import com.ascend.core.domain.classes.ClassCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassAwakeningRepositoryImpl
    @Inject
    constructor(
        private val awakeningDao: ClassAwakeningDao,
        private val classDao: ClassDao,
        private val buildFlags: BuildFlags,
    ) : ClassAwakeningRepository {
        override suspend fun awaken(
            userId: String,
            classId: String,
            source: AwakeningSource,
            evidenceJson: String?,
            awakenedAt: Long,
        ): Boolean {
            if (source == AwakeningSource.DEBUG_OVERRIDE && !buildFlags.isDebug) {
                return false
            }
            val inserted =
                awakeningDao.insertOrIgnore(
                    ClassAwakeningEntity(
                        userId = userId,
                        classId = classId,
                        awakenedAt = awakenedAt,
                        source = source.name,
                        evidenceJson = evidenceJson,
                    ),
                )
            return inserted != IGNORED
        }

        override fun observe(userId: String): Flow<Set<String>> =
            combine(
                awakeningDao.observe(userId),
                classDao.observeStartsAwakeIds(),
                classDao.observeDefinitionCount(),
            ) { rows, starters, definitionCount ->
                awakeIds(definitionCount, starters, rows.map { it.classId })
            }

        override suspend fun ids(userId: String): Set<String> =
            awakeIds(
                definitionCount = classDao.countDefinitions(),
                startsAwakeIds = classDao.startsAwakeIds(),
                awakenedClassIds = awakeningDao.ids(userId),
            )

        override suspend fun isAwake(
            userId: String,
            classId: String,
        ): Boolean = classId in ids(userId)

        /**
         * Before any definition is seeded, the code catalog is the source of starters. Once rows
         * exist the table is authoritative, including a starter whose `startsAwake` flag was not set.
         */
        private fun awakeIds(
            definitionCount: Int,
            startsAwakeIds: List<String>,
            awakenedClassIds: List<String>,
        ): Set<String> {
            val starters =
                if (definitionCount == 0) {
                    ClassCatalog.ALL.filter { it.startsAwake }.map { it.id }
                } else {
                    startsAwakeIds
                }
            return (starters + awakenedClassIds).toSet()
        }

        private companion object {
            /** Room's insert-or-ignore result when the primary key is already present. */
            const val IGNORED = -1L
        }
    }
