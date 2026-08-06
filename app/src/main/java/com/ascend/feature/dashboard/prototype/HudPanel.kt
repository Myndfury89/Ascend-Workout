package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The HUD window language: a semi-transparent, textured, edge-lit protocol panel used by the
 * Quest / Achievement / Level-up / Skill-unlock windows. It REUSES the existing panel treatment —
 * drawStatusPanelSurface (dark radial fill + faint grid/diagonal glass texture + accent top-haze)
 * and drawStatusPanelEdge (thin luminous cyan/violet outline + clipped-corner cross-marks) — so the
 * windows speak the same visual system as the main Status panel. It is deliberately NOT a Material
 * card: semi-transparent so the charged field shows through, angular (clipped-corner) framing, a
 * restrained inner glow, and an optional living breathing pulse. Brightness is capped so whites
 * never bloom and text stays first in the hierarchy.
 */

@Composable
fun HudPanel(
    accent: Color,
    reveal: Float,
    pulse: Float,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    crest: (@Composable () -> Unit)? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val r = reveal.coerceIn(0f, 1f)
    val breath = 0.9f + 0.1f * pulse.coerceIn(0f, 1f)
    Column(
        modifier
            .graphicsLayer {
                alpha = r
                // A restrained assemble: the window resolves in from 96% — never a boxy pop.
                val s = 0.96f + 0.04f * r
                scaleX = s
                scaleY = s
            }
            // Semi-transparent textured surface (reused) — 0.9 alpha so the field shows through.
            .drawBehind { drawStatusPanelSurface(accent, materialize = 0.9f * r) }
            .drawWithContent {
                drawContent()
                // Soft inner glow gathered at the top edge — energy pooling inside the window.
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.10f * r * breath), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.4f,
                        ),
                )
                // Reused luminous edge; the breathing pulse only nudges its brightness.
                drawStatusPanelEdge(accent, materialize = r * breath)
            }
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        if (title != null || crest != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (crest != null) {
                    crest()
                    Spacer(Modifier.size(12.dp))
                }
                Column {
                    if (title != null) {
                        Text(title, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    if (subtitle != null) {
                        Text(subtitle, color = StatusPalette.textMuted, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            HudDivider(accent, r)
            Spacer(Modifier.height(12.dp))
        }
        content()
        if (footer != null) {
            Spacer(Modifier.height(12.dp))
            HudDivider(accent, r)
            Spacer(Modifier.height(8.dp))
            Text(footer, color = StatusPalette.label, fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}

/** A thin luminous separator: a faint full-width rail with a brighter cyan core segment. */
@Composable
fun HudDivider(
    accent: Color,
    reveal: Float,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawLine(
                    brush =
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accent.copy(alpha = 0.5f * reveal),
                                StatusPalette.cyan.copy(alpha = 0.7f * reveal),
                                Color.Transparent,
                            ),
                        ),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = size.height,
                )
            },
    )
}
