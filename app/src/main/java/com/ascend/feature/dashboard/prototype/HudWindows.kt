package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/*
 * The four HUD windows — Daily Quest, Achievement, Level-up, and Skill unlock — all built on the
 * shared [HudPanel] so they read as one system: semi-transparent, textured, edge-lit protocol
 * panels rather than flat cards, banners, or toasts. [reveal] (0..1) is the assemble; [pulse]
 * (0..1) is the living breathing/energy beat. Under reduced motion the caller passes reveal=1 and
 * a constant pulse so the same window resolves with fades only. Fake copy — no production data.
 */

@Composable
fun HudReviewWindow(
    kind: HudWindowKind,
    accent: Color,
    reveal: Float,
    pulse: Float,
    modifier: Modifier = Modifier,
    burstMode: BurstMode = BurstMode.PROCEDURAL,
) {
    when (kind) {
        HudWindowKind.NONE -> Unit
        HudWindowKind.QUEST -> QuestWindow(accent, reveal, pulse, modifier)
        HudWindowKind.ACHIEVEMENT -> AchievementWindow(accent, reveal, pulse, modifier)
        HudWindowKind.LEVEL_UP -> LevelUpWindow(accent, reveal, pulse, burstMode, modifier)
        HudWindowKind.SKILL_UNLOCK -> SkillUnlockWindow(accent, reveal, pulse, modifier)
    }
}

@Composable
private fun QuestWindow(
    accent: Color,
    reveal: Float,
    pulse: Float,
    modifier: Modifier,
) {
    HudPanel(
        accent = accent,
        reveal = reveal,
        pulse = pulse,
        modifier = modifier,
        title = "DAILY QUEST",
        subtitle = "System protocol · 3 objectives",
        crest = { HudCrest(accent, pulse) },
        footer = "SYSTEM · protocol resets at 00:00",
    ) {
        ObjectiveRow(accent, "Push-ups", 120, 200, pulse)
        Spacer(Modifier.height(10.dp))
        ObjectiveRow(accent, "Bodyweight squats", 30, 30, pulse)
        Spacer(Modifier.height(10.dp))
        ObjectiveRow(accent, "Walking steps", 6400, 8000, pulse)
    }
}

@Composable
private fun AchievementWindow(
    accent: Color,
    reveal: Float,
    pulse: Float,
    modifier: Modifier,
) {
    HudPanel(
        accent = accent,
        reveal = reveal,
        pulse = pulse,
        modifier = modifier,
        title = "ACHIEVEMENT UNLOCKED",
        subtitle = "Milestone recognised",
        crest = { HudCrest(accent, pulse) },
        footer = "SYSTEM · recorded to your ascension log",
    ) {
        Text("Unbroken", color = StatusPalette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Complete seven consecutive days of Daily Quests without a miss.", color = StatusPalette.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        RewardChip(accent, "+250 Player XP", pulse)
    }
}

@Composable
private fun LevelUpWindow(
    accent: Color,
    reveal: Float,
    pulse: Float,
    burstMode: BurstMode,
    modifier: Modifier,
) {
    HudPanel(
        accent = accent,
        reveal = reveal,
        pulse = pulse,
        modifier = modifier,
        title = "THRESHOLD SURPASSED",
        subtitle = "Charge → Breakthrough → Lock → Settle",
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // The ceremonial burst is radial line geometry, never a square box.
                Box(contentAlignment = Alignment.Center) {
                    BreakthroughBurst(accent, pulse, burstMode, Modifier.size(120.dp))
                    Text("6", color = StatusPalette.textPrimary, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("PLAYER LEVEL", color = accent, fontSize = 12.sp, letterSpacing = 3.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        HudDivider(accent, reveal)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatColumn(accent, "STRENGTH", "+2")
            StatColumn(accent, "DISCIPLINE", "+1")
            StatColumn(accent, "NEXT", "Lv 7")
        }
    }
}

@Composable
private fun SkillUnlockWindow(
    accent: Color,
    reveal: Float,
    pulse: Float,
    modifier: Modifier,
) {
    HudPanel(
        accent = accent,
        reveal = reveal,
        pulse = pulse,
        modifier = modifier,
        title = "SKILL UNLOCKED",
        subtitle = "New system function online",
        crest = { HudCrest(accent, pulse) },
        footer = "SYSTEM · transferable across every path",
    ) {
        Text("Perception", color = StatusPalette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("Awareness · Skill Lv 1", color = accent, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "Surfaces hidden training insight — reveals readiness and progression cues earlier.",
            color = StatusPalette.textMuted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatColumn(accent, "UNLOCKED BY", "15 min cardio")
            StatColumn(accent, "AFFINITY", "Magician")
        }
        Spacer(Modifier.height(14.dp))
        RewardChip(accent, "CONTINUE ▸", pulse)
    }
}

// ---- shared window parts ----

@Composable
private fun ObjectiveRow(
    accent: Color,
    name: String,
    current: Int,
    target: Int,
    pulse: Float,
) {
    val done = current >= target
    val fraction = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Completion marker — a ring that fills to a bright dot when the objective is met.
        Canvas(Modifier.size(14.dp)) {
            drawCircle(StatusPalette.label.copy(alpha = 0.7f), radius = size.minDimension / 2f, style = Stroke(width = 1.5f))
            if (done) drawCircle(StatusPalette.cyan.copy(alpha = 0.6f + 0.4f * pulse), radius = size.minDimension / 3f)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = StatusPalette.textPrimary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                drawLine(
                    StatusPalette.label.copy(alpha = 0.35f),
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    (if (done) StatusPalette.cyan else accent).copy(alpha = 0.9f),
                    Offset(0f, size.height / 2),
                    Offset(size.width * fraction, size.height / 2),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "$current/$target",
            color = if (done) StatusPalette.cyan else StatusPalette.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RewardChip(
    accent: Color,
    label: String,
    pulse: Float,
) {
    Box(
        Modifier
            .height(34.dp)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                // Semi-transparent fill + a thin luminous border + a faint outer glow — never a solid button.
                drawRoundRect(accent.copy(alpha = 0.28f), cornerRadius = radius, style = Stroke(width = 4f))
                drawRoundRect(accent.copy(alpha = 0.10f + 0.06f * pulse), cornerRadius = radius)
                drawRoundRect(accent.copy(alpha = 0.6f + 0.3f * pulse), cornerRadius = radius, style = Stroke(width = 1.5f))
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatColumn(
    accent: Color,
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = StatusPalette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = accent.copy(alpha = 0.85f), fontSize = 9.sp, letterSpacing = 1.5.sp)
    }
}

/** A small original geometric crest — a ring + inscribed star — for the window headers. */
@Composable
private fun HudCrest(
    accent: Color,
    pulse: Float,
) {
    Canvas(Modifier.size(30.dp)) {
        val r = size.minDimension / 2f
        drawCircle(accent.copy(alpha = 0.35f + 0.25f * pulse), radius = r, style = Stroke(width = 1.5f))
        val path = androidx.compose.ui.graphics.Path()
        val points = 5
        for (i in 0 until points * 2) {
            val rr = if (i % 2 == 0) r * 0.7f else r * 0.3f
            val a = -Math.PI / 2 + Math.PI * i / points
            val p = Offset((cos(a) * rr).toFloat() + size.width / 2, (sin(a) * rr).toFloat() + size.height / 2)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, StatusPalette.cyanSoft.copy(alpha = 0.7f + 0.3f * pulse), style = Stroke(width = 1.2f))
    }
}
