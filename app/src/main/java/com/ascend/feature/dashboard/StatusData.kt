package com.ascend.feature.dashboard

import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import kotlinx.coroutines.flow.Flow

/**
 * Read side of the Status screen's **domain state** layer. In the prototype this is
 * backed by fake, in‑memory data; production will back it with the real
 * `ProgressionRepository` without the ViewModel changing.
 */
interface StatusDataSource {
    val hunterName: String

    /** Resolve (and cache) the local user id backing this screen. */
    suspend fun userId(): String

    fun observeProgress(): Flow<PlayerProgress>

    fun observeStats(): Flow<PlayerStats>
}

/** The fake completions the prototype can trigger. */
enum class SimulatedCompletion(val label: String) {
    LIGHT_QUEST("Trial of Focus"),
    HEAVY_QUEST("Ascension Trial"),
}

/**
 * Stands in for a real earning transaction. Like production `completeQuest`, a
 * simulate call **atomically updates the domain and enqueues the presentation
 * events** — so the ViewModel only ever observes (domain + pending queue) and never
 * knows whether the earning was real or simulated.
 */
interface StatusSimulator {
    suspend fun simulate(kind: SimulatedCompletion)

    /** Restore the baseline hunter and drain any pending events (prototype only). */
    suspend fun reset()
}
