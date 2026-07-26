package com.ascend.feature.dashboard

import com.ascend.core.common.newId
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.ProgressionEventFactory
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.domain.repository.ProgressionEventRepository
import com.ascend.core.model.AttributeType
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import com.ascend.core.model.ProgressionSnapshot
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prototype‑only fake for the Status motion system. Holds an in‑memory hunter and,
 * on [simulate], plays the role of a real earning transaction: it advances the
 * (fake) domain state **and** enqueues the resulting [ProgressionEvent]s into the
 * real, persisted [ProgressionEventRepository]. The ViewModel then observes the
 * domain + the queue exactly as it will in production — only the trigger is fake.
 */
@Singleton
class FakeStatusData
    @Inject
    constructor(
        private val playerRepository: PlayerRepository,
        private val eventRepository: ProgressionEventRepository,
        private val eventFactory: ProgressionEventFactory,
        private val levelCalculator: LevelCalculator,
        private val rankCalculator: RankCalculator,
    ) : StatusDataSource, StatusSimulator {
        override val hunterName: String = HUNTER_NAME

        private val mutex = Mutex()

        private var lifetimeXp = BASELINE_XP
        private var attributes = BASELINE_ATTRIBUTES.toMutableMap()

        private val progressFlow = MutableStateFlow(computeProgress(PLACEHOLDER_ID))
        private val statsFlow = MutableStateFlow(computeStats(PLACEHOLDER_ID))

        @Volatile private var cachedUserId: String? = null

        override suspend fun userId(): String =
            cachedUserId ?: mutex.withLock {
                cachedUserId ?: playerRepository.ensureLocalPlayer(HUNTER_NAME).also {
                    cachedUserId = it
                    // Re‑stamp the exposed state with the real id now that we have it.
                    progressFlow.value = computeProgress(it)
                    statsFlow.value = computeStats(it)
                }
            }

        override fun observeProgress(): Flow<PlayerProgress> = progressFlow.asStateFlow()

        override fun observeStats(): Flow<PlayerStats> = statsFlow.asStateFlow()

        override suspend fun simulate(kind: SimulatedCompletion) {
            val uid = userId()
            mutex.withLock {
                val before = snapshot()
                applyDeltas(kind)
                val after = snapshot()

                progressFlow.value = computeProgress(uid)
                statsFlow.value = computeStats(uid)

                val events =
                    eventFactory.build(
                        userId = uid,
                        sourceType = XpSourceType.QUEST_COMPLETION,
                        // A fresh id per tap so repeated simulations never collide on the
                        // exactly‑once (batchId, sequence) guard.
                        sourceId = newId(),
                        before = before,
                        after = after,
                        label = kind.label,
                    )
                eventRepository.enqueue(events)
            }
        }

        override suspend fun reset() {
            val uid = userId()
            mutex.withLock {
                eventRepository.markConsumed(eventRepository.getPending(uid).map { it.id })
                lifetimeXp = BASELINE_XP
                attributes = BASELINE_ATTRIBUTES.toMutableMap()
                progressFlow.value = computeProgress(uid)
                statsFlow.value = computeStats(uid)
            }
        }

        private fun applyDeltas(kind: SimulatedCompletion) {
            when (kind) {
                SimulatedCompletion.LIGHT_QUEST -> {
                    lifetimeXp += 350
                    attributes.add(AttributeType.STRENGTH, 12)
                    attributes.add(AttributeType.DISCIPLINE, 10)
                }
                SimulatedCompletion.HEAVY_QUEST -> {
                    lifetimeXp += 900
                    attributes.add(AttributeType.STRENGTH, 45)
                    attributes.add(AttributeType.ENDURANCE, 15)
                    attributes.add(AttributeType.DISCIPLINE, 20)
                }
            }
        }

        private fun snapshot(): ProgressionSnapshot {
            val state = levelCalculator.resolve(lifetimeXp)
            return ProgressionSnapshot(
                level = state.level,
                lifetimeXp = lifetimeXp,
                rank = rankCalculator.rankFor(state.level, lifetimeXp),
                attributes = attributes.toMap(),
            )
        }

        private fun computeProgress(uid: String): PlayerProgress {
            val state = levelCalculator.resolve(lifetimeXp)
            return PlayerProgress(
                userId = uid,
                level = state.level,
                currentLevelXp = state.currentLevelXp,
                xpToNextLevel = state.xpToNextLevel,
                lifetimeXp = lifetimeXp,
                rank = rankCalculator.rankFor(state.level, lifetimeXp),
                activeStreak = BASELINE_STREAK,
                longestStreak = BASELINE_STREAK,
                totalWorkouts = 0,
                totalQuests = 0,
                totalExpeditions = 0,
            )
        }

        private fun computeStats(uid: String): PlayerStats =
            PlayerStats(
                userId = uid,
                strength = attributes.getValue(AttributeType.STRENGTH),
                endurance = attributes.getValue(AttributeType.ENDURANCE),
                agility = attributes.getValue(AttributeType.AGILITY),
                discipline = attributes.getValue(AttributeType.DISCIPLINE),
                recovery = attributes.getValue(AttributeType.RECOVERY),
            )

        private fun MutableMap<AttributeType, Long>.add(
            attribute: AttributeType,
            amount: Long,
        ) {
            this[attribute] = (this[attribute] ?: 0L) + amount
        }

        private companion object {
            const val HUNTER_NAME = "Ascendant"
            const val PLACEHOLDER_ID = "prototype"

            // ~500 XP below the level‑6 threshold (4648): a light gain stays in level,
            // a heavy gain crosses into a level‑up (and, with it, a rank‑up).
            const val BASELINE_XP = 4_148L
            const val BASELINE_STREAK = 6

            val BASELINE_ATTRIBUTES: Map<AttributeType, Long> =
                mapOf(
                    AttributeType.STRENGTH to 48L,
                    AttributeType.ENDURANCE to 34L,
                    AttributeType.AGILITY to 26L,
                    AttributeType.DISCIPLINE to 61L,
                    AttributeType.RECOVERY to 20L,
                )
        }
    }
