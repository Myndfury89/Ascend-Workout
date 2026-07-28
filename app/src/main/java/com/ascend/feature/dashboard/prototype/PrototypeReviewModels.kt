package com.ascend.feature.dashboard.prototype

/*
 * Debug-only review knobs for the Status prototype. None of this ships in production; it exists
 * to make the fake-data prototype reviewable at different speeds, entrance modes, effect levels,
 * and simulated device widths. All timing flows through the shared MotionSpec — there is no
 * prototype-only timing system.
 */

/**
 * How the screen enters. [EVERYDAY_OPEN] is the fast, low-ceremony open intended to become the
 * production default later (critical content ~400–700 ms, immediate interaction, minimal sigil
 * assembly). [MAJOR_EVENT] keeps the fuller cinematic beat for a meaningful moment.
 */
enum class EntranceMode(val label: String) {
    EVERYDAY_OPEN("Everyday open"),
    MAJOR_EVENT("Major event"),
}

/** Speed profile as a scalar over the shared motion tokens (via `MotionSpec.speedScale`). */
enum class MotionSpeed(val label: String, val scale: Float) {
    FAST("Fast", 0.65f),
    STANDARD("Standard", 1.0f),
    CINEMATIC("Cinematic", 1.35f),
}

/** Decorative-effect ceiling. Never changes hierarchy or final data — only ambient motion. */
enum class EffectsQuality(val label: String) {
    FULL("Full"),
    SIMPLIFIED("Simplified"),
    MINIMAL("Minimal"),
    ;

    fun toConfig(): EffectsConfig =
        when (this) {
            FULL -> EffectsConfig(particleQuality = 1f, sigilIdle = true, scan = true, glowPulses = true, scanQuality = 1f)
            SIMPLIFIED -> EffectsConfig(particleQuality = 0.4f, sigilIdle = true, scan = true, glowPulses = true, scanQuality = 0.5f)
            MINIMAL -> EffectsConfig(particleQuality = 0f, sigilIdle = false, scan = false, glowPulses = false, scanQuality = 0f)
        }
}

/** The resolved per-quality effect switches consumed by the composition. */
data class EffectsConfig(
    val particleQuality: Float,
    val sigilIdle: Boolean,
    val scan: Boolean,
    val glowPulses: Boolean,
    val scanQuality: Float,
)

/** Simulated device-width category for legibility review; null width fills the container. */
enum class DeviceWidth(val label: String, val widthDp: Int?) {
    COMPACT("Small phone", 340),
    MEDIUM("Normal phone", 400),
    EXPANDED("Large phone", 520),
    FILL("Fill", null),
}
