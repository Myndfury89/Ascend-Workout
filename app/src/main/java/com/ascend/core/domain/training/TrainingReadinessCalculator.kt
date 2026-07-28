package com.ascend.core.domain.training

import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.ReadinessCheckIn
import com.ascend.core.model.RecentBaseline
import com.ascend.core.model.SessionPerformance
import com.ascend.core.model.TrainingReadiness
import com.ascend.core.model.TrainingReadinessState
import javax.inject.Inject

/** Configurable, explainable readiness thresholds — balancing defaults only. */
data class ReadinessConfig(
    val requiredSuccessfulExposures: Int = 1,
    val lowDataThreshold: Int = 3,
    val extraExposureWhenLowData: Int = 1,
    val maxPerceivedEffortForProgression: Int = 8,
    val maxRpeForLoad: Double = 9.0,
    val questIncreaseConsistency: Double = 0.8,
    val questReduceConsistency: Double = 0.5,
)

/**
 * Determines training readiness from **real performance** — never from level/EXP. The
 * result is structured (state + score + confidence + explicit evidence/limiting/
 * missing signals + safety), never a lone opaque number. Works with missing signals;
 * confidence simply drops. Safety symptoms always block and win.
 */
class TrainingReadinessCalculator
    @Inject
    constructor(private val config: ReadinessConfig) {
        constructor() : this(ReadinessConfig())

        fun evaluateResistance(
            sessions: List<SessionPerformance>,
            prescription: ExercisePrescription,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
            now: Long = System.currentTimeMillis(),
        ): TrainingReadiness {
            safetyGate(checkIn, now)?.let { return it }

            if (sessions.isEmpty()) return insufficient(now, "No completed sessions for this exercise yet")

            val targetSets = prescription.targetSets ?: 1
            val maxReps = prescription.maximumReps ?: Int.MAX_VALUE
            val minReps = prescription.minimumReps ?: 0
            val latest = sessions.last()

            val positive = mutableListOf<String>()
            val limiting = mutableListOf<String>()
            val missing = mutableListOf<String>()

            fun SessionPerformance.allTopRange() = sets.size >= targetSets && sets.take(targetSets).all { it.reps >= maxReps && !it.failed }

            val anyFailure = latest.anyFailure || latest.sets.take(targetSets).any { it.reps < minReps }
            val exposuresAtTop = sessions.reversed().takeWhile { it.allTopRange() }.size
            val requiredExposures =
                config.requiredSuccessfulExposures +
                    if (sessions.size < config.lowDataThreshold) config.extraExposureWhenLowData else 0

            if (latest.perceivedEffort == null && latest.sets.all { it.rpe == null }) missing += "No effort/RPE reported"
            val effortAcceptable =
                (latest.perceivedEffort?.let { it <= config.maxPerceivedEffortForProgression } ?: true) &&
                    latest.sets.mapNotNull { it.rpe }.all { it <= config.maxRpeForLoad }
            if (!effortAcceptable) limiting += "Effort near maximum on the last session"
            if (anyFailure) limiting += "A prescribed set was not completed"

            val confidence = confidenceFor(sessions.size, hasSubjective = latest.perceivedEffort != null)

            val state =
                when {
                    anyFailure -> TrainingReadinessState.MAINTAIN
                    latest.allTopRange() && effortAcceptable && exposuresAtTop >= requiredExposures -> {
                        positive += "All $targetSets sets reached the top of the rep range"
                        positive += "$exposuresAtTop successful exposure(s) at the top range"
                        TrainingReadinessState.READY_FOR_LOAD_PROGRESSION
                    }
                    !latest.allTopRange() && !anyFailure -> {
                        positive += "Stable performance below the top of the rep range"
                        TrainingReadinessState.READY_FOR_REP_PROGRESSION
                    }
                    else -> TrainingReadinessState.MAINTAIN
                }

            return TrainingReadiness(
                state = state,
                score = scoreFor(state),
                confidence = confidence,
                evidence = listOf("Last session: ${latest.sets.joinToString(", ") { it.reps.toString() }} reps"),
                positiveSignals = positive,
                limitingSignals = limiting,
                missingSignals = missing,
                safetyState = ProgressionSafetyState.OK,
                safetyFlags = emptyList(),
                generatedAt = now,
            )
        }

        fun evaluateDailyQuest(
            baseline: RecentBaseline?,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
            now: Long = System.currentTimeMillis(),
        ): TrainingReadiness {
            safetyGate(checkIn, now)?.let { return it }
            if (baseline == null || baseline.sampleCount < 2) return insufficient(now, "Not enough recent quest history")

            val positive = mutableListOf<String>()
            val limiting = mutableListOf<String>()

            val state =
                when {
                    baseline.completionConsistency >= config.questIncreaseConsistency && baseline.recentTrend >= 0.0 -> {
                        positive += "Consistently completing recent targets"
                        TrainingReadinessState.READY_FOR_REP_PROGRESSION
                    }
                    baseline.completionConsistency < config.questReduceConsistency -> {
                        limiting += "Recent targets frequently left incomplete"
                        TrainingReadinessState.DELOAD_RECOMMENDED
                    }
                    else -> {
                        limiting += "Borderline recent completion"
                        TrainingReadinessState.MAINTAIN
                    }
                }

            return TrainingReadiness(
                state = state,
                score = scoreFor(state),
                confidence = baseline.confidence,
                evidence =
                    listOf(
                        "Representative target ${baseline.representativeTarget}",
                        "Consistency ${(baseline.completionConsistency * 100).toInt()}%",
                    ),
                positiveSignals = positive,
                limitingSignals = limiting,
                missingSignals = if (baseline.averagePerceivedEffort == null) listOf("No perceived‑effort history") else emptyList(),
                safetyState = ProgressionSafetyState.OK,
                safetyFlags = emptyList(),
                generatedAt = now,
            )
        }

        private fun safetyGate(
            checkIn: ReadinessCheckIn,
            now: Long,
        ): TrainingReadiness? {
            if (checkIn.seriousSymptomReported) {
                return blocked(
                    now,
                    TrainingReadinessState.RECOVERY_RECOMMENDED,
                    ProgressionSafetyState.STOP_AND_SEEK_GUIDANCE,
                    "Serious symptom reported — stop and seek appropriate guidance",
                )
            }
            if (checkIn.injuryReported || checkIn.painReported) {
                return blocked(now, TrainingReadinessState.REGRESS, ProgressionSafetyState.BLOCKED, "Pain/injury reported — no progression")
            }
            return null
        }

        private fun blocked(
            now: Long,
            state: TrainingReadinessState,
            safety: ProgressionSafetyState,
            flag: String,
        ) = TrainingReadiness(state, 0.0, 1.0, listOf(flag), emptyList(), listOf(flag), emptyList(), safety, listOf(flag), now)

        private fun insufficient(
            now: Long,
            reason: String,
        ) = TrainingReadiness(
            TrainingReadinessState.INSUFFICIENT_DATA, 0.0, 0.2, listOf(reason), emptyList(), emptyList(),
            listOf(reason), ProgressionSafetyState.OK, emptyList(), now,
        )

        private fun confidenceFor(
            sessionCount: Int,
            hasSubjective: Boolean,
        ): Double {
            val base = (sessionCount.toDouble() / config.lowDataThreshold).coerceIn(0.2, 1.0)
            return if (hasSubjective) base else base * 0.85
        }

        private fun scoreFor(state: TrainingReadinessState): Double =
            when (state) {
                TrainingReadinessState.READY_FOR_LOAD_PROGRESSION -> 1.0
                TrainingReadinessState.READY_FOR_REP_PROGRESSION -> 0.8
                TrainingReadinessState.MAINTAIN -> 0.5
                TrainingReadinessState.DELOAD_RECOMMENDED, TrainingReadinessState.REGRESS -> 0.2
                else -> 0.0
            }
    }
