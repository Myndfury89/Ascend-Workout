package com.ascend.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.LevelState
import com.ascend.core.domain.repository.ClassRepository
import com.ascend.core.domain.repository.ProgressionEventRepository
import com.ascend.core.model.AttributeType
import com.ascend.core.model.PlayerClassSelection
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import com.ascend.core.model.ProgressionEvent
import com.ascend.core.model.Rank
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatusPhase { LOADING, READY }

/** Domain‑state layer: the final, authoritative progression facts. */
data class StatusDomain(
    val level: Int,
    val rank: Rank,
    val lifetimeXp: Long,
    val currentLevelXp: Long,
    val xpToNextLevel: Long,
    val progressFraction: Float,
    val attributes: Map<AttributeType, Long>,
)

data class StatusUiState(
    val phase: StatusPhase = StatusPhase.LOADING,
    val hunterName: String = "",
    val reducedMotion: Boolean = false,
    val domain: StatusDomain? = null,
    // Presentation‑event layer: the next batch of events awaiting animation (empty = none).
    val pendingBatch: List<ProgressionEvent> = emptyList(),
    // Bumped to replay the entrance sequence.
    val entranceKey: Int = 0,
    val isSimulating: Boolean = false,
    // Real class/specialization facts for the ornate composition (neutral until a class is chosen).
    val classInfo: StatusClassInfo = StatusClassInfo.NEUTRAL,
)

/**
 * Orchestrates the Status screen's three state layers without owning the pixels:
 *
 *  - **domain state** — collected from [StatusDataSource].
 *  - **presentation‑event state** — the persisted [ProgressionEventRepository]
 *    queue, drained one batch at a time; the animation layer marks a batch played.
 *  - **animation state** lives in the composable (Animatables), driven by these.
 *
 * The ViewModel never touches durations or easing — it only says *what* changed and
 * *what's next*, so the same VM works with real or simulated earnings.
 */
@HiltViewModel
class StatusMotionViewModel
    @Inject
    constructor(
        private val dataSource: StatusDataSource,
        private val simulator: StatusSimulator,
        private val eventRepository: ProgressionEventRepository,
        private val levelCalculator: LevelCalculator,
        private val classRepository: ClassRepository,
    ) : ViewModel() {
        /** Resolve any lifetime‑XP value into a level state so the bar can sweep the
         *  curve continuously and level‑ups fall out for free during animation. */
        fun levelState(lifetimeXp: Long): LevelState = levelCalculator.resolve(lifetimeXp.coerceAtLeast(0))

        private val domain = MutableStateFlow<StatusDomain?>(null)
        private val pending = MutableStateFlow<List<ProgressionEvent>>(emptyList())
        private val reducedMotion = MutableStateFlow(false)
        private val entranceKey = MutableStateFlow(0)
        private val simulating = MutableStateFlow(false)
        private val classInfo = MutableStateFlow(StatusClassInfo.NEUTRAL)

        val uiState: StateFlow<StatusUiState> =
            combine(domain, pending, reducedMotion, entranceKey, simulating) {
                    domainState, pendingBatch, reduced, entrance, isSimulating ->
                StatusUiState(
                    phase = if (domainState == null) StatusPhase.LOADING else StatusPhase.READY,
                    hunterName = dataSource.hunterName,
                    reducedMotion = reduced,
                    domain = domainState,
                    pendingBatch = pendingBatch,
                    entranceKey = entrance,
                    isSimulating = isSimulating,
                )
            }.combine(classInfo) { state, ci -> state.copy(classInfo = ci) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusUiState())

        init {
            viewModelScope.launch {
                val uid = dataSource.userId()
                launch {
                    combine(dataSource.observeProgress(), dataSource.observeStats(), ::toDomain)
                        .collect { domain.value = it }
                }
                launch {
                    eventRepository.observePending(uid).collect { events ->
                        pending.value = firstBatch(events)
                    }
                }
                launch {
                    // Re-resolve class progress whenever the selection OR any earning ticks player
                    // progress (class XP is gained on the same completions), so the class ring and
                    // bars stay live without a dedicated class-progress flow.
                    combine(classRepository.observeSelection(uid), dataSource.observeProgress()) { sel, _ -> sel }
                        .collect { classInfo.value = resolveClassInfo(uid, it) }
                }
            }
        }

        private suspend fun resolveClassInfo(
            userId: String,
            selection: PlayerClassSelection,
        ): StatusClassInfo {
            val primaryId = selection.primaryClassId ?: return StatusClassInfo.NEUTRAL
            val primary = classRepository.classProgress(userId, primaryId)
            val secondary = selection.secondaryClassId?.let { classRepository.classProgress(userId, it) }
            return StatusClassInfo(
                variant = StatusComposition.variantOf(primaryId),
                classLevel = primary.classLevel,
                classXpInLevel = primary.currentLevelXp.clampToInt(),
                classXpForLevel = primary.xpToNextLevel.clampToInt(),
                uniqueProficiency = primary.uniqueProficiency.clampToInt(),
                secondaryVariant = selection.secondaryClassId?.let { StatusComposition.variantOf(it) },
                secondaryXpInLevel = secondary?.currentLevelXp?.clampToInt(),
                secondaryXpForLevel = secondary?.xpToNextLevel?.clampToInt(),
            )
        }

        fun simulate(kind: SimulatedCompletion) =
            viewModelScope.launch {
                simulating.value = true
                try {
                    simulator.simulate(kind)
                } finally {
                    simulating.value = false
                }
            }

        fun reset() =
            viewModelScope.launch {
                simulator.reset()
                entranceKey.value += 1
            }

        fun replayEntrance() {
            entranceKey.value += 1
        }

        fun setReducedMotion(value: Boolean) {
            reducedMotion.value = value
        }

        /** Called by the animation layer once a batch has finished playing. */
        fun onBatchPlayed(eventIds: List<String>) =
            viewModelScope.launch {
                eventRepository.markConsumed(eventIds)
            }

        private fun toDomain(
            progress: PlayerProgress,
            stats: PlayerStats,
        ): StatusDomain =
            StatusDomain(
                level = progress.level,
                rank = progress.rank,
                lifetimeXp = progress.lifetimeXp,
                currentLevelXp = progress.currentLevelXp,
                xpToNextLevel = progress.xpToNextLevel,
                progressFraction = progress.progressFraction,
                attributes = stats.asMap,
            )

        /** The oldest unconsumed batch, in play order. Events arrive pre‑sorted. */
        private fun firstBatch(events: List<ProgressionEvent>): List<ProgressionEvent> {
            val head = events.firstOrNull() ?: return emptyList()
            return events.filter { it.batchId == head.batchId }.sortedBy { it.sequence }
        }
    }
