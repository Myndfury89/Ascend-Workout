package com.ascend.feature.ascended.prototype.body

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase

/*
 * Renders a class silhouette from its flat cut-paper shapes. Three review modes:
 *  - normal: the few grayscale value layers (near-black / charcoal / medium / one light accent);
 *  - [showLayers]: adds crisp seams so the construction pieces are visible;
 *  - [silhouetteOnly]: flattens every shape to solid near-black to judge recognition by outline alone.
 * One a11y description per figure; no text, no faces — silhouette first.
 */
@Composable
fun ClassSilhouetteFigure(
    ascendedClass: AscendedClass,
    bodyBase: BodyBase,
    modifier: Modifier = Modifier,
    silhouetteOnly: Boolean = false,
    showLayers: Boolean = false,
) {
    val figure = remember(ascendedClass, bodyBase) { ClassSilhouetteGeometry.build(ascendedClass, bodyBase) }
    val description =
        "${ascendedClass.displayName} class silhouette, ${bodyBase.name.lowercase()} base, front-facing."
    Canvas(modifier.semantics { contentDescription = description }) {
        figure.shapes.forEach { shape ->
            val path =
                Path().apply {
                    shape.polygon.forEachIndexed { i, pt ->
                        val x = pt.x * size.width
                        val y = pt.y * size.height
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
            val tone = if (silhouetteOnly) SilhouetteTone.SILHOUETTE.value else shape.tone.value
            drawPath(path, Color(tone, tone, tone))
            if (showLayers && !silhouetteOnly) {
                drawPath(path, color = Color(0.0f, 0.75f, 0.85f, 0.55f), style = Stroke(width = 1.5f))
            }
        }
    }
}
