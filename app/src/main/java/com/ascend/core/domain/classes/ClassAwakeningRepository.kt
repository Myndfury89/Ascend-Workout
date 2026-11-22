package com.ascend.core.domain.classes

import kotlinx.coroutines.flow.Flow

/**
 * Why a class awakened. Persisted on the awakening row. A later call with a different source does
 * not overwrite the original, and nothing revokes the row.
 */
enum class AwakeningSource {
    BUILD_AFFINITY,
    ONBOARDING_RECOMMENDATION,
    LEGACY_SELECTION,
    DEBUG_OVERRIDE,
}

/**
 * Per-user permanent class awakening. Classes whose definition has [com.ascend.core.model.ClassDefinition.startsAwake]
 * are awake with no row. [awaken] is insert-or-ignore. [AwakeningSource.DEBUG_OVERRIDE] writes
 * nothing unless the injected debug flag is true.
 */
interface ClassAwakeningRepository {
    /**
     * Records that [classId] is awake for [userId]. Returns true when a new row was stored.
     * A repeat call keeps the original row and returns false.
     */
    suspend fun awaken(
        userId: String,
        classId: String,
        source: AwakeningSource,
        evidenceJson: String? = null,
        awakenedAt: Long = System.currentTimeMillis(),
    ): Boolean

    /** Awake class ids: stored awakenings plus definitions flagged `startsAwake`. */
    fun observe(userId: String): Flow<Set<String>>

    suspend fun ids(userId: String): Set<String>

    suspend fun isAwake(
        userId: String,
        classId: String,
    ): Boolean
}
