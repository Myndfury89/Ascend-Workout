package com.ascend.core.domain.quest.interval

import com.ascend.core.database.dao.QuestIntervalDao
import com.ascend.core.model.AdaptiveIntervalContext
import com.ascend.core.model.AdaptiveIntervalRecommendation
import com.ascend.core.model.FutureInterval
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.RedistributionPreference
import com.ascend.core.model.TrainingReadinessState
import javax.inject.Inject
import kotlin.math.roundToInt

/** Runtime signals + limits the persisted plan can't know on its own. */
data class AdaptiveIntervalParams(
    val dailyTarget: Int,
    val now: Long,
    val dayEndMillis: Long,
    val maximumIntervalTarget: Int = Int.MAX_VALUE,
    val minimumSetSize: Int = 1,
    val maximumSetSize: Int = Int.MAX_VALUE,
    val quietHours: List<Pair<Long, Long>> = emptyList(),
    val fatigue: Int? = null,
    val readinessState: TrainingReadinessState = TrainingReadinessState.MAINTAIN,
    val safetyState: ProgressionSafetyState = ProgressionSafetyState.OK,
)

/**
 * Bridges the **persisted** interval plan to the pure redistribution engine: it reads the
 * schedule + intervals, derives the daily progress, leftover, and future intervals, asks the
 * engine for a recommendation, and (only when the engine actually auto-applied a safe change)
 * writes the new interval targets back. Completing the daily total stays authoritative — this
 * never changes the daily objective.
 */
class AdaptiveIntervalRedistributionUseCase
    @Inject
    constructor(
        private val dao: QuestIntervalDao,
        private val engine: QuestIntervalRedistributionEngine,
    ) {
        suspend fun recommend(
            questId: String,
            params: AdaptiveIntervalParams,
        ): AdaptiveIntervalRecommendation {
            val schedule = dao.getSchedule(questId) ?: return maintain()
            val intervals = dao.getIntervals(questId)

            val currentDailyProgress = intervals.sumOf { it.currentValue }.roundToInt()
            val leftover =
                intervals
                    .filter { it.scheduledEnd < params.now && it.currentValue < it.targetValue }
                    .sumOf { (it.targetValue - it.currentValue) }
                    .roundToInt()
            val futureIntervals =
                intervals
                    .filter { it.scheduledStart >= params.now || (it.scheduledEnd >= params.now && it.currentValue < it.targetValue) }
                    .map { FutureInterval(it.id, it.targetValue.roundToInt(), it.scheduledStart) }

            val context =
                AdaptiveIntervalContext(
                    dailyTarget = params.dailyTarget,
                    currentDailyProgress = currentDailyProgress,
                    leftover = leftover,
                    futureIntervals = futureIntervals,
                    timeRemainingMillis = (params.dayEndMillis - params.now).coerceAtLeast(0),
                    quietHours = params.quietHours,
                    minimumSetSize = params.minimumSetSize,
                    maximumSetSize = params.maximumSetSize,
                    maximumIntervalTarget = params.maximumIntervalTarget,
                    fatigue = params.fatigue,
                    readinessState = params.readinessState,
                    safetyState = params.safetyState,
                    redistributionPreference =
                        runCatching {
                            RedistributionPreference.valueOf(schedule.redistributionPreference)
                        }.getOrDefault(RedistributionPreference.EVEN),
                    autoSafeAdaptationEnabled = schedule.adaptiveRedistributionEnabled,
                    now = params.now,
                )

            val recommendation = engine.adaptiveRecommend(context)
            if (recommendation.autoApplied) applyTargets(recommendation)
            return recommendation
        }

        /** Writes the proposed interval targets back. Used on auto-apply or explicit confirmation. */
        suspend fun applyTargets(recommendation: AdaptiveIntervalRecommendation) {
            recommendation.proposedTargets.forEach { (intervalId, target) ->
                dao.updateIntervalTarget(intervalId, target.toDouble())
            }
        }

        private fun maintain() =
            AdaptiveIntervalRecommendation(
                action = com.ascend.core.model.AdaptiveIntervalAction.MAINTAIN_PLAN,
                summary = "No interval schedule to adapt",
                reasons = emptyList(),
                requiresConfirmation = false,
            )
    }
