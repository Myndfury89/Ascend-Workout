package com.ascend.core.domain.build

import kotlinx.coroutines.flow.Flow

/** A cached, read-only build snapshot: the resolved profile and the affinity result, plus when it was computed. */
data class BuildProfileSnapshot(
    val computedAt: Long,
    val profile: BuildProfile,
    val affinities: BuildAffinityResult,
)

/**
 * Read + cache for the Build snapshot. This layer only ever reads training evidence and writes its
 * own snapshot row — it never touches XP, attributes, skills, quests, or the progression queue.
 */
interface BuildProfileRepository {
    fun observe(userId: String): Flow<BuildProfileSnapshot?>

    suspend fun latest(userId: String): BuildProfileSnapshot?

    suspend fun save(
        userId: String,
        snapshot: BuildProfileSnapshot,
    )
}

/**
 * Supplies verified evidence for a user, already gathered from whatever sources exist. Keeping this an
 * interface is what makes the engine source-agnostic: the v1 implementation reads Ascend-native
 * tables; a Health Connect provider can be added later without changing the engine or calculator.
 */
interface BuildEvidenceProvider {
    suspend fun gather(userId: String): BuildEvidence
}
