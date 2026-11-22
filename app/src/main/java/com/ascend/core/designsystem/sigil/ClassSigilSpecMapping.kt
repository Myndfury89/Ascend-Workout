package com.ascend.core.designsystem.sigil

/** The class key for a prototype variant (same names; Neutral = no class). */
fun StatusClassVariant.classKey(): ClassKey =
    when (this) {
        StatusClassVariant.NEUTRAL -> ClassKey.NEUTRAL
        StatusClassVariant.BERSERKER -> ClassKey.BERSERKER
        StatusClassVariant.MONK -> ClassKey.MONK
        StatusClassVariant.MAGE -> ClassKey.MAGE
    }

/** The data-driven sigil spec (geometry + palette) for a prototype variant. */
fun StatusClassVariant.sigilSpec(): ClassSigilSpec = ClassSigilCatalog.of(classKey())

/**
 * The prototype variant with the same name as a [ClassKey], or null for classes that have no Status
 * prototype variant yet (Assassin, Fighter, Ranger, Guardian).
 */
fun ClassKey.prototypeVariantOrNull(): StatusClassVariant? =
    when (this) {
        ClassKey.NEUTRAL -> StatusClassVariant.NEUTRAL
        ClassKey.BERSERKER -> StatusClassVariant.BERSERKER
        ClassKey.MONK -> StatusClassVariant.MONK
        ClassKey.MAGE -> StatusClassVariant.MAGE
        ClassKey.ASSASSIN, ClassKey.FIGHTER, ClassKey.RANGER, ClassKey.GUARDIAN -> null
    }

/**
 * The prototype variant that carries a [ClassKey] through [OrnateSigilState]. Classes without one
 * ride on NEUTRAL; the renderer reads [OrnateSigilState.classKey] for all class-specific drawing.
 */
fun ClassKey.statusVariant(): StatusClassVariant = prototypeVariantOrNull() ?: StatusClassVariant.NEUTRAL
