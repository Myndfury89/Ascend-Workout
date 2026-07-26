package com.ascend.core.domain.repository

import com.ascend.core.model.ProgressionEvent
import kotlinx.coroutines.flow.Flow

/**
 * The ProgressionEventQueue. Owns the persisted stream of presentation events that
 * sit *between* the earning transaction and the animation. Enqueue is exactly‑once
 * per (batchId, sequence); pending events are drained by whatever is on screen and
 * then marked consumed, so a reward survives app death and replays exactly once.
 */
interface ProgressionEventRepository {
    /** Persist a batch; already‑enqueued events (same batchId+sequence) are ignored. */
    suspend fun enqueue(events: List<ProgressionEvent>)

    /** Pending (unconsumed) events for the user, in play order. */
    fun observePending(userId: String): Flow<List<ProgressionEvent>>

    suspend fun getPending(userId: String): List<ProgressionEvent>

    /** Mark events as consumed once their animation has played. */
    suspend fun markConsumed(eventIds: List<String>)
}
