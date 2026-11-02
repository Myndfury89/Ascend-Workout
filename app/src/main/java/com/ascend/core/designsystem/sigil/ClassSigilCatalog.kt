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
        )

    val all: List<ClassSigilSpec> = listOf(Neutral, Berserker, Monk, Mage)

    fun of(key: ClassKey): ClassSigilSpec = all.first { it.key == key }
}
