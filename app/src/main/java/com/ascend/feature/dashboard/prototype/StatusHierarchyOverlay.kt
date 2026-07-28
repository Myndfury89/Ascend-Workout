package com.ascend.feature.dashboard.prototype

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

/**
 * A debug-only overlay that draws a labelled, dashed-free outline around a layout zone. It is
 * layout-neutral — it only paints on top via [Modifier.drawWithContent], so toggling it never
 * changes the measured layout of any component. Restrained styling (thin line + small label chip)
 * to reveal overlap, clipping, and hierarchy problems without obscuring content.
 */
@Composable
fun rememberHierarchyLabeler(): TextMeasurer = rememberTextMeasurer()

fun Modifier.hierarchyZone(
    label: String,
    enabled: Boolean,
    measurer: TextMeasurer,
    color: Color = Color(0xFF3FD9C7),
): Modifier =
    if (!enabled) {
        this
    } else {
        this.drawWithContent {
            drawContent()
            drawRect(color = color.copy(alpha = 0.55f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
            val layout =
                measurer.measure(
                    text = label,
                    style = TextStyle(color = Color(0xFF04070C), fontSize = 8.sp),
                )
            val pad = 3f
            drawRect(
                color = color.copy(alpha = 0.8f),
                topLeft = Offset(0f, 0f),
                size = Size(layout.size.width + pad * 2, layout.size.height + pad),
            )
            drawText(layout, topLeft = Offset(pad, 0f))
        }
    }
