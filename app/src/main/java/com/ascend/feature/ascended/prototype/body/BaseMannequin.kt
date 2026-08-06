package com.ascend.feature.ascended.prototype.body

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.dashboard.prototype.StatusPalette

/*
 * The base mannequin body layer: draws the addressable [MannequinGeometry] regions as flat
 * grayscale polygons with subtle tonal value-separation and crisp seams — a structured, front-
 * facing geometric base, not a 3D render, toy, or rounded blob. [seams] brightens the outlines
 * (a review aid) to make the individually addressable regions obvious. Purely presentational.
 */
@Composable
fun MaleBaseMannequin(
    modifier: Modifier = Modifier,
    seams: Boolean = false,
) = BaseMannequin(BodyBase.MALE, modifier, seams)

@Composable
fun FemaleBaseMannequin(
    modifier: Modifier = Modifier,
    seams: Boolean = false,
) = BaseMannequin(BodyBase.FEMALE, modifier, seams)

@Composable
fun BaseMannequin(
    bodyBase: BodyBase,
    modifier: Modifier = Modifier,
    seams: Boolean = false,
) {
    val regions = remember(bodyBase) { MannequinGeometry.build(bodyBase) }
    val description =
        when (bodyBase) {
            BodyBase.MALE -> "Male base mannequin, front-facing."
            BodyBase.FEMALE -> "Female base mannequin, front-facing."
        }
    Canvas(
        modifier.semantics { contentDescription = description },
    ) {
        val outline =
            if (seams) StatusPalette.cyanSoft.copy(alpha = 0.55f) else Color(0xFF0A0D16).copy(alpha = 0.55f)
        val outlineWidth = if (seams) 1.6f else 1.1f
        regions.forEach { region ->
            val path =
                Path().apply {
                    region.polygon.forEachIndexed { i, pt ->
                        val x = pt.x * size.width
                        val y = pt.y * size.height
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
            drawPath(path, color = grayFor(region.tone))
            drawPath(path, color = outline, style = Stroke(width = outlineWidth))
        }
    }
}

/** Map a 0..1 region tone to a slightly cool grayscale fill — light greys on the dark field. */
private fun grayFor(tone: Float): Color {
    val l = LUM_DARK + (LUM_LIGHT - LUM_DARK) * tone.coerceIn(0f, 1f)
    return Color(red = l * 0.93f, green = l * 0.97f, blue = l, alpha = 0.93f)
}

private const val LUM_DARK = 0.30f
private const val LUM_LIGHT = 0.85f

/** Scale a normalized point into a drawing rect (exposed for overlay layers that attach to the body). */
fun scaledPoint(
    normalized: Offset,
    width: Float,
    height: Float,
): Offset = Offset(normalized.x * width, normalized.y * height)
