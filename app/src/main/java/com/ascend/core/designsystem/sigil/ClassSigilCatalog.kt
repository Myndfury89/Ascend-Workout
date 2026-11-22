package com.ascend.core.designsystem.sigil

import androidx.compose.ui.graphics.Color

/**
 * The per-class sigil specs. Values are copied 1:1 from the original inline renderer
 * (OrnateSigilGeometry `classCentralPaths`/`coreAnchorPath`, OrnateSigil `accentBlend`), so the
 * PR 1 screenshot goldens stay identical. Change a value here to change a class's sigil.
 */
object ClassSigilCatalog {
    // Colours mirror StatusPalette (a parity test guards this).
    private val EMBER = Color(color = 0xFFF9744F)
    private val VIOLET = Color(color = 0xFF7C5CFF)
    private val VIOLET_BRIGHT = Color(color = 0xFFA88BFF)
    private val CYAN_SOFT = Color(color = 0xFF7FE9FF)
    private val INFO_LINE = Color(color = 0xFFD9E4FF)

    // Accents approved by Matt 11/21 (steel / jade / gold).
    private val STEEL = Color(color = 0xFFB7C5D8)
    private val JADE = Color(color = 0xFF5ECF9A)
    private val GOLD = Color(color = 0xFFE0C36A)

    /** Neutral — rank polygon + star, understated until a class is chosen. */
    val Neutral =
        ClassSigilSpec(
            key = ClassKey.NEUTRAL,
            geometry =
                SigilGeometrySpec(
                    central =
                        listOf(
                            SigilShape.Polygon(sides = null, radius = 0.32f),
                            SigilShape.Star(points = null, outer = 0.28f, inner = 0.13f),
                        ),
                    coreAnchor = SigilShape.Circle(radius = 0.05f),
                ),
            palette = SigilPalette(accent = INFO_LINE),
            motion = ClassMotionProfile(rotationFactor = 1.0f, strokeScale = 1.0f),
        )

    /** Berserker — bold five-point star over a small inner pentagon; 4-point spike-star core. */
    val Berserker =
        ClassSigilSpec(
            key = ClassKey.BERSERKER,
            geometry =
                SigilGeometrySpec(
                    central =
                        listOf(
                            SigilShape.Star(points = 5, outer = 0.38f, inner = 0.10f),
                            SigilShape.Polygon(sides = 5, radius = 0.14f, rotationDeg = -90f + 36f),
                        ),
                    coreAnchor = SigilShape.Star(points = 4, outer = 0.11f, inner = 0.028f),
                ),
            palette = SigilPalette(accent = VIOLET_BRIGHT, warmAccent = EMBER),
            motion = ClassMotionProfile(rotationFactor = 0.77f, strokeScale = 1.2f),
        )

    /** Monk — hexagon enclosing an inscribed hexagram; hexagon core. */
    val Monk =
        ClassSigilSpec(
            key = ClassKey.MONK,
            geometry =
                SigilGeometrySpec(
                    central =
                        listOf(
                            SigilShape.Polygon(sides = 6, radius = 0.30f),
                            SigilShape.Polygon(sides = 3, radius = 0.28f),
                            SigilShape.Polygon(sides = 3, radius = 0.28f, rotationDeg = 90f),
                        ),
                    coreAnchor = SigilShape.Polygon(sides = 6, radius = 0.085f),
                ),
            palette = SigilPalette(accent = CYAN_SOFT),
            motion = ClassMotionProfile(rotationFactor = 0.95f, strokeScale = 1.0f),
        )

    /** Mage — six-petal rosette inside a bounding circle (all curves); circle core. */
    val Mage =
        ClassSigilSpec(
            key = ClassKey.MAGE,
            geometry =
                SigilGeometrySpec(
                    central =
                        listOf(
                            SigilShape.Rosette(petals = 6, offset = 0.17f, petal = 0.17f),
                            SigilShape.Circle(radius = 0.30f),
                        ),
                    coreAnchor = SigilShape.Circle(radius = 0.075f),
                ),
            palette = SigilPalette(accent = VIOLET),
            motion = ClassMotionProfile(rotationFactor = -1.18f, strokeScale = 0.85f),
        )

    // Fighter / Ranger / Guardian: approved "A" geometry (the 10/29 draft table). rotationFactor is the
    // 10/29 draft value; strokeScale is 1.0 as on the approved icons. Both are tuning values.

    /** Fighter — octagon with a slightly larger eight-point star (octagram); octagon core. */
    val Fighter =
        ClassSigilSpec(
            key = ClassKey.FIGHTER,
            geometry =
                SigilGeometrySpec(
                    central =
                        listOf(
                            SigilShape.Polygon(sides = 8, radius = 0.30f),
                            SigilShape.Star(points = 8, outer = 0.34f, inner = 0.24f),
                        ),
                    coreAnchor = SigilShape.Polygon(sides = 8, radius = 0.08f),
                ),
            palette = SigilPalette(accent = STEEL),
            motion = ClassMotionProfile(rotationFactor = 0.85f, strokeScale = 1.0f),
        )

    /** Ranger — three open chevrons at 120° around an upward triangle; small upward chevron core. */
    val Ranger =
        ClassSigilSpec(
            key = ClassKey.RANGER,
            geometry =
                SigilGeometrySpec(
                    central =
                        listOf(
                            SigilShape.Chevron(width = 0.30f, depth = 0.18f, tipRadius = 0.34f, rotationDeg = -90f),
                            SigilShape.Chevron(width = 0.30f, depth = 0.18f, tipRadius = 0.34f, rotationDeg = 30f),
                            SigilShape.Chevron(width = 0.30f, depth = 0.18f, tipRadius = 0.34f, rotationDeg = 150f),
                            SigilShape.Polygon(sides = 3, radius = 0.12f),
                        ),
                    coreAnchor = SigilShape.Chevron(width = 0.08f, depth = 0.05f, tipRadius = 0.055f),
                ),
            palette = SigilPalette(accent = JADE),
            motion = ClassMotionProfile(rotationFactor = 1.1f, strokeScale = 1.0f),
        )

    /** Guardian — heater shield enclosing a hexagon; circle core. */
    val Guardian =
        ClassSigilSpec(
            key = ClassKey.GUARDIAN,
            geometry =
                SigilGeometrySpec(
                    central =
                        listOf(
                            SigilShape.Shield(width = 0.42f, height = 0.50f),
                            SigilShape.Polygon(sides = 6, radius = 0.20f),
                        ),
                    coreAnchor = SigilShape.Circle(radius = 0.07f),
                ),
            palette = SigilPalette(accent = GOLD),
            motion = ClassMotionProfile(rotationFactor = 0.4f, strokeScale = 1.0f),
        )

    val all: List<ClassSigilSpec> = listOf(Neutral, Berserker, Monk, Mage, Fighter, Ranger, Guardian)

    fun of(key: ClassKey): ClassSigilSpec = all.first { it.key == key }
}
