package com.ascend.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.feature.dashboard.prototype.HudPanel
import com.ascend.feature.dashboard.prototype.QuestCompletionSummary
import com.ascend.feature.dashboard.prototype.StatusPalette
import kotlin.math.cos
import kotlin.math.sin

/*
 * The PRODUCTION Quest Complete HUD window. It is fed REAL data — [QuestCompletionSummary] built by
 * StatusComposition from the drained ProgressionEventQueue batch (title from the event label,
 * identity from sourceId, rewards from the batch deltas) — and reuses the shared [HudPanel] so it
 * speaks the same semi-transparent, edge-lit protocol language as the main Status panel rather than
 * a flat Material card or a toast. It is NON-MODAL: it renders as a floating overlay that neither
 * blocks input nor the queue, and its only interactive element is an optional dismiss affordance
 * that changes no rewards and no queue state. [reveal] drives the assemble (1 = settled, used as-is
 * under reduced motion); [pulse] is the living energy beat (constant under reduced motion).
 */
@Composable
fun QuestCompleteHud(
    summary: QuestCompletionSummary,
    accent: Color,
    reveal: Float,
    pulse: Float,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    HudPanel(
        accent = accent,
        reveal = reveal,
        pulse = pulse,
        modifier = modifier,
        title = "DAILY QUEST COMPLETE",
        subtitle = summary.title,
        crest = { QuestCompleteCrest(accent, pulse) },
        footer = "SYSTEM · recorded to your ascension log",
    ) {
        val hasRewards = summary.xpGained > 0 || summary.attributeGains.isNotEmpty()
        if (hasRewards) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (summary.xpGained > 0) RewardStat(accent, "PLAYER XP", "+${summary.xpGained}")
                // A handful of attribute gains — the window summarises them so no separate beat is needed.
                summary.attributeGains.take(3).forEach { RewardStat(accent, it.name.uppercase(), "+${it.amount}") }
            }
        } else {
            Text("Objectives complete.", color = StatusPalette.textMuted, fontSize = 13.sp)
        }
        if (onDismiss != null) {
            Spacer(Modifier.height(14.dp))
            ContinueChip(accent, pulse, onDismiss)
        }
    }
}

@Composable
private fun RewardStat(
    accent: Color,
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = StatusPalette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = accent.copy(alpha = 0.85f), fontSize = 9.sp, letterSpacing = 1.5.sp)
    }
}

/** The optional non-modal dismiss: a semi-transparent luminous chip — never a solid Material button. */
@Composable
private fun ContinueChip(
    accent: Color,
    pulse: Float,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .height(34.dp)
            .clickable(onClick = onDismiss)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(accent.copy(alpha = 0.10f + 0.06f * pulse), cornerRadius = radius)
                drawRoundRect(accent.copy(alpha = 0.6f + 0.3f * pulse), cornerRadius = radius, style = Stroke(width = 1.5f))
            }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("CONTINUE ▸", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A small original geometric crest — a ring + inscribed star. Decorative: excluded from a11y. */
@Composable
private fun QuestCompleteCrest(
    accent: Color,
    pulse: Float,
) {
    Canvas(
        Modifier
            .size(30.dp)
            .clearAndSetSemantics { },
    ) {
        val r = size.minDimension / 2f
        drawCircle(accent.copy(alpha = 0.35f + 0.25f * pulse), radius = r, style = Stroke(width = 1.5f))
        val path = Path()
        val points = 5
        for (i in 0 until points * 2) {
            val rr = if (i % 2 == 0) r * 0.7f else r * 0.32f
            val a = -Math.PI / 2 + Math.PI * i / points
            val p = Offset((cos(a) * rr).toFloat() + size.width / 2, (sin(a) * rr).toFloat() + size.height / 2)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, StatusPalette.cyanSoft.copy(alpha = 0.7f + 0.3f * pulse), style = Stroke(width = 1.2f))
    }
}
