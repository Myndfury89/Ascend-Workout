package com.ascend.core.domain.training

import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.FatigueCost
import com.ascend.core.model.ProgressionCandidate
import com.ascend.core.model.ProgressionDimension
import com.ascend.core.model.ProgressionRecommendationType
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.ReadinessCheckIn
import com.ascend.core.model.SessionPerformance
import javax.inject.Inject
import kotlin.math.max

/**
 * Tunable thresholds for the bodyweight calculators — balancing defaults carried as
 * data, never constants baked into logic.
 */
data class BodyweightProgressionConfig(
    val repIncrement: Int = 1,
    val requiredSessionsToAddSet: Int = 3,
    val maxWorkingSets: Int = 5,
    val restDecrementSeconds: Int = 15,
    val minimumRestSeconds: Int = 30,
    val restIncrementSeconds: Int = 30,
    val assistanceReductionFraction: Double = 0.25,
    val assistanceIncrementValue: Double = 5.0,
    val requiredExposuresToReduceAssistance: Int = 2,
    val maxPerceivedEffortForProgression: Int = 8,
    val maxRpeForProgression: Double = 9.0,
    val meaningfulDeclineFraction: Double = 0.10,
    val highFatigueThreshold: Int = 8,
)

/**
 * Real-performance signals distilled from an exercise's recent sessions. Optional
 * subjective fields only ever *temper* a decision; absent data never invents readiness.
 */
internal data class BodyweightSignals(
    val hasData: Boolean,
    val topReps: Int,
    val minReps: Int,
    val allAtTop: Boolean,
    val allAtLeastMin: Boolean,
    val anyFailure: Boolean,
    val effortAcceptable: Boolean,
    val consecutiveSuccesses: Int,
    val consecutiveFailures: Int,
    val outputDeclined: Boolean,
    val highFatigue: Boolean,
) {
    companion object {
        fun from(
            prescription: ExercisePrescription,
            sessions: List<SessionPerformance>,
            checkIn: ReadinessCheckIn,
            config: BodyweightProgressionConfig,
        ): BodyweightSignals {
            val targetSets = prescription.targetSets ?: 1
            val topReps = prescription.maximumReps ?: Int.MAX_VALUE
            val minReps = prescription.minimumReps ?: 0
            if (sessions.isEmpty()) {
                return BodyweightSignals(false, topReps, minReps, false, false, false, true, 0, 0, false, false)
            }
            val latest = sessions.last()

            fun SessionPerformance.working() = sets.take(targetSets)

            fun SessionPerformance.atTop() = sets.size >= targetSets && working().all { it.reps >= topReps && !it.failed }

            fun SessionPerformance.atLeastMin() = sets.size >= targetSets && working().all { it.reps >= minReps && !it.failed }

            fun SessionPerformance.success() = atTop()

            fun SessionPerformance.failure() = anyFailure || working().any { it.reps < minReps }

            val effortAcceptable =
                (latest.perceivedEffort?.let { it <= config.maxPerceivedEffortForProgression } ?: true) &&
                    latest.sets.mapNotNull { it.rpe }.all { it <= config.maxRpeForProgression }

            val consecutiveSuccesses = sessions.reversed().takeWhile { it.success() }.size
            val consecutiveFailures = sessions.reversed().takeWhile { it.failure() }.size

            val outputDeclined =
                if (sessions.size < 2) {
                    false
                } else {
                    val prev = sessions[sessions.size - 2].totalReps
                    prev > 0 && latest.totalReps < prev * (1 - config.meaningfulDeclineFraction)
                }

            val highFatigue = (checkIn.fatigue ?: 0) >= config.highFatigueThreshold

            return BodyweightSignals(
                hasData = true,
                topReps = topReps,
                minReps = minReps,
                allAtTop = latest.atTop(),
                allAtLeastMin = latest.atLeastMin(),
                anyFailure = latest.failure(),
                effortAcceptable = effortAcceptable,
                consecutiveSuccesses = consecutiveSuccesses,
                consecutiveFailures = consecutiveFailures,
                outputDeclined = outputDeclined,
                highFatigue = highFatigue,
            )
        }
    }
}

private fun safetyBlocked(checkIn: ReadinessCheckIn): Boolean =
    checkIn.painReported || checkIn.injuryReported || checkIn.seriousSymptomReported

private fun maintain(
    reason: String,
    dimension: ProgressionDimension = ProgressionDimension.MAINTAIN,
    safety: ProgressionSafetyState = ProgressionSafetyState.OK,
) = ProgressionCandidate(
    recommendationType = ProgressionRecommendationType.MAINTAIN_PRESCRIPTION,
    dimension = dimension,
    summary = reason,
    fatigueCost = FatigueCost.NONE,
    safetyState = safety,
)

private fun requestData(reason: String) =
    ProgressionCandidate(
        recommendationType = ProgressionRecommendationType.REQUEST_MORE_DATA,
        dimension = ProgressionDimension.MAINTAIN,
        summary = reason,
        fatigueCost = FatigueCost.NONE,
        requiresMoreData = true,
    )

/**
 * Rep progression. Raises the target range once the athlete owns the top of it; holds
 * while they climb toward it; reduces (never below the floor) after real decline. Uses a
 * configurable rep increment and supports set-specific work through the prescription's
 * range (the model carries a single range, so the range itself is shifted).
 */
class RepProgressionCalculator
    @Inject
    constructor(private val config: BodyweightProgressionConfig) {
        constructor() : this(BodyweightProgressionConfig())

        fun evaluate(
            prescription: ExercisePrescription,
            sessions: List<SessionPerformance>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
        ): ProgressionCandidate {
            if (safetyBlocked(
                    checkIn,
                )
            ) {
                return maintain("Hold reps — recovery first", ProgressionDimension.REPS, ProgressionSafetyState.BLOCKED)
            }
            val s = BodyweightSignals.from(prescription, sessions, checkIn, config)
            if (!s.hasData) return requestData("No sessions logged for this exercise yet")

            return when {
                s.anyFailure && s.consecutiveFailures >= 2 ->
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.REDUCE_REPS,
                        dimension = ProgressionDimension.REPS,
                        summary = "Ease the target reps to rebuild clean sets",
                        evidence = listOf("${s.consecutiveFailures} recent sessions fell short"),
                        proposed = shiftRange(prescription, -config.repIncrement),
                        fatigueCost = FatigueCost.NONE,
                    )
                s.anyFailure -> maintain("Repeat the current reps before progressing", ProgressionDimension.REPS)
                s.allAtTop && s.effortAcceptable ->
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.INCREASE_REPS,
                        dimension = ProgressionDimension.REPS,
                        summary = "Raise the target range by ${config.repIncrement} rep(s)",
                        evidence = listOf("All working sets reached the top of the range"),
                        proposed = shiftRange(prescription, config.repIncrement),
                        fatigueCost = FatigueCost.LOW,
                    )
                s.allAtLeastMin ->
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.INCREASE_REPS,
                        dimension = ProgressionDimension.REPS,
                        summary = "Add a rep, working toward the top of the range",
                        evidence = listOf("Stable performance within the rep range"),
                        fatigueCost = FatigueCost.LOW,
                    )
                else -> maintain("Hold the current reps", ProgressionDimension.REPS)
            }
        }

        private fun shiftRange(
            p: ExercisePrescription,
            delta: Int,
        ): ExercisePrescription =
            p.copy(
                id = com.ascend.core.common.newId(),
                minimumReps = p.minimumReps?.let { max(1, it + delta) },
                maximumReps = p.maximumReps?.let { max(1, it + delta) },
                status = "PROPOSED",
                effectiveFrom = 0,
            )
    }

/**
 * Set progression. Adding a set demands **more** evidence than adding a rep — multiple
 * consecutive strong sessions, headroom below the max set count, and acceptable fatigue.
 * One easy session never adds a set. Removing a set is a safe response to real decline.
 */
class SetProgressionCalculator
    @Inject
    constructor(private val config: BodyweightProgressionConfig) {
        constructor() : this(BodyweightProgressionConfig())

        fun evaluate(
            prescription: ExercisePrescription,
            sessions: List<SessionPerformance>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
        ): ProgressionCandidate {
            if (safetyBlocked(
                    checkIn,
                )
            ) {
                return maintain("Hold volume — recovery first", ProgressionDimension.SETS, ProgressionSafetyState.BLOCKED)
            }
            val currentSets = prescription.targetSets ?: 1
            val s = BodyweightSignals.from(prescription, sessions, checkIn, config)
            if (!s.hasData) return requestData("No sessions logged yet")

            return when {
                s.consecutiveFailures >= 2 && currentSets > 1 ->
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.REMOVE_SET,
                        dimension = ProgressionDimension.SETS,
                        summary = "Drop a set to restore quality",
                        evidence = listOf("${s.consecutiveFailures} recent sessions fell short"),
                        proposed = withSets(prescription, currentSets - 1),
                        fatigueCost = FatigueCost.NONE,
                    )
                s.consecutiveSuccesses < config.requiredSessionsToAddSet ->
                    requestData(
                        "Adding a set needs ${config.requiredSessionsToAddSet} strong sessions " +
                            "(${s.consecutiveSuccesses} so far)",
                    )
                s.highFatigue ->
                    maintain("Hold volume — fatigue is elevated", ProgressionDimension.SETS)
                currentSets >= config.maxWorkingSets ->
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.LIGHT_SESSION,
                        dimension = ProgressionDimension.SETS,
                        summary = "At the set ceiling — add lighter accessory volume instead",
                        fatigueCost = FatigueCost.LOW,
                    )
                else ->
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.ADD_SET,
                        dimension = ProgressionDimension.SETS,
                        summary = "Add a working set",
                        evidence = listOf("${s.consecutiveSuccesses} strong sessions in a row"),
                        proposed = withSets(prescription, currentSets + 1),
                        fatigueCost = FatigueCost.HIGH,
                    )
            }
        }

        private fun withSets(
            p: ExercisePrescription,
            sets: Int,
        ): ExercisePrescription =
            p.copy(id = com.ascend.core.common.newId(), targetSets = max(1, sets), status = "PROPOSED", effectiveFrom = 0)
    }

/**
 * Rest progression. Rest is not a punishment metric: a reduction only counts as
 * progression when output stays stable, and heavy strength work may correctly keep or
 * lengthen rest. Declining output never triggers a reduction — it earns more rest, and
 * longer rest is never framed as a failure.
 */
class RestProgressionCalculator
    @Inject
    constructor(private val config: BodyweightProgressionConfig) {
        constructor() : this(BodyweightProgressionConfig())

        fun evaluate(
            prescription: ExercisePrescription,
            sessions: List<SessionPerformance>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
            isHeavyStrength: Boolean = false,
        ): ProgressionCandidate {
            if (safetyBlocked(checkIn)) return maintain("Keep your usual rest — recover fully", ProgressionDimension.REST)
            val currentRest = prescription.targetRestSeconds
            val s = BodyweightSignals.from(prescription, sessions, checkIn, config)
            if (!s.hasData || sessions.size < 2) return requestData("Need a couple of sessions to judge rest")

            return when {
                s.outputDeclined ->
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.INCREASE_REST,
                        dimension = ProgressionDimension.REST,
                        summary = "Take a little more rest to keep output up",
                        evidence = listOf("Recent output dipped — more recovery between sets helps"),
                        proposed = currentRest?.let { withRest(prescription, it + config.restIncrementSeconds) },
                        fatigueCost = FatigueCost.NONE,
                    )
                isHeavyStrength ->
                    maintain("Keep full rest for heavy strength work", ProgressionDimension.REST)
                currentRest == null || currentRest <= config.minimumRestSeconds ->
                    maintain("Rest is already efficient — hold it", ProgressionDimension.REST)
                s.allAtLeastMin && !s.anyFailure && s.effortAcceptable ->
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.REDUCE_REST,
                        dimension = ProgressionDimension.REST,
                        summary = "Trim rest by ${config.restDecrementSeconds}s while performance holds",
                        evidence = listOf("Output stable — density can increase"),
                        proposed = withRest(prescription, max(config.minimumRestSeconds, currentRest - config.restDecrementSeconds)),
                        fatigueCost = FatigueCost.MODERATE,
                    )
                else -> maintain("Hold your current rest", ProgressionDimension.REST)
            }
        }

        private fun withRest(
            p: ExercisePrescription,
            rest: Int,
        ): ExercisePrescription =
            p.copy(id = com.ascend.core.common.newId(), targetRestSeconds = rest, status = "PROPOSED", effectiveFrom = 0)
    }

/**
 * Assistance progression. Lowers band/machine assistance once the athlete owns the
 * current level for enough exposures; raises it again (a safe regression) after real
 * decline. When there is no assistance to reduce, it holds and defers to load/variation.
 */
class AssistanceProgressionCalculator
    @Inject
    constructor(private val config: BodyweightProgressionConfig) {
        constructor() : this(BodyweightProgressionConfig())

        fun evaluate(
            prescription: ExercisePrescription,
            sessions: List<SessionPerformance>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
        ): ProgressionCandidate {
            if (safetyBlocked(checkIn)) return maintain("Keep current assistance — recover first", ProgressionDimension.ASSISTANCE)
            val assistance = prescription.assistanceValue
            val s = BodyweightSignals.from(prescription, sessions, checkIn, config)
            if (!s.hasData) return requestData("No sessions logged yet")
            if (assistance == null || assistance <= 0.0) {
                return maintain("No assistance to reduce — progress load or variation instead", ProgressionDimension.ASSISTANCE)
            }

            return when {
                s.consecutiveFailures >= 2 ->
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.INCREASE_ASSISTANCE,
                        dimension = ProgressionDimension.ASSISTANCE,
                        summary = "Add a little assistance to keep clean reps",
                        evidence = listOf("${s.consecutiveFailures} recent sessions fell short"),
                        proposed = withAssistance(prescription, assistance + config.assistanceIncrementValue),
                        fatigueCost = FatigueCost.NONE,
                    )
                s.consecutiveSuccesses < config.requiredExposuresToReduceAssistance ->
                    requestData(
                        "Reducing assistance needs ${config.requiredExposuresToReduceAssistance} " +
                            "strong sessions (${s.consecutiveSuccesses} so far)",
                    )
                else -> {
                    val reduced = (assistance * (1 - config.assistanceReductionFraction)).coerceAtLeast(0.0)
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.REDUCE_ASSISTANCE,
                        dimension = ProgressionDimension.ASSISTANCE,
                        summary = "Reduce assistance toward unassisted work",
                        evidence = listOf("${s.consecutiveSuccesses} strong sessions at the current assistance"),
                        proposed = withAssistance(prescription, reduced),
                        fatigueCost = FatigueCost.MODERATE,
                    )
                }
            }
        }

        private fun withAssistance(
            p: ExercisePrescription,
            value: Double,
        ): ExercisePrescription =
            p.copy(id = com.ascend.core.common.newId(), assistanceValue = value, status = "PROPOSED", effectiveFrom = 0)
    }

/**
 * External-load progression. The endgame for a bodyweight movement the athlete has
 * mastered unassisted: once the variation supports load, assistance is gone, and the top
 * of the range is owned across several sessions, adding a little external load is the safe
 * next stimulus. Otherwise it holds and defers to reducing assistance or advancing the
 * variation first.
 */
class ExternalLoadProgressionCalculator
    @Inject
    constructor(private val config: BodyweightProgressionConfig) {
        constructor() : this(BodyweightProgressionConfig())

        fun evaluate(
            prescription: ExercisePrescription,
            sessions: List<SessionPerformance>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
            externalLoadSupported: Boolean = false,
        ): ProgressionCandidate {
            if (safetyBlocked(checkIn)) return maintain("Hold load — recover first", ProgressionDimension.EXTERNAL_LOAD)
            if (!externalLoadSupported) return maintain("This variation doesn't take external load", ProgressionDimension.EXTERNAL_LOAD)
            if ((prescription.assistanceValue ?: 0.0) > 0.0) {
                return maintain("Remove assistance before adding load", ProgressionDimension.EXTERNAL_LOAD)
            }
            val s = BodyweightSignals.from(prescription, sessions, checkIn, config)
            if (!s.hasData) return requestData("No sessions logged yet")

            return if (s.allAtTop && s.effortAcceptable && s.consecutiveSuccesses >= config.requiredExposuresToReduceAssistance) {
                ProgressionCandidate(
                    recommendationType = ProgressionRecommendationType.ADD_EXTERNAL_LOAD,
                    dimension = ProgressionDimension.EXTERNAL_LOAD,
                    summary = "Add light external load — bodyweight mastered",
                    evidence = listOf("${s.consecutiveSuccesses} strong sessions at the top of the range"),
                    fatigueCost = FatigueCost.HIGH,
                )
            } else {
                maintain("Own the top of the range before adding load", ProgressionDimension.EXTERNAL_LOAD)
            }
        }
    }

/**
 * Tempo & control progression. Only offered when the athlete consistently completes the
 * movement with acceptable effort, no safety flags, and established control of the
 * variation — tempo is never used to make a movement arbitrarily harder. Produces a
 * slower eccentric, a pause, or greater range of motion.
 */
class TempoProgressionCalculator
    @Inject
    constructor(private val config: BodyweightProgressionConfig) {
        constructor() : this(BodyweightProgressionConfig())

        fun evaluate(
            prescription: ExercisePrescription,
            sessions: List<SessionPerformance>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
            hasVariationControl: Boolean = true,
        ): ProgressionCandidate = candidates(prescription, sessions, checkIn, hasVariationControl).first()

        /** The tempo/control options, most conservative first (slower eccentric, pause, ROM). */
        fun candidates(
            prescription: ExercisePrescription,
            sessions: List<SessionPerformance>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
            hasVariationControl: Boolean = true,
        ): List<ProgressionCandidate> {
            if (safetyBlocked(
                    checkIn,
                )
            ) {
                return listOf(maintain("Hold tempo — recover first", ProgressionDimension.TEMPO, ProgressionSafetyState.BLOCKED))
            }
            val s = BodyweightSignals.from(prescription, sessions, checkIn, config)
            if (!s.hasData) return listOf(requestData("No sessions logged yet"))
            if (!hasVariationControl || !s.allAtLeastMin || s.anyFailure || !s.effortAcceptable) {
                return listOf(maintain("Build clean, controlled reps before adding tempo", ProgressionDimension.TEMPO))
            }
            return listOf(
                ProgressionCandidate(
                    recommendationType = ProgressionRecommendationType.SLOW_ECCENTRIC,
                    dimension = ProgressionDimension.TEMPO,
                    summary = "Slow the lowering phase for more control",
                    evidence = listOf("Consistent, controlled completion"),
                    fatigueCost = FatigueCost.MODERATE,
                ),
                ProgressionCandidate(
                    recommendationType = ProgressionRecommendationType.ADD_PAUSE,
                    dimension = ProgressionDimension.TEMPO,
                    summary = "Add a pause at the hardest position",
                    fatigueCost = FatigueCost.MODERATE,
                ),
                ProgressionCandidate(
                    recommendationType = ProgressionRecommendationType.INCREASE_RANGE_OF_MOTION,
                    dimension = ProgressionDimension.RANGE_OF_MOTION,
                    summary = "Increase range of motion",
                    fatigueCost = FatigueCost.MODERATE,
                ),
            )
        }
    }
