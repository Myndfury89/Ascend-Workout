package com.ascend.feature.ascended.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ascend.feature.ascended.prototype.model.EvolutionStage

/*
 * Effect layers drawn AROUND the imported figure art — the aura behind and the Skill manifestation
 * in front. These stay procedural (they are light, animatable energy effects), while the figure
 * itself is real imported artwork. Kept intentionally restrained so the art reads first.
 */

private val AURA = Color(0.42f, 0.45f, 0.55f, 0.20f)
private val SENSE = Color(0.10f, 0.68f, 0.85f, 0.85f)
private val SENSE_SOFT = Color(0.10f, 0.68f, 0.85f, 0.35f)

/** Class aura as concentric rings behind the figure, growing with the stage (Base = none). */
@Composable
fun AuraOverlay(
    stage: EvolutionStage,
    modifier: Modifier = Modifier,
) {
    val rings = stage.ordinal
    if (rings <= 0) return
    Canvas(modifier) {
        val center = Offset(size.width * 0.5f, size.height * 0.52f)
        val base = size.minDimension * 0.48f
        for (i in 0 until rings) {
            drawCircle(AURA, radius = base * (0.72f + 0.13f * i), center = center, style = Stroke(width = 2f))
        }
    }
}

/**
 * The Perception Skill manifestation at the head: L1 faint eyes; L5 brighter eyes + a sensing halo +
 * scan lines; L10 awakened eyes + a layered awareness field + energetic traces. Clearly stronger at
 * 10 than at 1. Head is assumed near top-centre of the figure frame.
 */
@Composable
fun PerceptionOverlay(
    level: Int,
    modifier: Modifier = Modifier,
) {
    if (level <= 0) return
    Canvas(modifier) {
        val hx = size.width * 0.5f
        val hy = size.height * 0.16f
        val w = size.width
        val eyeR =
            w * (
                if (level >= 10) {
                    0.014f
                } else if (level >= 5) {
                    0.011f
                } else {
                    0.009f
                }
            )
        drawCircle(SENSE, radius = eyeR, center = Offset(hx - w * 0.02f, hy))
        drawCircle(SENSE, radius = eyeR, center = Offset(hx + w * 0.02f, hy))
        if (level >= 5) {
            drawCircle(SENSE_SOFT, radius = w * 0.09f, center = Offset(hx, hy), style = Stroke(width = 2f))
            drawLine(SENSE_SOFT, Offset(hx - w * 0.16f, hy), Offset(hx - w * 0.10f, hy), strokeWidth = 2f)
            drawLine(SENSE_SOFT, Offset(hx + w * 0.10f, hy), Offset(hx + w * 0.16f, hy), strokeWidth = 2f)
        }
        if (level >= 10) {
            drawCircle(SENSE_SOFT, radius = w * 0.14f, center = Offset(hx, hy), style = Stroke(width = 1.5f))
            drawLine(SENSE_SOFT, Offset(hx, hy - w * 0.18f), Offset(hx, hy - w * 0.11f), strokeWidth = 2f)
            drawLine(SENSE_SOFT, Offset(hx - w * 0.13f, hy + w * 0.10f), Offset(hx - w * 0.07f, hy + w * 0.05f), strokeWidth = 2f)
            drawLine(SENSE_SOFT, Offset(hx + w * 0.07f, hy + w * 0.05f), Offset(hx + w * 0.13f, hy + w * 0.10f), strokeWidth = 2f)
        }
    }
}
