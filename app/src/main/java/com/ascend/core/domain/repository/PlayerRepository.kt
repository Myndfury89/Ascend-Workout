package com.ascend.core.domain.repository

import com.ascend.core.common.WeightUnit
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import kotlinx.coroutines.flow.Flow

/** Bootstraps and exposes the local player profile (offline-first, no account required). */
interface PlayerRepository {
    /** Ensure a local profile + progress + stats exist. Safe to call repeatedly. */
    suspend fun ensureLocalPlayer(displayName: String = "Player"): String

    /** The profile's display name (falls back to a neutral default if unset). */
    suspend fun displayName(userId: String): String

    fun observeProgress(userId: String): Flow<PlayerProgress?>

    fun observeStats(userId: String): Flow<PlayerStats?>

    /** The user's chosen display weight unit (canonical storage stays kilograms). */
    suspend fun weightUnit(userId: String): WeightUnit

    fun observeWeightUnit(userId: String): Flow<WeightUnit>

    /** Persist a new display weight unit. Never rewrites any stored weight. */
    suspend fun setWeightUnit(
        userId: String,
        unit: WeightUnit,
    )

    /**
     * The cosmetic "Your Ascended" avatar body base as a raw token ("MALE"/"FEMALE"), or null when
     * the player has not chosen yet. Presentation-only; independent of physiological data.
     */
    fun observeAvatarBodyBase(userId: String): Flow<String?>

    /** Persist the cosmetic avatar body base. Never affects any gameplay/progression data. */
    suspend fun setAvatarBodyBase(
        userId: String,
        value: String?,
    )
}
