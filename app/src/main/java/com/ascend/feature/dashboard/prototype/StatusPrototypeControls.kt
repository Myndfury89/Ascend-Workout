package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The transient event overlay pill raised over the panel; animated by [reveal] and [flash]. */
@Composable
internal fun EventOverlayChip(
    overlay: StatusEventOverlay,
    accent: Color,
    reveal: Float,
    flash: Float,
) {
    if (overlay.kind == StatusOverlayKind.NONE || reveal <= 0.01f) return
    Surface(
        color = Color(0xFF10151F).copy(alpha = 0.92f),
        shape = RoundedCornerShape(10.dp),
        modifier =
            Modifier
                .graphicsLayer {
                    alpha = reveal
                    translationY = (1f - reveal) * -16.dp.toPx()
                    val s = 0.96f + 0.04f * reveal + 0.04f * flash
                    scaleX = s
                    scaleY = s
                },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(overlay.title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (overlay.detail.isNotEmpty()) {
                Text(overlay.detail, color = Color(0xFF9AA6B8), fontSize = 12.sp)
            }
        }
    }
}

/**
 * Debug-only controls: a state selector (each fake event), a class selector, the
 * reduced-motion and simplified-effects toggles, and replay. Selecting a state re-triggers its
 * animation, so these double as the fake-event trigger buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatusPrototypeControls(
    stateId: StatusPrototypeStateId,
    variant: StatusClassVariant,
    reducedMotion: Boolean,
    simplifiedEffects: Boolean,
    onState: (StatusPrototypeStateId) -> Unit,
    onVariant: (StatusClassVariant) -> Unit,
    onReplay: () -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onSimplifiedEffects: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xFF10141C),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Label("Class")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusClassVariant.entries.forEach { v ->
                    FilterChip(
                        selected = v == variant,
                        onClick = { onVariant(v) },
                        label = { Text(v.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Label("State / fake event")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPrototypeStateId.entries.forEach { s ->
                    FilterChip(
                        selected = s == stateId,
                        onClick = { onState(s) },
                        label = { Text(s.label, fontSize = 11.sp) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = reducedMotion, onCheckedChange = onReducedMotion)
                Text("  Reduced motion", color = Color(0xFFCAD4E2), fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = simplifiedEffects, onCheckedChange = onSimplifiedEffects)
                Text("  Simplified effects", color = Color(0xFFCAD4E2), fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color(0xFF1B2130),
                shape = RoundedCornerShape(10.dp),
                onClick = onReplay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Replay animation",
                    color = Color(0xFF3FD9C7),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        color = Color(0xFF6E7C90),
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}
