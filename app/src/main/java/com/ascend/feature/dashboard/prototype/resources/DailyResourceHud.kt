package com.ascend.feature.dashboard.prototype.resources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.feature.dashboard.prototype.StatusPalette
import java.util.Locale

/*
 * The HP / MP / XP daily-resource HUD. HP (Vitality) and MP (Energy) show a neutral progress BAND
 * (Low → Full) with the raw fraction; XP (Growth) shows level progress. Unavailable and rest-day
 * states render explicitly (never 0/0 or a hollow bar). Tapping a row reveals the real metric and
 * its source — the fantasy abstraction is always explainable. Read-only: no progression mutation.
 */
private val HP_ACCENT = StatusPalette.cyan
private val MP_ACCENT = StatusPalette.violetBright
private val XP_ACCENT = StatusPalette.ember

@Composable
fun DailyResourceHud(
    hp: DailyHpState,
    mp: DailyMpState,
    xp: XpState,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val hpAvailable = hp.availability == ResourceAvailability.AVAILABLE
        val hpSummary =
            if (hpAvailable) {
                "HP Vitality: ${grouped(
                    hp.currentSteps,
                )} of ${hp.targetSteps?.let(::grouped) ?: "no"} steps, ${bandLabel(hp.progressFraction)}"
            } else {
                "HP Vitality: movement data unavailable"
            }
        ResourceRow(
            accent = HP_ACCENT,
            label = "HP — VITALITY",
            summary = hpSummary,
            fraction = if (hpAvailable) hp.progressFraction else null,
            primary = {
                if (hpAvailable) {
                    ValueLine(hp.currentSteps, hp.targetSteps, "Steps", hp.progressFraction, HP_ACCENT)
                } else {
                    StateText("Movement data unavailable", null)
                }
            },
            detail = {
                Detail("Steps", grouped(hp.currentSteps))
                Detail("Target", hp.targetSteps?.let(::grouped) ?: "—")
                Detail("Progress", percentOrDash(hp.progressFraction))
                Detail("Source", hp.source.name)
                Detail("Availability", hp.availability.name)
            },
        )

        val rest = mp.availability == ResourceAvailability.REST_DAY
        val mpSummary =
            if (rest) {
                "MP Energy: recovery day, rest prescribed"
            } else {
                "MP Energy: ${mp.verifiedMinutes} of ${mp.target.targetMinutes} minutes, ${bandLabel(mp.progressFraction)}"
            }
        ResourceRow(
            accent = MP_ACCENT,
            label = "MP — ENERGY",
            summary = mpSummary,
            fraction = if (rest) null else mp.progressFraction,
            primary = {
                if (rest) {
                    StateText("Recovery day", "Rest prescribed")
                } else {
                    ValueLine(mp.verifiedMinutes, mp.target.targetMinutes, "Training · min", mp.progressFraction, MP_ACCENT)
                }
            },
            detail = {
                if (rest) {
                    Detail("State", "Recovery day")
                    Detail("Reason", mp.target.explanation)
                } else {
                    Detail("Verified", "${mp.verifiedMinutes} min")
                    Detail("Target", "${mp.target.targetMinutes} min · ${mp.target.type.name}")
                    Detail("Progress", percentOrDash(mp.progressFraction))
                    Detail("Sources", mp.includedSources.joinToString(", ") { it.name }.ifEmpty { "—" })
                    Detail("Duplicates suppressed", mp.suppressedDuplicateCount.toString())
                }
            },
        )

        ResourceRow(
            accent = XP_ACCENT,
            label = "XP — GROWTH",
            summary = "XP Growth: ${grouped(xp.currentLevelXp.toInt())} of ${grouped(xp.xpForNextLevel.toInt())}, level ${xp.level}",
            fraction = xp.progressFraction,
            primary = {
                Text(
                    "${grouped(xp.currentLevelXp.toInt())} / ${grouped(xp.xpForNextLevel.toInt())}",
                    color = StatusPalette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Level ${xp.level}", color = XP_ACCENT, fontSize = 12.sp)
            },
            detail = {
                Detail("Current XP", grouped(xp.currentLevelXp.toInt()))
                Detail("Next level", grouped(xp.xpForNextLevel.toInt()))
                Detail("Level", xp.level.toString())
                Detail("Source", "PlayerProgress")
            },
        )
    }
}

@Composable
private fun ResourceRow(
    accent: Color,
    label: String,
    summary: String,
    fraction: Float?,
    primary: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .semantics { contentDescription = summary }
            .padding(vertical = 4.dp),
    ) {
        Text(label, color = accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        primary()
        if (fraction != null) {
            Spacer(Modifier.height(6.dp))
            ProgressBar(fraction, accent)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            detail()
        }
    }
}

@Composable
private fun ValueLine(
    current: Int,
    target: Int?,
    unit: String,
    fraction: Float?,
    accent: Color,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "${grouped(current)} / ${target?.let(::grouped) ?: "—"}",
            color = StatusPalette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Text(bandLabel(fraction), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(unit, color = StatusPalette.textMuted, fontSize = 12.sp)
        Text(percentOrDash(fraction), color = StatusPalette.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun StateText(
    text: String,
    subtitle: String?,
) {
    Text(text, color = StatusPalette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    if (subtitle != null) {
        Text(subtitle, color = StatusPalette.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun Detail(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = StatusPalette.label, fontSize = 11.sp)
        Text(value, color = StatusPalette.infoLine, fontSize = 11.sp)
    }
    Spacer(Modifier.height(3.dp))
}

@Composable
private fun ProgressBar(
    fraction: Float,
    accent: Color,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(StatusPalette.label.copy(alpha = 0.25f), RoundedCornerShape(3.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .background(accent, RoundedCornerShape(3.dp)),
        )
    }
}

private fun bandLabel(fraction: Float?): String = if (fraction == null) "—" else resourceBand(fraction).label

private fun percentOrDash(fraction: Float?): String = if (fraction == null) "—" else "${(fraction * 100).toInt()}%"

private fun grouped(value: Int): String = String.format(Locale.US, "%,d", value)
