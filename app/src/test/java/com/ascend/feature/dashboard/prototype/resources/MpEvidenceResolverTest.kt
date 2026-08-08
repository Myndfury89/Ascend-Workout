package com.ascend.feature.dashboard.prototype.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** MP evidence resolution: phone-only support, source priority, and conservative dedup. */
class MpEvidenceResolverTest {
    private val minute = 60_000L
    private val train = DailyMpTarget(45, DailyMpTargetType.TRAIN, "Train", isRestDay = false)
    private val rest = DailyMpTarget(0, DailyMpTargetType.REST, "Rest prescribed", isRestDay = true)

    private fun ev(
        source: TrainingDurationSource,
        minutes: Int,
        startMin: Long = 0,
        stableSessionId: String? = null,
        externalRecordId: String? = null,
        sourceApplication: String? = null,
        classification: WorkoutKind = WorkoutKind.UNKNOWN,
    ) = TrainingDurationEvidence(
        source,
        minutes,
        startMin * minute,
        (startMin + minutes) * minute,
        stableSessionId,
        externalRecordId,
        sourceApplication,
        classification,
    )

    @Test
    fun `MP works without Health Connect`() {
        val mp = MpEvidenceResolver.resolve(listOf(ev(TrainingDurationSource.ASCEND_WORKOUT, 40)), train)
        assertEquals(ResourceAvailability.AVAILABLE, mp.availability)
        assertEquals(40, mp.verifiedMinutes)
        assertEquals(listOf(TrainingDurationSource.ASCEND_WORKOUT), mp.includedSources)
    }

    @Test
    fun `ascend workout and cardio both contribute`() {
        val mp =
            MpEvidenceResolver.resolve(
                listOf(ev(TrainingDurationSource.ASCEND_WORKOUT, 30), ev(TrainingDurationSource.ASCEND_CARDIO, 20, startMin = 200)),
                train,
            )
        assertEquals(50, mp.verifiedMinutes)
        assertEquals(0, mp.suppressedDuplicateCount)
    }

    @Test
    fun `a duplicate shared session id is counted once, keeping the higher-priority source`() {
        val mp =
            MpEvidenceResolver.resolve(
                listOf(
                    ev(TrainingDurationSource.HEALTH_CONNECT, 45, stableSessionId = "s1"),
                    ev(TrainingDurationSource.ASCEND_WORKOUT, 45, stableSessionId = "s1"),
                ),
                train,
            )
        assertEquals(45, mp.verifiedMinutes)
        assertEquals(1, mp.suppressedDuplicateCount)
        assertEquals(listOf(TrainingDurationSource.ASCEND_WORKOUT), mp.includedSources)
    }

    @Test
    fun `a duplicate shared sourceApplication and externalRecordId is counted once`() {
        val mp =
            MpEvidenceResolver.resolve(
                listOf(
                    ev(TrainingDurationSource.IMPORTED_ACTIVITY, 40, externalRecordId = "r1", sourceApplication = "com.x"),
                    ev(TrainingDurationSource.HEALTH_CONNECT, 40, externalRecordId = "r1", sourceApplication = "com.x"),
                ),
                train,
            )
        assertEquals(40, mp.verifiedMinutes)
        assertEquals(1, mp.suppressedDuplicateCount)
    }

    @Test
    fun `two distinct same-source workouts are never collapsed`() {
        // Same source + overlapping time must NOT merge — they are two real sessions.
        val mp =
            MpEvidenceResolver.resolve(
                listOf(
                    ev(TrainingDurationSource.ASCEND_WORKOUT, 30, startMin = 0),
                    ev(TrainingDurationSource.ASCEND_WORKOUT, 30, startMin = 0),
                ),
                train,
            )
        assertEquals(60, mp.verifiedMinutes)
        assertEquals(0, mp.suppressedDuplicateCount)
    }

    @Test
    fun `cross-source near-identical overlapping sessions collapse to one`() {
        val mp =
            MpEvidenceResolver.resolve(
                listOf(
                    ev(TrainingDurationSource.ASCEND_WORKOUT, 45, startMin = 0, classification = WorkoutKind.STRENGTH),
                    ev(TrainingDurationSource.HEALTH_CONNECT, 44, startMin = 1, classification = WorkoutKind.STRENGTH),
                ),
                train,
            )
        assertEquals(45, mp.verifiedMinutes)
        assertEquals(1, mp.suppressedDuplicateCount)
        assertEquals(listOf(TrainingDurationSource.ASCEND_WORKOUT), mp.includedSources)
    }

    @Test
    fun `cross-source non-overlapping sessions both count`() {
        val mp =
            MpEvidenceResolver.resolve(
                listOf(
                    ev(TrainingDurationSource.ASCEND_WORKOUT, 30, startMin = 0),
                    ev(TrainingDurationSource.HEALTH_CONNECT, 30, startMin = 120),
                ),
                train,
            )
        assertEquals(60, mp.verifiedMinutes)
        assertEquals(0, mp.suppressedDuplicateCount)
    }

    @Test
    fun `a rest-day target renders a recovery state, not zero over zero`() {
        val mp = MpEvidenceResolver.resolve(emptyList(), rest)
        assertEquals(ResourceAvailability.REST_DAY, mp.availability)
        assertEquals(0, mp.verifiedMinutes)
        assertNull(mp.progressFraction)
    }

    @Test
    fun `no evidence on a training day is available zero, not unavailable`() {
        val mp = MpEvidenceResolver.resolve(emptyList(), train)
        assertEquals(ResourceAvailability.AVAILABLE, mp.availability)
        assertEquals(0, mp.verifiedMinutes)
        assertEquals(0f, mp.progressFraction!!, 0.0001f)
    }
}
