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

/**
 * Ambient sigil rotation pace for review. [REVIEW] is the fast, review-friendly period; [AMBIENT]
 * is the restrained real-world drift the handoff calls for (150–260s/rev) so the reviewer can feel
 * how quiet it actually is in production.
 */
enum class RotationPace(val label: String, val periodMs: Int) {
    REVIEW("Review 24s", 24_000),
    AMBIENT("Ambient 200s", 200_000),
}

/** Whether the review viewport draws its ceremonial energy frame (FRAMED) or a bare canvas. */
enum class FrameMode(val label: String) {
    FRAMED("Framed"),
    CANVAS("Canvas"),
}

/** The four reviewable Skills for the Skill dock (full unlock-menu choreography is CP4). */
enum class PrototypeSkill(
    val displayName: String,
    val category: String,
    val effect: String,
) {
    PERCEPTION("Perception", "Awareness", "Surfaces hidden training insight"),
    STRENGTH_BOOST("Strength Boost", "Power", "Amplifies force output"),
    BREATH_CONTROL("Breath Control", "Endurance", "Sustains output under load"),
    BODY_AWARENESS("Body Awareness", "Control", "Sharpens movement precision"),
}

/** Simulated device-width category for legibility review; null width fills the container. */
enum class DeviceWidth(val label: String, val widthDp: Int?) {
    COMPACT("Small phone", 340),
    MEDIUM("Normal phone", 400),
    EXPANDED("Large phone", 520),
    FILL("Fill", null),
}
