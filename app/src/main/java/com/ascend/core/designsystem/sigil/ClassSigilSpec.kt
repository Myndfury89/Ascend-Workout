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
    ASSASSIN,
    FIGHTER,
    RANGER,
    GUARDIAN,
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

    /**
     * Open arrowhead (two strokes, never closed). The tip sits [tipRadius] out from the centre along
     * [rotationDeg]; the two arms run back toward the centre by [depth] and spread to a total [width].
     */
    data class Chevron(
        val width: Float,
        val depth: Float,
        val tipRadius: Float,
        val rotationDeg: Float = POINT_UP_DEG,
    ) : SigilShape

    /**
     * Closed heater shield centred on the sigil centre: flat top (corners inset to 92% of the half
     * width), sides widest at 55% of the height, point at the bottom. [width] and [height] are full
     * extents.
     */
    data class Shield(
        val width: Float,
        val height: Float,
    ) : SigilShape

    /**
     * A closed freeform outline: starts at [start] and follows [segments], then closes. Points and
     * radii are radius-relative with the sigil centre at (0, 0) and +y down, like the other shapes.
     * Used where the regular primitives cannot express the mark (Assassin crescent and dagger).
     */
    data class Contour(
        val start: ContourPoint,
        val segments: List<ContourSegment>,
    ) : SigilShape
}

/** A radius-relative point; (0, 0) is the sigil centre, +y is down. */
data class ContourPoint(
    val x: Float,
    val y: Float,
)

/** One step of a [SigilShape.Contour]. */
sealed interface ContourSegment {
    /** Straight line to [to]. */
    data class Line(
        val to: ContourPoint,
    ) : ContourSegment

    /**
     * Circular arc on the circle at [center] with [radius], from [startDeg] sweeping [sweepDeg]
     * (0° = +x, positive = clockwise on screen, negative = counter-clockwise), as `Path.arcTo`.
     */
    data class Arc(
        val center: ContourPoint,
        val radius: Float,
        val startDeg: Float,
        val sweepDeg: Float,
    ) : ContourSegment
}

/**
 * The class-distinct geometry: the rotating central structure and the small static core anchor.
 * [coreAnchor] is null for a class whose approved mark has no separate core (Assassin).
 */
data class SigilGeometrySpec(
    val central: List<SigilShape>,
    val coreAnchor: SigilShape?,
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
