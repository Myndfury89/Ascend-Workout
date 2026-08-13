package com.ascend.core.domain.build

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Headless scenario tests for the read-only Build engine. Pure Kotlin — no DB, no Android — so each
 * persona is just a hand-built [BuildEvidence] bundle over a fixed `now`.
 */
class BuildCharacteristicEngineTest {
    private val engine = BuildCharacteristicEngine()
    private val now = 1_700_000_000_000L
    private val dayMs = 86_400_000L

    private fun daysAgo(d: Int): Long = now - d.toLong() * dayMs

    private fun state(
        profile: BuildProfile,
        c: BuildCharacteristic,
    ): EvidenceState = profile[c]!!.state

    // ---------- structural ----------

    @Test
    fun `activity is excluded from class signatures, the other six are not`() {
        assertTrue(!BuildCharacteristic.ACTIVITY.inClassSignature)
        BuildCharacteristic.entries.filter { it != BuildCharacteristic.ACTIVITY }.forEach {
            assertTrue("$it should be in class signatures", it.inClassSignature)
        }
    }

    @Test
    fun `empty evidence yields zero activity, unavailable recovery, and insufficient sparse signals`() {
        val p = engine.resolve(BuildEvidence.EMPTY, now)
        assertEquals(EvidenceState.ZERO, state(p, BuildCharacteristic.ACTIVITY))
        assertEquals(EvidenceState.UNAVAILABLE, state(p, BuildCharacteristic.RECOVERY))
        // Not active enough to call an absence a confident zero -> uncertain.
        assertEquals(EvidenceState.INSUFFICIENT, state(p, BuildCharacteristic.SPEED))
        assertEquals(EvidenceState.INSUFFICIENT, state(p, BuildCharacteristic.DISTANCE))
        assertEquals(0.0, p.overallConfidence, 0.0001)
    }

    // ---------- personas ----------

    private fun liftingEvidence(days: IntRange): BuildEvidence =
        BuildEvidence(
            sessions = days.map { SessionEvidence(daysAgo(it), durationSeconds = 45 * 60) },
            strengthSets =
                days.flatMap {
                    listOf(
                        StrengthSetEvidence(daysAgo(it), volume = 2_000.0),
                        StrengthSetEvidence(daysAgo(it), volume = 2_000.0),
                        StrengthSetEvidence(daysAgo(it), volume = 2_000.0),
                    )
                },
            families = days.map { FamilyEvidence(daysAgo(it), ActivityFamily.TRADITIONAL_STRENGTH, 45 * 60) },
        )

    @Test
    fun `pure lifter - strength OK, cardio reads as confident ZERO, recovery unavailable`() {
        val p = engine.resolve(liftingEvidence(1..8), now)
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.ACTIVITY))
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.STRENGTH))
        assertTrue("strength score should be substantial", p[BuildCharacteristic.STRENGTH]!!.score > 60.0)
        // Active enough (8 days) => absent cardio is a meaningful ZERO, not INSUFFICIENT.
        assertEquals(EvidenceState.ZERO, state(p, BuildCharacteristic.SPEED))
        assertEquals(EvidenceState.ZERO, state(p, BuildCharacteristic.DISTANCE))
        assertEquals(EvidenceState.ZERO, state(p, BuildCharacteristic.ENDURANCE))
        assertEquals(EvidenceState.ZERO, state(p, BuildCharacteristic.VERSATILITY))
        assertEquals(EvidenceState.UNAVAILABLE, state(p, BuildCharacteristic.RECOVERY))
    }

    @Test
    fun `pure runner - endurance, distance, speed OK and strength reads as ZERO`() {
        val days = 1..8
        val p =
            engine.resolve(
                BuildEvidence(
                    sessions = days.map { SessionEvidence(daysAgo(it), 40 * 60) },
                    families = days.map { FamilyEvidence(daysAgo(it), ActivityFamily.RUN_WALK, 40 * 60) },
                    distances = days.map { DistanceEvidence(daysAgo(it), 5_000.0, ActivityModality.RUN) },
                    paces = days.map { PaceEvidence(daysAgo(it), 3.3, ActivityModality.RUN) },
                ),
                now,
            )
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.ENDURANCE))
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.DISTANCE))
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.SPEED))
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.VERSATILITY))
        assertEquals(EvidenceState.ZERO, state(p, BuildCharacteristic.STRENGTH))
    }

    @Test
    fun `hybrid - multiple families give OK versatility and readiness gives OK recovery`() {
        val days = 1..8
        val p =
            engine.resolve(
                BuildEvidence(
                    sessions = days.map { SessionEvidence(daysAgo(it), 45 * 60) },
                    strengthSets = (1..4).flatMap { d -> List(3) { StrengthSetEvidence(daysAgo(d), 2_000.0) } },
                    families =
                        listOf(
                            FamilyEvidence(daysAgo(1), ActivityFamily.TRADITIONAL_STRENGTH, 45 * 60),
                            FamilyEvidence(daysAgo(2), ActivityFamily.RUN_WALK, 40 * 60),
                            FamilyEvidence(daysAgo(3), ActivityFamily.COMBAT, 40 * 60),
                            FamilyEvidence(daysAgo(5), ActivityFamily.RUN_WALK, 40 * 60),
                            FamilyEvidence(daysAgo(6), ActivityFamily.COMBAT, 40 * 60),
                        ),
                    distances = listOf(daysAgo(2), daysAgo(5)).map { DistanceEvidence(it, 5_000.0, ActivityModality.RUN) },
                    recovery = (1..3).map { RecoverySignal(daysAgo(it), RecoveryKind.READINESS_SNAPSHOT, readinessScore = 72.0) },
                ),
                now,
            )
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.VERSATILITY))
        assertTrue("two non-baseline families -> versatility above one-family level", p[BuildCharacteristic.VERSATILITY]!!.score > 50.0)
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.RECOVERY))
        assertTrue("recovery tracks readiness score", p[BuildCharacteristic.RECOVERY]!!.score in 65.0..80.0)
    }

    // ---------- guards ----------

    @Test
    fun `a single huge session cannot make a sparse characteristic OK`() {
        val base = liftingEvidence(1..6) // active enough
        val spiked =
            base.copy(
                families = base.families + FamilyEvidence(daysAgo(3), ActivityFamily.RUN_WALK, 90 * 60),
                distances = listOf(DistanceEvidence(daysAgo(3), 20_000.0, ActivityModality.RUN)),
                paces = listOf(PaceEvidence(daysAgo(3), 4.0, ActivityModality.RUN)),
            )
        val p = engine.resolve(spiked, now)
        // One day of cardio, however large, stays INSUFFICIENT — it never crowns Ranger/Mage.
        assertEquals(EvidenceState.INSUFFICIENT, state(p, BuildCharacteristic.DISTANCE))
        assertEquals(EvidenceState.INSUFFICIENT, state(p, BuildCharacteristic.ENDURANCE))
        assertEquals(EvidenceState.INSUFFICIENT, state(p, BuildCharacteristic.SPEED))
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.STRENGTH))
    }

    @Test
    fun `endurance ignores lifting duration - long lifting sessions do not qualify`() {
        // Active lifter, generous 60-min lifting sessions; 60 * 0.2 = 12 weighted min < 15 threshold.
        val p =
            engine.resolve(
                liftingEvidence(1..6).copy(
                    families =
                        (1..6).map {
                            FamilyEvidence(daysAgo(it), ActivityFamily.TRADITIONAL_STRENGTH, 60 * 60)
                        },
                ),
                now,
            )
        assertEquals(EvidenceState.ZERO, state(p, BuildCharacteristic.ENDURANCE))
    }

    // ---------- state trichotomy ----------

    @Test
    fun `ZERO, INSUFFICIENT and UNAVAILABLE are distinguished for distance and recovery`() {
        val lifter = engine.resolve(liftingEvidence(1..8), now)
        assertEquals(EvidenceState.ZERO, state(lifter, BuildCharacteristic.DISTANCE)) // active, genuinely none

        val oneRun =
            engine.resolve(
                BuildEvidence(
                    sessions = (1..2).map { SessionEvidence(daysAgo(it), 30 * 60) },
                    distances = listOf(DistanceEvidence(daysAgo(1), 5_000.0, ActivityModality.RUN)),
                ),
                now,
            )
        assertEquals(EvidenceState.INSUFFICIENT, state(oneRun, BuildCharacteristic.DISTANCE)) // too little to tell

        assertEquals(EvidenceState.UNAVAILABLE, state(lifter, BuildCharacteristic.RECOVERY)) // no source signal
        assertEquals(0.0, lifter[BuildCharacteristic.RECOVERY]!!.confidence, 0.0001)
    }

    @Test
    fun `inactivity is never inferred as recovery`() {
        // A user who simply stopped training: recovery must not silently rise.
        val p = engine.resolve(BuildEvidence.EMPTY, now)
        assertEquals(EvidenceState.UNAVAILABLE, state(p, BuildCharacteristic.RECOVERY))
        assertEquals(0.0, p[BuildCharacteristic.RECOVERY]!!.score, 0.0001)
    }

    // ---------- trend ----------

    @Test
    fun `distance trending up reads as developing, not judgemental language`() {
        val current = (1..6).map { DistanceEvidence(daysAgo(it), 5_000.0, ActivityModality.RUN) }
        val previous = listOf(30, 40).map { DistanceEvidence(daysAgo(it), 5_000.0, ActivityModality.RUN) }
        val p =
            engine.resolve(
                BuildEvidence(
                    sessions = (1..6).map { SessionEvidence(daysAgo(it), 40 * 60) },
                    distances = current + previous,
                ),
                now,
            )
        assertEquals(EvidenceState.OK, state(p, BuildCharacteristic.DISTANCE))
        assertEquals(BuildTrend.DEVELOPING, p[BuildCharacteristic.DISTANCE]!!.trend)
    }

    @Test
    fun `older evidence lifts confidence but not the current score`() {
        // Two distance days in the current window + more in the 28-56d lookback.
        val current = (1..2).map { DistanceEvidence(daysAgo(it), 5_000.0, ActivityModality.RUN) }
        val older = listOf(32, 36, 44, 50).map { DistanceEvidence(daysAgo(it), 5_000.0, ActivityModality.RUN) }
        val withLookback =
            engine.resolve(
                BuildEvidence(sessions = (1..2).map { SessionEvidence(daysAgo(it), 40 * 60) }, distances = current + older),
                now,
            )
        val withoutLookback =
            engine.resolve(
                BuildEvidence(sessions = (1..2).map { SessionEvidence(daysAgo(it), 40 * 60) }, distances = current),
                now,
            )
        assertEquals(EvidenceState.OK, state(withLookback, BuildCharacteristic.DISTANCE))
        // Same current-window score...
        assertEquals(
            withoutLookback[BuildCharacteristic.DISTANCE]!!.score,
            withLookback[BuildCharacteristic.DISTANCE]!!.score,
            0.0001,
        )
        // ...but higher confidence from the older evidence.
        assertTrue(
            withLookback[BuildCharacteristic.DISTANCE]!!.confidence > withoutLookback[BuildCharacteristic.DISTANCE]!!.confidence,
        )
    }
}
