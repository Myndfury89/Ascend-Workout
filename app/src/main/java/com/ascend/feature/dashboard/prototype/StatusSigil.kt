package com.ascend.feature.dashboard.prototype

import androidx.compose.ui.graphics.Color

/*
 * The class accent identity. The old simple sigil composable was superseded by the Original
 * Ornate Sigil System (see OrnateSigil.kt); this enum remains as the per-class accent source used
 * by the ornate sigil, the energy frame tints, and the panel.
 */
enum class StatusSigilVariant(val core: Color) {
    NEUTRAL(Color(0xFF8AA0B8)),
    BERSERKER(Color(0xFFE8A33D)),
    MONK(Color(0xFF3FD9C7)),
    MAGICIAN(Color(0xFF9B8CFF)),
    ;

    companion object {
        fun of(variant: StatusClassVariant): StatusSigilVariant =
            when (variant) {
                StatusClassVariant.NEUTRAL -> NEUTRAL
                StatusClassVariant.BERSERKER -> BERSERKER
                StatusClassVariant.MONK -> MONK
                StatusClassVariant.MAGICIAN -> MAGICIAN
            }
    }
}

/**
 * The class accent, with the refined-prototype warm treatment. When [warm] is set, **Berserker reads
 * as a controlled red-orange** ([StatusPalette.ember], handoff hue ~25) instead of amber — Monk and
 * Magician are unchanged. [warm] is off by default, so production keeps the current accents.
 */
fun classAccent(
    variant: StatusClassVariant,
    warm: Boolean,
): Color = if (warm && variant == StatusClassVariant.BERSERKER) StatusPalette.ember else StatusSigilVariant.of(variant).core
