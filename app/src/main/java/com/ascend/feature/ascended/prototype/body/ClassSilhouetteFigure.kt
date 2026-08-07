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
import com.ascend.feature.ascended.prototype.model.EvolutionStage

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
    fidelity: SilhouetteFidelity = SilhouetteFidelity.BLOCKOUT,
    silhouetteOnly: Boolean = false,
    showLayers: Boolean = false,
    outlineOnly: Boolean = false,
    stage: EvolutionStage = EvolutionStage.MASTERED,
    perceptionLevel: Int = 0,
) {
    val body =
        remember(ascendedClass, bodyBase, fidelity, stage) {
            ClassSilhouetteGeometry.build(ascendedClass, bodyBase, fidelity, stage).shapes
        }
    // Aura + Skill glows are not part of the silhouette itself, so they are dropped in the
    // silhouette-only / outline review modes (recognition + contour are judged on the body alone).
    val glowsVisible = !silhouetteOnly && !outlineOnly
    val aura = remember(stage, glowsVisible) { if (glowsVisible) ClassSilhouetteGeometry.auraShapes(stage) else emptyList() }
    val perception =
        remember(ascendedClass, perceptionLevel, glowsVisible) {
            if (glowsVisible) ClassSilhouetteGeometry.perceptionShapes(perceptionLevel, ascendedClass) else emptyList()
        }
    val description =
        "${ascendedClass.displayName} class silhouette, ${bodyBase.name.lowercase()} base, front-facing."
    Canvas(modifier.semantics { contentDescription = description }) {
        fun pathOf(shape: SilhouetteShape): Path =
            Path().apply {
                shape.polygon.forEachIndexed { i, pt ->
                    val x = pt.x * size.width
                    val y = pt.y * size.height
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        // Aura behind.
        aura.forEach {
            val t = it.tone.value
            drawPath(pathOf(it), Color(t, t, t))
        }
        // Body (stage-filtered), honouring the review modes.
        body.forEach { shape ->
            val path = pathOf(shape)
            if (outlineOnly) {
                drawPath(path, color = Color(0.08f, 0.08f, 0.10f, 0.9f), style = Stroke(width = 1.4f))
            } else {
                val tone = if (silhouetteOnly) SilhouetteTone.SILHOUETTE.value else shape.tone.value
                drawPath(path, Color(tone, tone, tone))
                if (showLayers && !silhouetteOnly) {
                    drawPath(path, color = Color(0.0f, 0.75f, 0.85f, 0.55f), style = Stroke(width = 1.5f))
                }
            }
        }
        // Skill manifestation in front.
        perception.forEach {
            val t = it.tone.value
            drawPath(pathOf(it), Color(t, t, t))
        }
    }
}

/** The shape count of a class figure at a given fidelity + stage — surfaced in the review controls. */
fun classShapeCount(
    ascendedClass: AscendedClass,
    bodyBase: BodyBase,
    fidelity: SilhouetteFidelity,
    stage: EvolutionStage = EvolutionStage.MASTERED,
): Int = ClassSilhouetteGeometry.build(ascendedClass, bodyBase, fidelity, stage).shapes.size
