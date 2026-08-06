package com.ascend.feature.dashboard.prototype

import androidx.compose.ui.graphics.Color

/*
 * The prototype's layered energy palette. Deliberately not a flat single-colour purple: the
 * outer frame carries the strongest saturated energy (electric violet + deep indigo with cool
 * cyan concentrations), information lines read in a crisp white-blue, and the ground stays a
 * near-black deep navy. Class accents (from the sigil) tint the class-specific bits on top.
 */
object StatusPalette {
    // Ground — near-black with a deep blue-violet tint.
    val groundDeep = Color(0xFF04050B)
    val groundNavy = Color(0xFF080A16)
    val groundTint = Color(0xFF0E0B1E)

    // Outer-frame energy.
    val violet = Color(0xFF7C5CFF)
    val violetBright = Color(0xFFA88BFF)
    val indigo = Color(0xFF3B2E8C)
    val indigoDeep = Color(0xFF1C1740)

    // Warm class energy (Berserker) — a controlled red-orange (handoff hue ~25), not neon.
    val ember = Color(0xFFF9744F)
    val emberSoft = Color(0xFFFF9E7E)

    // Highlight + information.
    val cyan = Color(0xFF4FE3FF)
    val cyanSoft = Color(0xFF7FE9FF)
    val infoLine = Color(0xFFD9E4FF)
    val textPrimary = Color(0xFFEAF0FF)
    val textMuted = Color(0xFF8A93B5)
    val label = Color(0xFF6E76A0)
}
