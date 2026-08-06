package com.ascend.feature.ascended.prototype.body

import androidx.compose.ui.geometry.Offset

/*
 * The flat, cut-paper class silhouette model. Each class figure is a small set of large grayscale
 * shapes — a shape-language system where the class reads at a glance from proportion, posture, and
 * iconic props, NOT from faces, texture, armour engraving, or fine linework. Pure data (normalized
 * polygons + a value layer); Compose Path construction lives in the renderer.
 */

/** The limited grayscale value system: near-black silhouette, charcoal, medium, and one light accent. */
enum class SilhouetteTone(val value: Float) {
    SILHOUETTE(0.10f),
    DARK(0.24f),
    MEDIUM(0.42f),
    LIGHT(0.62f),
}

/** One large flat shape in a class figure — a named slot, its value layer, and its polygon. */
data class SilhouetteShape(
    val name: String,
    val tone: SilhouetteTone,
    val polygon: List<Offset>,
)

/** A whole class figure: the class it represents and its ordered (back-to-front) shapes. */
data class ClassSilhouette(
    val shapes: List<SilhouetteShape>,
)

/** The figure's normalized bounding box — used to compare class proportions (aspect ratio, extent). */
data class SilhouetteBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY

    /** Width / height — low is tall-and-narrow (Mage / Assassin), high is broad (Berserker / Guardian). */
    val aspect: Float get() = if (height <= 0f) 0f else width / height
}

/** The tight bounding box over every shape's points. */
fun boundsOf(shapes: List<SilhouetteShape>): SilhouetteBounds {
    val pts = shapes.flatMap { it.polygon }
    require(pts.isNotEmpty()) { "a class silhouette must have at least one shape" }
    return SilhouetteBounds(
        minX = pts.minOf { it.x },
        maxX = pts.maxOf { it.x },
        minY = pts.minOf { it.y },
        maxY = pts.maxOf { it.y },
    )
}
