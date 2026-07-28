package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.ExercisePrescriptionEntity
import com.ascend.core.database.entity.ProgressionMilestoneEntity
import com.ascend.core.database.entity.ProgressionRecommendationEntity
import com.ascend.core.database.entity.TrainingReadinessSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdaptiveTrainingDao {
    // ---- Prescriptions ----
    @Upsert
    suspend fun upsertPrescription(entity: ExercisePrescriptionEntity)

    @Query("SELECT * FROM exercise_prescription WHERE id = :id")
    suspend fun getPrescription(id: String): ExercisePrescriptionEntity?

    @Query(
        "SELECT * FROM exercise_prescription WHERE userId = :userId AND exerciseId = :exerciseId AND status = 'ACTIVE' " +
            "ORDER BY effectiveFrom DESC LIMIT 1",
    )
    suspend fun getActivePrescription(
        userId: String,
        exerciseId: String,
    ): ExercisePrescriptionEntity?

    @Query("UPDATE exercise_prescription SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePrescriptionStatus(
        id: String,
        status: String,
        updatedAt: Long,
    )

    // ---- Readiness snapshots ----
    @Insert
    suspend fun insertSnapshot(entity: TrainingReadinessSnapshotEntity)

    @Query("SELECT * FROM training_readiness_snapshot WHERE id = :id")
    suspend fun getSnapshot(id: String): TrainingReadinessSnapshotEntity?

    // ---- Recommendations ----
    @Upsert
    suspend fun upsertRecommendation(entity: ProgressionRecommendationEntity)

    @Query("SELECT * FROM progression_recommendation WHERE id = :id")
    suspend fun getRecommendation(id: String): ProgressionRecommendationEntity?

    @Query("SELECT * FROM progression_recommendation WHERE userId = :userId AND status = 'PENDING' ORDER BY generatedAt DESC")
    fun observePending(userId: String): Flow<List<ProgressionRecommendationEntity>>

    @Query("SELECT * FROM progression_recommendation WHERE userId = :userId AND status = 'PENDING' ORDER BY generatedAt DESC")
    suspend fun getPending(userId: String): List<ProgressionRecommendationEntity>

    @Query(
        "UPDATE progression_recommendation SET status = :status, acceptedAt = :acceptedAt, rejectedAt = :rejectedAt, " +
            "appliedAt = :appliedAt WHERE id = :id",
    )
    suspend fun updateRecommendationStatus(
        id: String,
        status: String,
        acceptedAt: Long?,
        rejectedAt: Long?,
        appliedAt: Long?,
    )

    // ---- Milestones (idempotent reward source) ----

    /** Returns row id, or -1 when the milestoneKey guard blocks a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMilestone(entity: ProgressionMilestoneEntity): Long

    @Query("SELECT * FROM progression_milestone WHERE milestoneKey = :key")
    suspend fun getMilestoneByKey(key: String): ProgressionMilestoneEntity?
}
