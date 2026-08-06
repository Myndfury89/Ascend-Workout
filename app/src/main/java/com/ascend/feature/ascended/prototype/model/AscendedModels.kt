package com.ascend.feature.ascended.prototype.model

import com.ascend.core.model.AttributeType

/*
 * Fake, in-memory state for the "Your Ascended" character-avatar prototype. Nothing here touches
 * repositories, the ProgressionEventQueue, physiology, or real reward logic — every value is
 * deterministic fake data so the male/female mannequin systems and their future class / attribute /
 * Skill / aura / equipment overlays can be reviewed in isolation before any production wiring.
 *
 * IMPORTANT: [BodyBase] is a COSMETIC presentation choice for the avatar. It is deliberately NOT
 * derived from the optional physiological `physiologySex` captured at onboarding (a protected field
 * that may be PREFER_NOT_TO_ANSWER / NOT_SET and is walled off from gameplay). CP1 exposes it as a
 * simple review toggle; production sourcing/persistence is a separate, deferred decision.
 */

/** The two mannequin body bases. A cosmetic presentation choice — never a physiological conclusion. */
enum class BodyBase { MALE, FEMALE }

/** Visual evolution stage of the character. CP1 renders [BASE] only. */
enum class EvolutionStage(val displayName: String) {
    BASE("Newly Awakened"),
    EARLY_GROWTH("Early Growth"),
    ADVANCED("Advanced"),
    MASTERED("Mastered"),
}

/**
 * The seven class directions. Only [production] classes have real catalogs today
 * (berserker/monk/magician); the other four are shown as future directions. The prototype will give
 * all seven fake body/aura/equipment previews in later checkpoints.
 */
enum class AscendedClass(
    val id: String,
    val displayName: String,
    val production: Boolean,
) {
    BERSERKER("berserker", "Berserker", true),
    MONK("monk", "Monk", true),
    MAGICIAN("magician", "Magician", true),
    ASSASSIN("assassin", "Assassin", false),
    FIGHTER("fighter", "Fighter", false),
    RANGER("ranger", "Ranger", false),
    GUARDIAN("guardian", "Guardian", false),
}

/**
 * An individually addressable muscle-group region. Left/right groups are represented once here and
 * carried by [RegionSide]; central groups use [RegionSide.CENTER]. This is intentionally a starting
 * set of major groups — finer subdivisions (triceps long head, brachioradialis, serratus, sartorius)
 * are layered in during later checkpoints without reshaping this enum.
 */
enum class MuscleRegionId {
    HEAD,
    NECK,
    STERNUM,
    ABS_UPPER,
    ABS_LOWER,
    PELVIS,
    TRAPEZIUS,
    DELTOID,
    PECTORAL,
    OBLIQUE,
    UPPER_ARM,
    FOREARM,
    HAND,
    HIP,
    THIGH,
    KNEE,
    CALF,
    FOOT,
}

/** Which side of the figure a region belongs to. */
enum class RegionSide { CENTER, LEFT, RIGHT }

/**
 * Anchor points where class modifiers, equipment overlays, and Skill effects attach. They differ by
 * [BodyBase] (via the body proportions), so switching the base moves the registration points too —
 * exactly what CP1's male/female toggle must do (without touching class/attributes/progression).
 */
enum class AttachPoint {
    HEAD,
    CHEST_CENTER,
    SHOULDER_LEFT,
    SHOULDER_RIGHT,
    HAND_LEFT,
    HAND_RIGHT,
    WAIST,
    FOOT_LEFT,
    FOOT_RIGHT,
}

/**
 * The full fake character state the viewport renders. CP1 uses [FakeAscended.base] — a neutral base
 * with no class, no equipment, no aura, no Skill levels; only [bodyBase] and [reducedMotion] vary.
 */
data class AscendedState(
    val bodyBase: BodyBase,
    val ascendedClass: AscendedClass?,
    val stage: EvolutionStage,
    val attributes: Map<AttributeType, Int>,
    val skillLevels: Map<String, Int>,
    val auraIntensity: Float,
    val equipmentTier: Int,
    val reducedMotion: Boolean,
) {
    /** A visible, character-defining aura. Base state has none. */
    val hasStrongAura: Boolean get() = auraIntensity > STRONG_AURA_THRESHOLD

    /** Any earned visual equipment. Base state has none. */
    val hasEquipment: Boolean get() = equipmentTier > 0

    /** True when a class silhouette modifier should be applied. Base state is class-neutral. */
    val hasClassIdentity: Boolean get() = ascendedClass != null && stage != EvolutionStage.BASE

    companion object {
        const val STRONG_AURA_THRESHOLD = 0.15f
    }
}

/** Whether ambient motion (atmosphere drift, particles, floor-sigil rotation, aura pulse) may run. */
fun ambientRunning(state: AscendedState): Boolean = !state.reducedMotion

/** Deterministic fake states for the prototype. No repositories, no physiology, no randomness. */
object FakeAscended {
    /** The neutral, newly-awakened base: no class, equipment, aura, or Skill progression. */
    fun base(bodyBase: BodyBase): AscendedState =
        AscendedState(
            bodyBase = bodyBase,
            ascendedClass = null,
            stage = EvolutionStage.BASE,
            attributes = AttributeType.entries.associateWith { 0 },
            skillLevels = emptyMap(),
            auraIntensity = 0f,
            equipmentTier = 0,
            reducedMotion = false,
        )
}
