package com.ascend.feature.ascended.prototype.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.dashboard.prototype.StatusPalette

/**
 * CP1 review controls for the "Your Ascended" prototype. Deliberately minimal: the male/female base
 * toggle (the CP1 acceptance control), reduced motion, and a seam overlay to make the individually
 * addressable body regions visible during review. Class / stage / Skill / attribute / aura /
 * equipment controls arrive in later checkpoints. Prototype-only — never wired to production.
 */
@Composable
fun AscendedReviewControls(
    bodyBase: BodyBase,
    reducedMotion: Boolean,
    seams: Boolean,
    onBodyBase: (BodyBase) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onSeams: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xFF10151F).copy(alpha = 0.85f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("BODY BASE", color = StatusPalette.label, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BodyBase.entries.forEach { base ->
                    BaseToggle(
                        label = base.name,
                        selected = base == bodyBase,
                        onClick = { onBodyBase(base) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = reducedMotion, onCheckedChange = onReducedMotion)
                Text("  Reduced motion", color = StatusPalette.textMuted)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = seams, onCheckedChange = onSeams)
                Text("  Show region seams", color = StatusPalette.textMuted)
            }
        }
    }
}

@Composable
private fun BaseToggle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusPalette.violet),
        ) {
            Text(label, color = StatusPalette.textPrimary)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.height(44.dp)) {
            Text(label, color = StatusPalette.textMuted)
        }
    }
}
