package com.ascend.core.domain.repository

import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ProgressionRecommendation
import kotlinx.coroutines.flow.Flow

/** Outcome of applying an accepted recommendation (idempotent). */
sealed interface ApplyRecommendationResult {
    data class Applied(
        val newPrescriptionId: String?,
        val newTarget: Int?,
    ) : ApplyRecommendationResult

    data object AlreadyApplied : ApplyRecommendationResult

    data object NotAccepted : ApplyRecommendationResult

    data object NotFound : ApplyRecommendationResult
}

/**
 * Persists progression recommendations and prescriptions, and drives the
 * accept/reject/apply lifecycle. Recommendations survive restarts; an accepted
 * recommendation is applied **exactly once**; rejecting changes nothing.
 */
interface ProgressionRecommendationRepository {
    suspend fun savePrescription(prescription: ExercisePrescription): String

    suspend fun activePrescription(
        userId: String,
        exerciseId: String,
    ): ExercisePrescription?

    suspend fun save(recommendation: ProgressionRecommendation): String

    suspend fun get(id: String): ProgressionRecommendation?

    fun observePending(userId: String): Flow<List<ProgressionRecommendation>>

    suspend fun getPending(userId: String): List<ProgressionRecommendation>

    suspend fun accept(id: String): Boolean

    suspend fun reject(id: String): Boolean

    suspend fun apply(id: String): ApplyRecommendationResult

    suspend fun markCompleted(
        id: String,
        successful: Boolean,
    ): Boolean

    suspend fun expireStale(
        userId: String,
        now: Long,
    ): Int
}
