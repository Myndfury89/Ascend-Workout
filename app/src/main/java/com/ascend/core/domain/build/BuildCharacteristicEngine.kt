package com.ascend.core.domain.build

import javax.inject.Inject
import kotlin.math.min

private const val DAY_MS = 86_400_000L
private const val SCORE_MAX = 100.0
private const val SECONDS_PER_MINUTE = 60.0

/**
 * Pure, read-only engine turning verified evidence into the seven [BuildCharacteristic] scores. It is
 * a deterministic function of ([BuildEvidence], `now`, [BuildTuning]) — it holds no repositories and
 * writes nothing, so it never grants or removes progression and needs no database to test.
 *
 * Window model: scores use the current [BuildTuning.currentWindowDays]; sparse signals additionally
 * look back [BuildTuning.lookbackWindowDays] for confidence only. Speed, Distance, Endurance and
 * Recovery are anti-spike guarded (≥ [BuildTuning.sparseGuardMinSessions] qualifying days to reach
 * OK). Activity is scored and displayed but excluded from class signatures; here it also decides
 * whether an absent signal reads as a confident [EvidenceState.ZERO] or an uncertain
 * [EvidenceState.INSUFFICIENT].
 */
class BuildCharacteristicEngine
    @Inject
    constructor() {
        fun resolve(
            evidence: BuildEvidence,
            now: Long,
            tuning: BuildTuning = BuildTuning(),
        ): BuildProfile {
            val activity = resolveActivity(evidence, now, tuning)
            val activityConfidence = activity.confidence
            val activeEnough =
                distinctDays(evidence.sessions.currentWindow(now, tuning)) { it.at } >= tuning.confidentAbsenceMinActiveDays

            val scores =
                linkedMapOf(
                    BuildCharacteristic.ACTIVITY to activity,
                    BuildCharacteristic.STRENGTH to resolveStrength(evidence, now, tuning, activeEnough, activityConfidence),
                    BuildCharacteristic.VERSATILITY to resolveVersatility(evidence, now, tuning, activeEnough, activityConfidence),
                    BuildCharacteristic.ENDURANCE to resolveEndurance(evidence, now, tuning, activeEnough, activityConfidence),
                    BuildCharacteristic.SPEED to resolveSpeed(evidence, now, tuning, activeEnough, activityConfidence),
                    BuildCharacteristic.DISTANCE to resolveDistance(evidence, now, tuning, activeEnough, activityConfidence),
                    BuildCharacteristic.RECOVERY to resolveRecovery(evidence, now, tuning, activityConfidence),
                )
            return BuildProfile(scores = scores, overallConfidence = activityConfidence, windowDays = tuning.currentWindowDays)
        }

        // ---- Activity: session frequency; never UNAVAILABLE (the app always tracks workouts). ----
        private fun resolveActivity(
            evidence: BuildEvidence,
            now: Long,
            tuning: BuildTuning,
        ): CharacteristicScore {
            val curDays = distinctDays(evidence.sessions.currentWindow(now, tuning)) { it.at }
            val prevDays = distinctDays(evidence.sessions.previousWindow(now, tuning)) { it.at }
            val lookDays = distinctDays(evidence.sessions.lookbackWindow(now, tuning)) { it.at }
            val score = saturate(curDays.toDouble(), tuning.activityHalfSatDays)
            val prevScore = saturate(prevDays.toDouble(), tuning.activityHalfSatDays)
            val state =
                when {
                    curDays >= tuning.minSessionsForOk -> EvidenceState.OK
                    curDays == 1 -> EvidenceState.INSUFFICIENT
                    else -> EvidenceState.ZERO
                }
            val confidence = min(1.0, lookDays / tuning.confidenceFullDays)
            return CharacteristicScore(
                characteristic = BuildCharacteristic.ACTIVITY,
                score = score,
                state = state,
                trend = trend(score, prevScore, curDays >= 1 && prevDays >= 1, tuning),
                confidence = if (state == EvidenceState.INSUFFICIENT) min(tuning.insufficientConfidenceCap, confidence) else confidence,
            )
        }

        // ---- Strength: sustained resistance volume + bounded PR contribution. ----
        private fun resolveStrength(
            evidence: BuildEvidence,
            now: Long,
            tuning: BuildTuning,
            activeEnough: Boolean,
            activityConfidence: Double,
        ): CharacteristicScore {
            val cur = evidence.strengthSets.currentWindow(now, tuning)
            val prev = evidence.strengthSets.previousWindow(now, tuning)
            val lookDays = distinctDays(evidence.strengthSets.lookbackWindow(now, tuning)) { it.at }
            val curDays = distinctDays(cur) { it.at }
            val prBonus = min(cur.count { it.isPersonalRecord } * tuning.personalRecordBonusPerPr, tuning.personalRecordBonusCap)
            val score = min(SCORE_MAX, saturate(cur.sumOf { it.volume }, tuning.strengthHalfSatVolume) + prBonus)
            val prevScore = saturate(prev.sumOf { it.volume }, tuning.strengthHalfSatVolume)
            val state =
                when {
                    lookDays == 0 -> absentState(activeEnough)
                    curDays >= tuning.minSessionsForOk && cur.size >= tuning.minStrengthSets -> EvidenceState.OK
                    curDays == 0 -> absentState(activeEnough)
                    else -> EvidenceState.INSUFFICIENT
                }
            return CharacteristicScore(
                characteristic = BuildCharacteristic.STRENGTH,
                score = if (state == EvidenceState.ZERO) 0.0 else score,
                state = state,
                trend = trend(score, prevScore, curDays >= 1 && distinctDays(prev) { it.at } >= 1, tuning),
                confidence = stateConfidence(state, lookDays, activityConfidence, tuning),
            )
        }

        // ---- Versatility: distinct qualifying activity families (not exercise count). ----
        private fun resolveVersatility(
            evidence: BuildEvidence,
            now: Long,
            tuning: BuildTuning,
            activeEnough: Boolean,
            activityConfidence: Double,
        ): CharacteristicScore {
            val curFamilies = qualifyingFamilies(evidence.families.currentWindow(now, tuning), tuning)
            val prevFamilies = qualifyingFamilies(evidence.families.previousWindow(now, tuning), tuning)
            val lookDays = distinctDays(evidence.families.lookbackWindow(now, tuning)) { it.at }
            val score = saturate(curFamilies.size.toDouble(), tuning.versatilityHalfSatFamilies)
            val prevScore = saturate(prevFamilies.size.toDouble(), tuning.versatilityHalfSatFamilies)
            val state =
                when {
                    lookDays == 0 -> absentState(activeEnough)
                    curFamilies.isNotEmpty() -> EvidenceState.OK
                    else -> absentState(activeEnough)
                }
            return CharacteristicScore(
                characteristic = BuildCharacteristic.VERSATILITY,
                score = if (state == EvidenceState.ZERO) 0.0 else score,
                state = state,
                trend = trend(score, prevScore, lookDays > 0, tuning),
                confidence = stateConfidence(state, lookDays, activityConfidence, tuning),
            )
        }

        // ---- Endurance: type-weighted sustained duration; sparse-guarded. ----
        private fun resolveEndurance(
            evidence: BuildEvidence,
            now: Long,
            tuning: BuildTuning,
            activeEnough: Boolean,
            activityConfidence: Double,
        ): CharacteristicScore {
            val cur = enduranceSessions(evidence.families.currentWindow(now, tuning), tuning)
            val prev = enduranceSessions(evidence.families.previousWindow(now, tuning), tuning)
            val lookDays = distinctDays(evidence.families.lookbackWindow(now, tuning)) { it.at }
            val curGuardDays = distinctDays(cur) { it.at }
            val score = saturate(cur.sumOf { it.weightedMinutes }, tuning.enduranceHalfSatWeightedMinutes)
            val prevScore = saturate(prev.sumOf { it.weightedMinutes }, tuning.enduranceHalfSatWeightedMinutes)
            val state = sparseState(curGuardDays, lookDays > 0, activeEnough, tuning)
            return CharacteristicScore(
                characteristic = BuildCharacteristic.ENDURANCE,
                score = if (state == EvidenceState.ZERO) 0.0 else score,
                state = state,
                trend = trend(score, prevScore, curGuardDays >= 1 && distinctDays(prev) { it.at } >= 1, tuning),
                confidence = stateConfidence(state, lookDays, activityConfidence, tuning),
            )
        }

        // ---- Speed: modality-normalized pace; sparse-guarded. ----
        private fun resolveSpeed(
            evidence: BuildEvidence,
            now: Long,
            tuning: BuildTuning,
            activeEnough: Boolean,
            activityConfidence: Double,
        ): CharacteristicScore {
            val cur = evidence.paces.currentWindow(now, tuning)
            val prev = evidence.paces.previousWindow(now, tuning)
            val lookDays = distinctDays(evidence.paces.lookbackWindow(now, tuning)) { it.at }
            val curGuardDays = distinctDays(cur) { it.at }
            val score = SCORE_MAX * meanEmphasis(cur, tuning)
            val prevScore = SCORE_MAX * meanEmphasis(prev, tuning)
            val state = sparseState(curGuardDays, lookDays > 0, activeEnough, tuning)
            return CharacteristicScore(
                characteristic = BuildCharacteristic.SPEED,
                score = if (state == EvidenceState.ZERO) 0.0 else score,
                state = state,
                trend = trend(score, prevScore, curGuardDays >= 1 && distinctDays(prev) { it.at } >= 1, tuning),
                confidence = stateConfidence(state, lookDays, activityConfidence, tuning),
            )
        }

        // ---- Distance: modality-normalized distance units; sparse-guarded. ----
        private fun resolveDistance(
            evidence: BuildEvidence,
            now: Long,
            tuning: BuildTuning,
            activeEnough: Boolean,
            activityConfidence: Double,
        ): CharacteristicScore {
            val cur = evidence.distances.currentWindow(now, tuning)
            val prev = evidence.distances.previousWindow(now, tuning)
            val lookDays = distinctDays(evidence.distances.lookbackWindow(now, tuning)) { it.at }
            val curGuardDays = distinctDays(cur) { it.at }
            val score = saturate(distanceUnits(cur, tuning), tuning.distanceHalfSatUnits)
            val prevScore = saturate(distanceUnits(prev, tuning), tuning.distanceHalfSatUnits)
            val state = sparseState(curGuardDays, lookDays > 0, activeEnough, tuning)
            return CharacteristicScore(
                characteristic = BuildCharacteristic.DISTANCE,
                score = if (state == EvidenceState.ZERO) 0.0 else score,
                state = state,
                trend = trend(score, prevScore, curGuardDays >= 1 && distinctDays(prev) { it.at } >= 1, tuning),
                confidence = stateConfidence(state, lookDays, activityConfidence, tuning),
            )
        }

        // ---- Recovery: legitimate recovery signals only; absence is UNAVAILABLE, never inferred. ----
        private fun resolveRecovery(
            evidence: BuildEvidence,
            now: Long,
            tuning: BuildTuning,
            activityConfidence: Double,
        ): CharacteristicScore {
            val cur = evidence.recovery.currentWindow(now, tuning)
            val prev = evidence.recovery.previousWindow(now, tuning)
            val lookDays = distinctDays(evidence.recovery.lookbackWindow(now, tuning)) { it.at }
            val curGuardDays = distinctDays(cur) { it.at }
            val score = recoveryScore(cur, tuning)
            val prevScore = recoveryScore(prev, tuning)
            val state =
                when {
                    lookDays == 0 -> EvidenceState.UNAVAILABLE
                    curGuardDays >= tuning.sparseGuardMinSessions -> EvidenceState.OK
                    else -> EvidenceState.INSUFFICIENT
                }
            return CharacteristicScore(
                characteristic = BuildCharacteristic.RECOVERY,
                score = score,
                state = state,
                trend = trend(score, prevScore, curGuardDays >= 1 && distinctDays(prev) { it.at } >= 1, tuning),
                confidence = stateConfidence(state, lookDays, activityConfidence, tuning),
            )
        }

        // ---------- shared state / confidence / trend ----------

        /** Absence of a source-available signal: a confident ZERO if the user is clearly active, else uncertain. */
        private fun absentState(activeEnough: Boolean): EvidenceState = if (activeEnough) EvidenceState.ZERO else EvidenceState.INSUFFICIENT

        /** Anti-spike state for the guarded sparse characteristics (Speed/Distance/Endurance). */
        private fun sparseState(
            curGuardDays: Int,
            hasLookback: Boolean,
            activeEnough: Boolean,
            tuning: BuildTuning,
        ): EvidenceState =
            when {
                curGuardDays >= tuning.sparseGuardMinSessions -> EvidenceState.OK
                curGuardDays == 1 -> EvidenceState.INSUFFICIENT
                activeEnough -> EvidenceState.ZERO
                hasLookback -> EvidenceState.INSUFFICIENT
                else -> EvidenceState.INSUFFICIENT
            }

        private fun stateConfidence(
            state: EvidenceState,
            lookbackDays: Int,
            activityConfidence: Double,
            tuning: BuildTuning,
        ): Double =
            when (state) {
                EvidenceState.OK -> min(1.0, lookbackDays / tuning.confidenceFullDays)
                EvidenceState.ZERO -> activityConfidence
                EvidenceState.INSUFFICIENT -> min(tuning.insufficientConfidenceCap, lookbackDays / tuning.confidenceFullDays)
                EvidenceState.UNAVAILABLE -> 0.0
            }

        private fun trend(
            current: Double,
            previous: Double,
            bothWindowsQualify: Boolean,
            tuning: BuildTuning,
        ): BuildTrend =
            when {
                !bothWindowsQualify -> BuildTrend.UNKNOWN
                current - previous > tuning.trendMarginPoints -> BuildTrend.DEVELOPING
                previous - current > tuning.trendMarginPoints -> BuildTrend.DE_EMPHASIZED
                else -> BuildTrend.MAINTAINING
            }

        // ---------- characteristic-specific helpers ----------

        private data class EnduranceSession(val at: Long, val weightedMinutes: Double)

        private fun enduranceSessions(
            families: List<FamilyEvidence>,
            tuning: BuildTuning,
        ): List<EnduranceSession> =
            families
                .map { EnduranceSession(it.at, it.durationSeconds / SECONDS_PER_MINUTE * tuning.enduranceWeightFor(it.family)) }
                .filter { it.weightedMinutes >= tuning.enduranceQualifyingWeightedMinutes }

        private fun qualifyingFamilies(
            families: List<FamilyEvidence>,
            tuning: BuildTuning,
        ): Set<ActivityFamily> =
            families
                .filterNot { it.family.isBaseline }
                .groupBy { it.family }
                .filterValues { entries -> entries.sumOf { it.durationSeconds } / SECONDS_PER_MINUTE >= tuning.versatilityFamilyMinMinutes }
                .keys

        private fun meanEmphasis(
            paces: List<PaceEvidence>,
            tuning: BuildTuning,
        ): Double {
            if (paces.isEmpty()) return 0.0
            return paces
                .map { min(tuning.speedEmphasisCap, it.metersPerSecond / tuning.speedReferenceFor(it.modality)) }
                .average()
                .coerceIn(0.0, 1.0)
        }

        private fun distanceUnits(
            distances: List<DistanceEvidence>,
            tuning: BuildTuning,
        ): Double = distances.sumOf { it.meters / tuning.distanceReferenceFor(it.modality) }

        private fun recoveryScore(
            signals: List<RecoverySignal>,
            tuning: BuildTuning,
        ): Double {
            val readiness = signals.mapNotNull { it.readinessScore }
            return if (readiness.isNotEmpty()) {
                readiness.average()
            } else {
                saturate(signals.size.toDouble(), tuning.recoveryCountHalfSat)
            }
        }

        // ---------- window + math primitives ----------

        private fun saturate(
            value: Double,
            half: Double,
        ): Double = if (value <= 0.0) 0.0 else SCORE_MAX * value / (value + half)

        private fun <T> distinctDays(
            items: List<T>,
            at: (T) -> Long,
        ): Int = items.map { at(it).floorDiv(DAY_MS) }.toSet().size
    }

// ---- window filters (epoch-millis, current / previous / lookback) ----

private fun <T> List<T>.currentWindow(
    now: Long,
    tuning: BuildTuning,
): List<T> = windowBetween(now, tuning.currentWindowDays, 0)

private fun <T> List<T>.previousWindow(
    now: Long,
    tuning: BuildTuning,
): List<T> = windowBetween(now, tuning.currentWindowDays * 2, tuning.currentWindowDays)

private fun <T> List<T>.lookbackWindow(
    now: Long,
    tuning: BuildTuning,
): List<T> = windowBetween(now, tuning.lookbackWindowDays, 0)

/** Items whose `at` (via reflection-free access below) falls in (now - startDays, now - endDays]. */
private fun <T> List<T>.windowBetween(
    now: Long,
    startDaysAgo: Int,
    endDaysAgo: Int,
): List<T> {
    val start = now - startDaysAgo.toLong() * DAY_MS
    val end = now - endDaysAgo.toLong() * DAY_MS
    return filter { item ->
        val at = atOf(item)
        at > start && at <= end
    }
}

/** Extracts the timestamp from any supported evidence shape without per-type window overloads. */
private fun atOf(item: Any?): Long =
    when (item) {
        is SessionEvidence -> item.at
        is StrengthSetEvidence -> item.at
        is FamilyEvidence -> item.at
        is DistanceEvidence -> item.at
        is PaceEvidence -> item.at
        is RecoverySignal -> item.at
        else -> error("unsupported evidence type: ${item?.let { it::class.simpleName }}")
    }
