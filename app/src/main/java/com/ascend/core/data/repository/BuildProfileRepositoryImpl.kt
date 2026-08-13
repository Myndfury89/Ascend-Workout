package com.ascend.core.data.repository

import com.ascend.core.database.dao.BuildProfileDao
import com.ascend.core.database.entity.BuildProfileSnapshotEntity
import com.ascend.core.domain.build.BuildAffinityResult
import com.ascend.core.domain.build.BuildCharacteristic
import com.ascend.core.domain.build.BuildClass
import com.ascend.core.domain.build.BuildProfile
import com.ascend.core.domain.build.BuildProfileRepository
import com.ascend.core.domain.build.BuildProfileSnapshot
import com.ascend.core.domain.build.BuildTrend
import com.ascend.core.domain.build.CharacteristicScore
import com.ascend.core.domain.build.ClassAffinity
import com.ascend.core.domain.build.EvidenceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Room-backed cache for the read-only Build snapshot. The rich profile + affinity result are stored as
 * a JSON payload via data-layer DTOs, keeping the domain models free of serialization annotations.
 *
 * This repository only reads its own snapshot row and writes its own snapshot row — it holds no
 * progression dependency and can never award or remove progression.
 */
class BuildProfileRepositoryImpl
    @Inject
    constructor(
        private val dao: BuildProfileDao,
    ) : BuildProfileRepository {
        private val json = Json { ignoreUnknownKeys = true }

        override fun observe(userId: String): Flow<BuildProfileSnapshot?> = dao.observe(userId).map { it?.toDomain() }

        override suspend fun latest(userId: String): BuildProfileSnapshot? = dao.get(userId)?.toDomain()

        override suspend fun save(
            userId: String,
            snapshot: BuildProfileSnapshot,
        ) {
            dao.upsert(snapshot.toEntity(userId))
        }

        private fun BuildProfileSnapshot.toEntity(userId: String): BuildProfileSnapshotEntity {
            val payload =
                PayloadDto(
                    characteristics =
                        profile.scores.values.map {
                            CharacteristicDto(it.characteristic.name, it.score, it.state.name, it.trend.name, it.confidence)
                        },
                    ranked =
                        affinities.ranked.map {
                            ClassAffinityDto(it.buildClass.id, it.affinity, it.coverage, it.confidence, it.dominantEligible)
                        },
                    dominantClassId = affinities.dominant?.buildClass?.id,
                )
            return BuildProfileSnapshotEntity(
                userId = userId,
                computedAt = computedAt,
                windowDays = profile.windowDays,
                overallConfidence = profile.overallConfidence,
                payloadJson = json.encodeToString(payload),
            )
        }

        private fun BuildProfileSnapshotEntity.toDomain(): BuildProfileSnapshot {
            val payload = json.decodeFromString<PayloadDto>(payloadJson)
            val scores =
                payload.characteristics.associate { dto ->
                    val characteristic = BuildCharacteristic.valueOf(dto.characteristic)
                    characteristic to
                        CharacteristicScore(
                            characteristic = characteristic,
                            score = dto.score,
                            state = EvidenceState.valueOf(dto.state),
                            trend = BuildTrend.valueOf(dto.trend),
                            confidence = dto.confidence,
                        )
                }
            val ranked =
                payload.ranked.mapNotNull { dto ->
                    BuildClass.fromId(dto.classId)?.let {
                        ClassAffinity(it, dto.affinity, dto.coverage, dto.confidence, dto.dominantEligible)
                    }
                }
            return BuildProfileSnapshot(
                computedAt = computedAt,
                profile = BuildProfile(scores = scores, overallConfidence = overallConfidence, windowDays = windowDays),
                affinities =
                    BuildAffinityResult(
                        ranked = ranked,
                        dominant = ranked.firstOrNull { it.buildClass.id == payload.dominantClassId },
                    ),
            )
        }

        @Serializable
        private data class PayloadDto(
            val characteristics: List<CharacteristicDto>,
            val ranked: List<ClassAffinityDto>,
            val dominantClassId: String?,
        )

        @Serializable
        private data class CharacteristicDto(
            val characteristic: String,
            val score: Double,
            val state: String,
            val trend: String,
            val confidence: Double,
        )

        @Serializable
        private data class ClassAffinityDto(
            val classId: String,
            val affinity: Double,
            val coverage: Double,
            val confidence: Double,
            val dominantEligible: Boolean,
        )
    }
