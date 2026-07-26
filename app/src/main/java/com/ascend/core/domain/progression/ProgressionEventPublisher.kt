package com.ascend.core.domain.progression

import com.ascend.core.domain.repository.ClassRepository
import com.ascend.core.domain.repository.ProgressionEventRepository
import com.ascend.core.model.ClassRewardLine
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import com.ascend.core.model.ProgressionSnapshot
import com.ascend.core.model.Rank
import com.ascend.core.model.RewardBreakdown
import com.ascend.core.model.XpSourceType
import javax.inject.Inject

/**
 * Bridges an earned completion to the persisted ProgressionEventQueue. Called inside
 * the completion transaction so the animation script is enqueued exactly‑once,
 * atomically with the earning — the presentation is then decoupled and replayable.
 */
class ProgressionEventPublisher
    @Inject
    constructor(
        private val eventRepository: ProgressionEventRepository,
        private val eventFactory: ProgressionEventFactory,
        private val classRepository: ClassRepository,
        private val levelCalculator: LevelCalculator,
    ) {
        suspend fun publish(
            userId: String,
            sourceType: XpSourceType,
            sourceId: String,
            label: String?,
            playerBefore: ProgressionSnapshot,
            playerAfter: ProgressionSnapshot,
            breakdown: RewardBreakdown,
        ) {
            val classChanges =
                listOfNotNull(breakdown.primaryClass, breakdown.secondaryClass)
                    .map { classEventInput(userId, it) }

            val events =
                eventFactory.build(
                    userId = userId,
                    sourceType = sourceType,
                    sourceId = sourceId,
                    before = playerBefore,
                    after = playerAfter,
                    classChanges = classChanges,
                    label = label,
                )
            eventRepository.enqueue(events)
        }

        /** Reconstructs a class's before/after from post‑award totals and the reward line. */
        private suspend fun classEventInput(
            userId: String,
            line: ClassRewardLine,
        ): ClassEventInput {
            val classXpTo = classRepository.totalClassXp(userId, line.classId)
            val classXpFrom = (classXpTo - line.classXp).coerceAtLeast(0)
            val proficiencyTo = classRepository.totalProficiency(userId, line.uniqueProficiencyKey)
            val proficiencyFrom = (proficiencyTo - line.uniqueProficiencyGain).coerceAtLeast(0)
            return ClassEventInput(
                classId = line.classId,
                className = line.className,
                classXpFrom = classXpFrom,
                classXpTo = classXpTo,
                classLevelFrom = levelCalculator.resolve(classXpFrom).level,
                classLevelTo = line.newClassLevel,
                proficiencyKey = line.uniqueProficiencyKey,
                proficiencyName = line.uniqueProficiencyName,
                proficiencyFrom = proficiencyFrom,
                proficiencyTo = proficiencyTo,
            )
        }

        companion object {
            /** A [ProgressionSnapshot] from raw progress/stats (baseline when null). */
            fun snapshot(
                progress: PlayerProgress?,
                stats: PlayerStats?,
            ): ProgressionSnapshot =
                ProgressionSnapshot(
                    level = progress?.level ?: 1,
                    lifetimeXp = progress?.lifetimeXp ?: 0L,
                    rank = progress?.rank ?: Rank.INITIATE,
                    attributes = stats?.asMap ?: emptyMap(),
                )
        }
    }
