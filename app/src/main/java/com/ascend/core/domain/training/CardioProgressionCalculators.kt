package com.ascend.core.domain.training

import com.ascend.core.common.newId
import com.ascend.core.model.CardioMode
import com.ascend.core.model.CardioPerformanceSummary
import com.ascend.core.model.CardioPrescription
import com.ascend.core.model.FatigueCost
import com.ascend.core.model.ProgressionCandidate
import com.ascend.core.model.ProgressionDimension
import com.ascend.core.model.ProgressionRecommendationType
import com.ascend.core.model.ReadinessCheckIn
import javax.inject.Inject

/** Tunable cardio increments — balancing defaults carried as data, one variable per step. */
data class CardioProgressionConfig(
    val durationIncrementSeconds: Long = 300,
    val distanceIncrementMeters: Double = 400.0,
    val paceImprovementSecondsPerKm: Double = 5.0,
    val inclineIncrement: Double = 0.5,
    val resistanceIncrement: Double = 1.0,
    val maxPerceivedEffortForProgression: Int = 7,
    val struggleEffortThreshold: Int = 9,
    val requiredConsistentSessions: Int = 2,
    val deloadDurationFraction: Double = 0.8,
)

/** Repeated struggle: several recent sessions incomplete or at near-max effort. */
internal fun cardioStruggling(
    recent: List<CardioPerformanceSummary>,
    config: CardioProgressionConfig,
): Boolean {
    val struggles = recent.count { !it.completed || (it.perceivedEffort ?: 0) >= config.struggleEffortThreshold }
    return struggles >= config.requiredConsistentSessions
}

/** Consistent & comfortable: the recent window is fully completed at acceptable effort. */
internal fun cardioConsistent(
    recent: List<CardioPerformanceSummary>,
    config: CardioProgressionConfig,
): Boolean {
    val window = recent.takeLast(config.requiredConsistentSessions)
    if (window.size < config.requiredConsistentSessions) return false
    return window.all { it.completed && (it.perceivedEffort == null || it.perceivedEffort <= config.maxPerceivedEffortForProgression) }
}

/**
 * Progresses steady-state / distance / pace cardio by **one primary variable at a time**.
 * It never escalates duration *and* intensity together. Works with only manual duration,
 * distance, pace and perceived effort; repeated struggle earns a deload or a reduction,
 * never a push.
 */
class CardioProgressionCalculator
    @Inject
    constructor(private val config: CardioProgressionConfig) {
        constructor() : this(CardioProgressionConfig())

        fun recommend(
            prescription: CardioPrescription,
            recent: List<CardioPerformanceSummary>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
        ): ProgressionCandidate = candidates(prescription, recent, checkIn).first()

        /** All safe cardio options for the session, primary variable first. */
        fun candidates(
            prescription: CardioPrescription,
            recent: List<CardioPerformanceSummary>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
        ): List<ProgressionCandidate> {
            if (checkIn.painReported || checkIn.injuryReported || checkIn.seriousSymptomReported) {
                return listOf(maintainCardio("Ease off — recover first"))
            }
            if (recent.isEmpty()) return listOf(requestCardioData())

            if (cardioStruggling(recent, config)) {
                return listOf(
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.DELOAD,
                        dimension = ProgressionDimension.DELOAD,
                        summary = "Deload — recent sessions were a struggle",
                        evidence = listOf("Repeated high effort or incomplete sessions"),
                        fatigueCost = FatigueCost.NONE,
                    ),
                )
            }
            if (!cardioConsistent(recent, config)) {
                return listOf(maintainCardio("Repeat the current session before progressing"))
            }
            // Consistent and comfortable → advance exactly one primary variable (others are alternatives).
            return primaryVariableCandidates(prescription)
        }

        private fun primaryVariableCandidates(p: CardioPrescription): List<ProgressionCandidate> {
            val out = mutableListOf<ProgressionCandidate>()
            when (p.mode) {
                CardioMode.STEADY_STATE -> {
                    if (p.targetDurationSeconds != null) out += durationCandidate()
                    if (p.resistance != null) out += resistanceCandidate()
                    if (p.incline != null) out += inclineCandidate()
                }
                CardioMode.DISTANCE -> {
                    if (p.targetDistanceMeters != null) out += distanceCandidate()
                    if (p.targetPaceSecondsPerKm != null) out += paceCandidate()
                }
                CardioMode.PACE_WORK -> {
                    if (p.targetPaceSecondsPerKm != null) out += paceCandidate()
                    if (p.targetDistanceMeters != null) out += distanceCandidate()
                }
                CardioMode.INTERVAL -> {
                    if (p.targetDurationSeconds != null) out += durationCandidate()
                }
            }
            return out.ifEmpty { listOf(maintainCardio("Log more detail to progress this session")) }
        }

        private fun durationCandidate() =
            ProgressionCandidate(
                recommendationType = ProgressionRecommendationType.INCREASE_DURATION,
                dimension = ProgressionDimension.CARDIO_DURATION,
                summary = "Add ${config.durationIncrementSeconds / 60} min to the session",
                evidence = listOf("Consistent completion at comfortable effort"),
                fatigueCost = FatigueCost.LOW,
            )

        private fun distanceCandidate() =
            ProgressionCandidate(
                recommendationType = ProgressionRecommendationType.INCREASE_DISTANCE,
                dimension = ProgressionDimension.CARDIO_DISTANCE,
                summary = "Extend the distance by ${config.distanceIncrementMeters.toInt()} m",
                fatigueCost = FatigueCost.MODERATE,
            )

        private fun paceCandidate() =
            ProgressionCandidate(
                recommendationType = ProgressionRecommendationType.INCREASE_PACE,
                dimension = ProgressionDimension.CARDIO_PACE,
                summary = "Improve pace slightly (${config.paceImprovementSecondsPerKm.toInt()}s/km faster)",
                fatigueCost = FatigueCost.MODERATE,
            )

        private fun resistanceCandidate() =
            ProgressionCandidate(
                recommendationType = ProgressionRecommendationType.INCREASE_RESISTANCE,
                dimension = ProgressionDimension.CARDIO_RESISTANCE,
                summary = "Raise resistance a step",
                fatigueCost = FatigueCost.MODERATE,
            )

        private fun inclineCandidate() =
            ProgressionCandidate(
                recommendationType = ProgressionRecommendationType.INCREASE_INCLINE,
                dimension = ProgressionDimension.CARDIO_INCLINE,
                summary = "Raise incline a step",
                fatigueCost = FatigueCost.MODERATE,
            )

        /** The proposed prescription for a chosen cardio candidate (applied on accept). */
        fun applyToPrescription(
            p: CardioPrescription,
            dimension: ProgressionDimension,
        ): CardioPrescription =
            when (dimension) {
                ProgressionDimension.CARDIO_DURATION ->
                    p.copy(
                        id = newId(),
                        targetDurationSeconds = (p.targetDurationSeconds ?: 0) + config.durationIncrementSeconds,
                        status = "PROPOSED",
                    )
                ProgressionDimension.CARDIO_DISTANCE ->
                    p.copy(
                        id = newId(),
                        targetDistanceMeters = (p.targetDistanceMeters ?: 0.0) + config.distanceIncrementMeters,
                        status = "PROPOSED",
                    )
                ProgressionDimension.CARDIO_PACE ->
                    p.copy(
                        id = newId(),
                        targetPaceSecondsPerKm = (p.targetPaceSecondsPerKm ?: 0.0) - config.paceImprovementSecondsPerKm,
                        status = "PROPOSED",
                    )
                ProgressionDimension.CARDIO_RESISTANCE ->
                    p.copy(id = newId(), resistance = (p.resistance ?: 0.0) + config.resistanceIncrement, status = "PROPOSED")
                ProgressionDimension.CARDIO_INCLINE ->
                    p.copy(id = newId(), incline = (p.incline ?: 0.0) + config.inclineIncrement, status = "PROPOSED")
                else -> p
            }

        private fun maintainCardio(reason: String) =
            ProgressionCandidate(
                ProgressionRecommendationType.MAINTAIN_PRESCRIPTION,
                ProgressionDimension.MAINTAIN,
                reason,
                fatigueCost = FatigueCost.NONE,
            )

        private fun requestCardioData() =
            ProgressionCandidate(
                ProgressionRecommendationType.REQUEST_MORE_DATA,
                ProgressionDimension.MAINTAIN,
                "No cardio sessions logged yet",
                fatigueCost = FatigueCost.NONE,
                requiresMoreData = true,
            )
    }

/**
 * Progresses interval sessions. Adds an interval **or** trims the rest interval — never
 * both aggressively in one step. Repeated struggle reduces intensity instead.
 */
class IntervalProgressionCalculator
    @Inject
    constructor(private val config: CardioProgressionConfig) {
        constructor() : this(CardioProgressionConfig())

        fun recommend(
            prescription: CardioPrescription,
            recent: List<CardioPerformanceSummary>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
        ): ProgressionCandidate = candidates(prescription, recent, checkIn).first()

        fun candidates(
            prescription: CardioPrescription,
            recent: List<CardioPerformanceSummary>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
        ): List<ProgressionCandidate> {
            if (checkIn.painReported || checkIn.injuryReported || checkIn.seriousSymptomReported) {
                return listOf(maintain("Ease off — recover first"))
            }
            if (recent.isEmpty()) {
                return listOf(
                    ProgressionCandidate(
                        ProgressionRecommendationType.REQUEST_MORE_DATA,
                        ProgressionDimension.MAINTAIN,
                        "No interval sessions logged yet",
                        requiresMoreData = true,
                        fatigueCost = FatigueCost.NONE,
                    ),
                )
            }
            val currentRounds = prescription.intervalCount ?: 0
            if (cardioStruggling(recent, config)) {
                return listOf(
                    ProgressionCandidate(
                        ProgressionRecommendationType.REDUCE_INTERVAL,
                        ProgressionDimension.CARDIO_INTERVAL,
                        "Reduce to ${(currentRounds - 1).coerceAtLeast(1)} rounds — recent rounds were a struggle",
                        fatigueCost = FatigueCost.NONE,
                    ),
                )
            }
            if (!cardioConsistent(recent, config)) return listOf(maintain("Repeat the current intervals before progressing"))

            // Add a round OR trim rest — offered as separate atomic candidates, never fused.
            return listOf(
                ProgressionCandidate(
                    ProgressionRecommendationType.ADD_INTERVAL,
                    ProgressionDimension.CARDIO_INTERVAL,
                    "Add a work interval (to ${currentRounds + 1} rounds)",
                    evidence = listOf("Consistent, comfortable rounds"),
                    fatigueCost = FatigueCost.HIGH,
                ),
                ProgressionCandidate(
                    ProgressionRecommendationType.CHANGE_WORK_REST_RATIO,
                    ProgressionDimension.CARDIO_INTERVAL,
                    "Shorten the rest interval a little",
                    fatigueCost = FatigueCost.MODERATE,
                ),
            )
        }

        private fun maintain(reason: String) =
            ProgressionCandidate(
                ProgressionRecommendationType.MAINTAIN_PRESCRIPTION,
                ProgressionDimension.MAINTAIN,
                reason,
                fatigueCost = FatigueCost.NONE,
            )
    }
