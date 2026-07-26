package com.ascend.core.data.repository

import androidx.room.withTransaction
import com.ascend.core.data.mapper.toDomain
import com.ascend.core.data.mapper.toEntity
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.ProgressionEventDao
import com.ascend.core.domain.repository.ProgressionEventRepository
import com.ascend.core.model.ProgressionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressionEventRepositoryImpl
    @Inject
    constructor(
        private val db: AscendDatabase,
        private val eventDao: ProgressionEventDao,
    ) : ProgressionEventRepository {
        override suspend fun enqueue(events: List<ProgressionEvent>) {
            if (events.isEmpty()) return
            db.withTransaction {
                events.forEach { eventDao.insertIgnoringDuplicates(it.toEntity()) }
            }
        }

        override fun observePending(userId: String): Flow<List<ProgressionEvent>> =
            eventDao.observePending(userId).map { list -> list.map { it.toDomain() } }

        override suspend fun getPending(userId: String): List<ProgressionEvent> = eventDao.getPending(userId).map { it.toDomain() }

        override suspend fun markConsumed(eventIds: List<String>) {
            if (eventIds.isEmpty()) return
            eventDao.markConsumed(eventIds, System.currentTimeMillis())
        }
    }
