package com.ascend.core.domain.build

import javax.inject.Inject

/**
 * Recomputes and caches a user's Build snapshot: gather evidence -> resolve characteristics -> score
 * affinity -> persist. Read-only end to end; it awards nothing. Intended to be invoked on a
 * recompute-on-write moment (workout/cardio completed, health sync), not continuously.
 *
 * The concrete [BuildEvidenceProvider] that reads real Ascend tables is wired in the next phase; this
 * use case is deliberately independent of it so it can be tested with a fake provider today.
 */
class RefreshBuildProfileUseCase
    @Inject
    constructor(
        private val evidenceProvider: BuildEvidenceProvider,
        private val engine: BuildCharacteristicEngine,
        private val affinityCalculator: BuildAffinityCalculator,
        private val repository: BuildProfileRepository,
    ) {
        suspend fun refresh(
            userId: String,
            now: Long,
            tuning: BuildTuning = BuildTuning(),
        ): BuildProfileSnapshot {
            val evidence = evidenceProvider.gather(userId)
            val profile = engine.resolve(evidence, now, tuning)
            val affinities = affinityCalculator.calculate(profile, tuning)
            val snapshot = BuildProfileSnapshot(computedAt = now, profile = profile, affinities = affinities)
            repository.save(userId, snapshot)
            return snapshot
        }
    }
