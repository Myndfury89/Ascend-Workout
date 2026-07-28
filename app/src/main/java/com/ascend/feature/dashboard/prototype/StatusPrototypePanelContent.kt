package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

internal val AttributeAccent: Map<String, Color> =
    mapOf(
        "Strength" to Color(0xFFE8735A),
        "Endurance" to Color(0xFF3FD9C7),
        "Agility" to Color(0xFF7EC46B),
        "Discipline" to Color(0xFF9B8CFF),
        "Recovery" to Color(0xFF62B6E8),
    )

private const val ATTRIBUTE_CEILING = 120f

/** A thin luminous bar: dark track, accent-gradient fill, and a bright leading edge. */
@Composable
internal fun LuminBar(
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Int = 8,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height.dp)) {
        val h = size.height
        val r = h / 2f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.05f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        )
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.5f), accent)),
                size = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            )
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = androidx.compose.ui.geometry.Offset(w, 0f),
                end = androidx.compose.ui.geometry.Offset(w, h),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Level + class identity — the top of the visual hierarchy. */
@Composable
internal fun IdentityBlock(
    data: StatusPrototypeData,
    accent: Color,
    reveal: Float,
) {
    Column(Modifier.graphicsLayer { alpha = reveal }) {
        Text(
            text = data.rankLabel.uppercase(),
            color = accent,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(data.hunterName, color = Color(0xFFEAEEF6), fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LEVEL ${data.level}", color = Color(0xFFB6C2D4), fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
            Text("   ·   ${data.variant.displayName}", color = accent.copy(alpha = 0.9f), fontSize = 14.sp)
            data.secondaryVariant?.let {
                Text("  /  ${it.displayName}", color = Color(0xFF8A97AC), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(data.title, color = Color(0xFF7E8CA0), fontSize = 12.sp)
    }
}

/** XP, class XP (and optional secondary) as luminous bars — hierarchy tier 3. */
@Composable
internal fun ProgressionBars(
    data: StatusPrototypeData,
    accent: Color,
    xpFill: Float,
    classXpFill: Float,
    secondaryFill: Float,
) {
    Column(Modifier.fillMaxWidth()) {
        BarRow("XP", "${data.playerXpInLevel} / ${data.playerXpForLevel}", xpFill, accent)
        Spacer(Modifier.height(12.dp))
        BarRow(
            "${data.variant.displayName} · Lv ${data.classLevel}",
            "${data.classXpInLevel} / ${data.classXpForLevel}",
            classXpFill,
            accent.copy(alpha = 0.85f),
        )
        if (data.secondaryClassXpFraction != null && data.secondaryVariant != null) {
            Spacer(Modifier.height(12.dp))
            BarRow(
                "${data.secondaryVariant.displayName} (secondary)",
                "${data.secondaryClassXpInLevel} / ${data.secondaryClassXpForLevel}",
                secondaryFill,
                Color(0xFF8A97AC),
            )
        }
    }
}

@Composable
private fun BarRow(
    label: String,
    value: String,
    fraction: Float,
    accent: Color,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFFB6C2D4), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value, color = Color(0xFF8A97AC), fontSize = 11.sp)
        }
        Spacer(Modifier.height(5.dp))
        LuminBar(fraction, accent)
    }
}

/** The five universal attributes — staggered reveal + pulse driven from motion. */
@Composable
internal fun AttributeMeters(
    data: StatusPrototypeData,
    reveals: List<Float>,
    values: List<Float>,
    pulses: List<Float>,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("Attributes")
        Spacer(Modifier.height(10.dp))
        data.attributes.forEachIndexed { i, line ->
            val accent = AttributeAccent[line.name] ?: Color(0xFF8AA0B8)
            val reveal = reveals.getOrElse(i) { 1f }
            val value = values.getOrElse(i) { line.value.toFloat() }
            val pulse = pulses.getOrElse(i) { 1f }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = reveal
                            translationY = (1f - reveal) * 24.dp.toPx()
                            scaleX = pulse
                            scaleY = pulse
                        },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(8.dp)) { drawCircle(accent) }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(line.name, color = Color(0xFFCAD4E2), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    LuminBar((value / ATTRIBUTE_CEILING).coerceIn(0f, 1f), accent, height = 5)
                }
                Text(
                    value.roundToInt().toString(),
                    color = if (line.emphasized) accent else Color(0xFFEAEEF6),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** Unique class proficiency — hierarchy tier 5, pulses on a proficiency event. */
@Composable
internal fun ProficiencyBlock(
    data: StatusPrototypeData,
    accent: Color,
    reveal: Float,
    pulse: Float,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = reveal
                    scaleX = pulse
                    scaleY = pulse
                },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(data.variant.proficiencyName.uppercase(), color = accent, fontSize = 11.sp, letterSpacing = 2.sp)
            Text("Class proficiency", color = Color(0xFF7E8CA0), fontSize = 11.sp)
        }
        Text(data.uniqueProficiency.toString(), color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/** Recent progression + readiness + PR — hierarchy tier 6, and secondary metadata below. */
@Composable
internal fun ProgressionInfo(
    data: StatusPrototypeData,
    accent: Color,
    reveal: Float,
) {
    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = reveal }) {
        SectionLabel("Adaptive training")
        Spacer(Modifier.height(8.dp))
        InfoLine("Focus", data.trainingFocus, accent)
        InfoLine("Readiness", data.readinessState, accent)
        InfoLine("Recent", data.recentProgressionEvent, accent)
        data.pendingRecommendation?.let { InfoLine("Recommendation", it, accent, highlight = true) }
        data.recentPersonalRecord?.let { InfoLine("Personal record", it, accent, highlight = true) }
        Spacer(Modifier.height(14.dp))
        SectionLabel("Daily quest")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(data.questName, color = Color(0xFFCAD4E2), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${data.questProgress} / ${data.questTarget}", color = Color(0xFF8A97AC), fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        LuminBar(data.questFraction, accent, height = 6)
        data.questIntervalState?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Color(0xFF7E8CA0), fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
    accent: Color,
    highlight: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color(0xFF7E8CA0), fontSize = 12.sp, modifier = Modifier.width(112.dp))
        Text(
            value,
            color = if (highlight) accent else Color(0xFFCAD4E2),
            fontSize = 12.sp,
            fontWeight = if (highlight) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = Color(0xFF6E7C90), fontSize = 11.sp, letterSpacing = 2.sp, textAlign = TextAlign.Start)
}
