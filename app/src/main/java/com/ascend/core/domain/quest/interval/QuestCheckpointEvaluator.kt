package com.ascend.core.domain.quest.interval

import com.ascend.core.model.QuestCheckpoint
import javax.inject.Inject

data class CheckpointEvaluation(
    val checkpointId: String,
    val met: Boolean,
    val overdue: Boolean,
    val remaining: Double,
)

/**
 * Evaluates cumulative "by this time" checkpoints against the authoritative daily
 * progress. A checkpoint is met when cumulative progress reaches its target; overdue
 * when its due time has passed unmet. Checkpoints are cumulative and must increase.
 */
class QuestCheckpointEvaluator
    @Inject
    constructor() {
        fun evaluate(
            checkpoints: List<QuestCheckpoint>,
            dailyProgress: Double,
            now: Long,
        ): List<CheckpointEvaluation> =
            checkpoints.map { checkpoint ->
                val met = dailyProgress >= checkpoint.targetValue
                CheckpointEvaluation(
                    checkpointId = checkpoint.id,
                    met = met,
                    overdue = !met && now > checkpoint.dueAt,
                    remaining = (checkpoint.targetValue - dailyProgress).coerceAtLeast(0.0),
                )
            }

        /** Cumulative checkpoint targets must strictly increase in order. */
        fun isCumulativeValid(checkpoints: List<QuestCheckpoint>): Boolean =
            checkpoints.zipWithNext().all { (a, b) -> b.targetValue > a.targetValue }
    }
