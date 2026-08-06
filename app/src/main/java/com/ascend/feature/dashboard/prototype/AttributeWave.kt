package com.ascend.feature.dashboard.prototype

import androidx.compose.animation.core.Easing
import com.ascend.core.designsystem.motion.AscendVerbEasing

/*
 * CP3: the attribute-increase **center-out wave** — a thin circle that scales from the sigil centre
 * outward toward the ring structure and fades as it expands (restrained, not a flash). Each attribute
 * has its own wave personality (duration + easing) per the handoff choreography; the three the handoff
 * pins down are exact, the other two follow the same character.
 */
data class WaveProfile(
    val durationMs: Int,
    val easing: Easing,
)

object AttributeWaveCatalog {
    // Index 0..4 == Strength, Endurance, Agility, Discipline, Recovery (the medallion/attribute order):
    //   Strength   — firmer/faster, snap-overshoot
    //   Endurance  — longer sustained, ease-in-out
    //   Agility    — quick, snap
    //   Discipline — precise clean, expo-decelerate
    //   Recovery   — sustained, ease-in-out
    private val PROFILES =
        listOf(
            WaveProfile(420, AscendVerbEasing.lock),
            WaveProfile(950, AscendVerbEasing.charge),
            WaveProfile(460, AscendVerbEasing.lock),
            WaveProfile(520, AscendVerbEasing.assemble),
            WaveProfile(800, AscendVerbEasing.charge),
        )

    /** The wave that spreads to this fraction of the sigil radius — into the ring structure. */
    const val REACH_FRACTION = 0.74f

    fun forIndex(index: Int): WaveProfile = PROFILES.getOrElse(index) { PROFILES[3] }

    fun forRole(role: MedallionRole): WaveProfile = forIndex(role.ordinal.coerceIn(0, PROFILES.lastIndex))
}
