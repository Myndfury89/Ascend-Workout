package com.ascend.core.data.repository

import androidx.room.withTransaction
import com.ascend.core.data.mapper.toDomain
import com.ascend.core.data.mapper.toEntity
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.AdaptiveTrainingDao
import com.ascend.core.domain.repository.ApplyRecommendationResult
import com.ascend.core.domain.repository.ProgressionRecommendationRepository
import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ProgressionRecommendation
import com.ascend.core.model.RecommendationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressionRecommendationRepositoryImpl
    @Inject
    constructor(
        private val db: AscendDatabase,
        private val dao: AdaptiveTrainingDao,
    ) : ProgressionRecommendationRepository {
        private fun now() = System.currentTimeMillis()

        override suspend fun savePrescription(prescription: ExercisePrescription): String {
            dao.upsertPrescription(prescription.toEntity(now()))
            return prescription.id
        }

        override suspend fun activePrescription(
            userId: String,
            exerciseId: String,
        ): ExercisePrescription? = dao.getActivePrescription(userId, exerciseId)?.toDomain()

        override suspend fun save(recommendation: ProgressionRecommendation): String =
            db.withTransaction {
                recommendation.proposed?.let { dao.upsertPrescription(it.copy(status = "PROPOSED").toEntity(now())) }
                dao.upsertRecommendation(recommendation.toEntity())
                recommendation.id
            }

        override suspend fun get(id: String): ProgressionRecommendation? {
            val entity = dao.getRecommendation(id) ?: return null
            val proposed = entity.proposedPrescriptionId?.let { dao.getPrescription(it)?.toDomain() }
            return entity.toDomain(proposed)
        }

        override fun observePending(userId: String): Flow<List<ProgressionRecommendation>> =
            dao.observePending(userId).map { list -> list.map { it.toDomain(null) } }

        override suspend fun getPending(userId: String): List<ProgressionRecommendation> = dao.getPending(userId).map { it.toDomain(null) }

        override suspend fun accept(id: String): Boolean {
            val entity = dao.getRecommendation(id) ?: return false
            if (entity.status != RecommendationStatus.PENDING.name) return false
            dao.updateRecommendationStatus(id, RecommendationStatus.ACCEPTED.name, now(), null, entity.appliedAt)
            return true
        }

        override suspend fun reject(id: String): Boolean {
            val entity = dao.getRecommendation(id) ?: return false
            if (entity.status != RecommendationStatus.PENDING.name) return false
            // Rejecting changes nothing else — no XP/level/attribute/streak impact.
            dao.updateRecommendationStatus(id, RecommendationStatus.REJECTED.name, null, now(), null)
            return true
        }

        override suspend fun apply(id: String): ApplyRecommendationResult =
            db.withTransaction {
                val entity = dao.getRecommendation(id) ?: return@withTransaction ApplyRecommendationResult.NotFound
                when (entity.status) {
                    RecommendationStatus.APPLIED.name,
                    RecommendationStatus.COMPLETED_SUCCESSFULLY.name,
                    RecommendationStatus.COMPLETED_UNSUCCESSFULLY.name,
                    -> return@withTransaction ApplyRecommendationResult.AlreadyApplied
                    RecommendationStatus.ACCEPTED.name -> Unit
                    else -> return@withTransaction ApplyRecommendationResult.NotAccepted
                }

                val ts = now()
                // Resistance: activate the proposed prescription, supersede the current one.
                entity.proposedPrescriptionId?.let { proposedId ->
                    dao.updatePrescriptionStatus(proposedId, "ACTIVE", ts)
                    entity.currentPrescriptionId?.let { dao.updatePrescriptionStatus(it, "SUPERSEDED", ts) }
                }
                dao.updateRecommendationStatus(id, RecommendationStatus.APPLIED.name, entity.acceptedAt, null, ts)
                ApplyRecommendationResult.Applied(entity.proposedPrescriptionId, entity.proposedTarget)
            }

        override suspend fun markCompleted(
            id: String,
            successful: Boolean,
        ): Boolean {
            val entity = dao.getRecommendation(id) ?: return false
            val status = if (successful) RecommendationStatus.COMPLETED_SUCCESSFULLY else RecommendationStatus.COMPLETED_UNSUCCESSFULLY
            dao.updateRecommendationStatus(id, status.name, entity.acceptedAt, entity.rejectedAt, entity.appliedAt)
            return true
        }

        override suspend fun expireStale(
            userId: String,
            now: Long,
        ): Int {
            var count = 0
            dao.getPending(userId).filter { it.expiresAt < now }.forEach {
                dao.updateRecommendationStatus(it.id, RecommendationStatus.EXPIRED.name, null, null, null)
                count++
            }
            return count
        }
    }
