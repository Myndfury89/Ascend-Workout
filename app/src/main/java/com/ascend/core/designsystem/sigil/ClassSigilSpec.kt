package com.ascend.core.designsystem.sigil

import androidx.compose.ui.graphics.Color

/** Rotation that points a shape's first vertex straight up. */
const val POINT_UP_DEG = -90f

/** Stable class identity for sigil specs. Display names live elsewhere; "Mage" is final. */
enum class ClassKey {
    NEUTRAL,
    BERSERKER,
    MONK,
    MAGE,
}

/**
 * One primitive in a sigil, sized as a fraction of the sigil radius (so specs are resolution-free).
 * Rotation angles are degrees; -90 points the first vertex straight up.
 */
sealed interface SigilShape {
    /** Regular polygon. [sides] null = use the rank tier's base polygon side count. */
    data class Polygon(
        val sides: Int?,
        val radius: Float,
        val rotationDeg: Float = POINT_UP_DEG,
    ) : SigilShape

    /** Star with alternating [outer]/[inner] radii. [points] null = use the rank tier's side count. */
    data class Star(
        val points: Int?,
        val outer: Float,
        val inner: Float,
        val rotationDeg: Float = POINT_UP_DEG,
    ) : SigilShape

    /** Circle centred on the sigil centre. */
    data class Circle(
        val radius: Float,
    ) : SigilShape

    /** [petals] circles of radius [petal], centres [offset] out from the centre, first one straight up. */
    data class Rosette(
        val petals: Int,
        val offset: Float,
        val petal: Float,
    ) : SigilShape
}

/** The class-distinct geometry: the rotating central structure and the small static core anchor. */
data class SigilGeometrySpec(
    val central: List<SigilShape>,
    val coreAnchor: SigilShape,
)

/** Class colours. [warmAccent], when set, replaces [accent] under the warm-accent treatment. */
data class SigilPalette(
    val accent: Color,
    val warmAccent: Color? = null,
) {
    fun accentFor(warm: Boolean): Color = if (warm) warmAccent ?: accent else accent
}

/**
 * How a class moves and weighs. [rotationFactor] is signed: magnitude sets ambient drift speed,
 * sign sets direction (Mage reverses). [strokeScale] weights the class-distinct central strokes.
 */
data class ClassMotionProfile(
    val rotationFactor: Float,
    val strokeScale: Float,
)

/** Everything that makes one class's sigil distinct, as data. */
data class ClassSigilSpec(
    val key: ClassKey,
    val geometry: SigilGeometrySpec,
    val palette: SigilPalette,
    val motion: ClassMotionProfile,
)
