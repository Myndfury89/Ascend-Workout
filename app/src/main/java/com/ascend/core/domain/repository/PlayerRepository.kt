package com.ascend.core.domain.repository

import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import kotlinx.coroutines.flow.Flow

/** Bootstraps and exposes the local player profile (offline-first, no account required). */
interface PlayerRepository {
    /** Ensure a local profile + progress + stats exist. Safe to call repeatedly. */
    suspend fun ensureLocalPlayer(displayName: String = "Player"): String

    fun observeProgress(userId: String): Flow<PlayerProgress?>

    fun observeStats(userId: String): Flow<PlayerStats?>
}
