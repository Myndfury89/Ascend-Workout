package com.ascend.feature.ascended.model

/*
 * Fake, in-memory state for the image-driven "Your Ascended" character prototype. The figures are
 * REAL imported artwork (per-class, per-body-base grayscale images) displayed as drawables — not
 * procedurally drawn. Progression, aura, and Skill effects are layered ON TOP of the art. Everything
 * here is deterministic fake state: no repositories, no physiology, no schema.
 */

/** The two body bases. A cosmetic presentation choice — never derived from physiological data. */
enum class BodyBase(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
}

/** Visual evolution stage; higher stages prefer a richer art variant when one is supplied. */
enum class EvolutionStage(
    val displayName: String,
    val slug: String,
) {
    BASE("Base", "base"),
    EARLY_GROWTH("Early", "early"),
    ADVANCED("Advanced", "advanced"),
    MASTERED("Mastered", "mastered"),
}

/** The seven class directions. Only [production] classes have real catalogs (berserker/monk/mage). */
enum class AscendedClass(
    val id: String,
    val displayName: String,
    val production: Boolean,
) {
    BERSERKER("berserker", "Berserker", true),
    MONK("monk", "Monk", true),
    MAGE("mage", "Mage", true),
    ASSASSIN("assassin", "Assassin", false),
    FIGHTER("fighter", "Fighter", false),
    RANGER("ranger", "Ranger", false),
    GUARDIAN("guardian", "Guardian", false),
}
