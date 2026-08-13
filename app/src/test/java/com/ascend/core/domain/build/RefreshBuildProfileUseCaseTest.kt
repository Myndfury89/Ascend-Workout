package com.ascend.core.domain.build

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RefreshBuildProfileUseCaseTest {
    private val now = 1_700_000_000_000L
    private val dayMs = 86_400_000L

    private fun daysAgo(d: Int): Long = now - d.toLong() * dayMs

    private class FakeProvider(private val evidence: BuildEvidence) : BuildEvidenceProvider {
        override suspend fun gather(userId: String): BuildEvidence = evidence
    }

    private class RecordingRepository : BuildProfileRepository {
        var saved: BuildProfileSnapshot? = null

        override fun observe(userId: String): Flow<BuildProfileSnapshot?> = flowOf(saved)

        override suspend fun latest(userId: String): BuildProfileSnapshot? = saved

        override suspend fun save(
            userId: String,
            snapshot: BuildProfileSnapshot,
        ) {
            saved = snapshot
        }
    }

    @Test
    fun `refresh gathers evidence, computes, and persists a snapshot`() =
        runTest {
            val evidence =
                BuildEvidence(
                    sessions = (1..6).map { SessionEvidence(daysAgo(it), 40 * 60) },
                    families = (1..6).map { FamilyEvidence(daysAgo(it), ActivityFamily.RUN_WALK, 40 * 60) },
                    distances = (1..6).map { DistanceEvidence(daysAgo(it), 5_000.0, ActivityModality.RUN) },
                )
            val repo = RecordingRepository()
            val useCase =
                RefreshBuildProfileUseCase(
                    evidenceProvider = FakeProvider(evidence),
                    engine = BuildCharacteristicEngine(),
                    affinityCalculator = BuildAffinityCalculator(),
                    repository = repo,
                )

            val result = useCase.refresh("u1", now)

            assertNotNull(repo.saved)
            assertEquals(now, repo.saved!!.computedAt)
            assertEquals(7, result.affinities.ranked.size)
            assertEquals(EvidenceState.OK, result.profile[BuildCharacteristic.DISTANCE]!!.state)
        }
}
