package com.ascend.feature.build

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.core.domain.build.BuildCharacteristic
import com.ascend.core.domain.build.BuildTrend
import com.ascend.core.domain.build.CharacteristicScore
import com.ascend.core.domain.build.ClassAffinity
import com.ascend.core.domain.build.EvidenceState
import kotlin.math.roundToInt

/*
 * Read-only "Build Analysis": what the player's recent verified training resembles. Shows each
 * characteristic (score + trend + confidence, with explicit Insufficient / Not-tracked states) and a
 * truthful class-resemblance ranking. The player's current class is shown BESIDE the ranking, never
 * anchored inside it, so the ranking stays honest. Self-contained styling; no prototype dependency;
 * no progression is affected.
 */
private val BG = Color(0xFF04050B)
private val PANEL = Color(0xFF10131F)
private val INK = Color(0xFFEAF0FF)
private val MUTED = Color(0xFF8A93B5)
private val FAINT = Color(0xFF5A6180)
private val ACCENT = Color(0xFFA88BFF)
private val TRACK = Color(0xFF1C2033)

@Composable
fun BuildIdentityScreen(viewModel: BuildIdentityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BuildIdentityContent(state)
}

@Composable
fun BuildIdentityContent(
    state: BuildIdentityUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(BG), contentAlignment = Alignment.Center) {
        if (state.loading) {
            CircularProgressIndicator(color = ACCENT)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 22.dp),
            ) {
                Text("BUILD ANALYSIS", color = MUTED, fontSize = 12.sp, letterSpacing = 4.sp)
                Spacer(Modifier.height(4.dp))
                Text("What your recent training resembles", color = INK, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

                state.currentClassName?.let { CurrentClassChip(it, state.currentClass) }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Build characteristics")
                state.characteristics.forEach { CharacteristicRow(it) }

                Spacer(Modifier.height(22.dp))
                SectionLabel("Your strongest affinities")
                state.affinities.forEach { ClassAffinityRow(it, isDominant = it.buildClass == state.dominant?.buildClass) }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun CurrentClassChip(
    name: String,
    affinity: ClassAffinity?,
) {
    val suffix = affinity?.let { " · Affinity ${pct(it.affinity)}%" } ?: ""
    Spacer(Modifier.height(14.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PANEL).padding(14.dp),
    ) {
        Text("CURRENT CLASS", color = FAINT, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text("$name$suffix", color = ACCENT, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = MUTED, fontSize = 11.sp, letterSpacing = 2.sp)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun CharacteristicRow(score: CharacteristicScore) {
    val name = titleCase(score.characteristic)
    val evidenced = score.state == EvidenceState.OK || score.state == EvidenceState.ZERO
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = INK, fontSize = 15.sp)
            if (evidenced) {
                Text("${pct(score.score / SCORE_MAX)}", color = INK, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            } else {
                // Low-evidence: the state, not a number, is the prominent element.
                Text(stateShort(score.state), color = MUTED, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(6.dp))
        Bar(fraction = if (evidenced) (score.score / SCORE_MAX).toFloat().coerceIn(0f, 1f) else 0f, muted = !evidenced)
        Spacer(Modifier.height(4.dp))
        Text(caption(score), color = if (evidenced) FAINT else MUTED, fontSize = 11.sp)
    }
}

@Composable
private fun Bar(
    fraction: Float,
    muted: Boolean,
) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(TRACK)) {
        if (!muted && fraction > 0f) {
            Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp)).background(ACCENT))
        }
    }
}

@Composable
private fun ClassAffinityRow(
    affinity: ClassAffinity,
    isDominant: Boolean,
) {
    val bg = if (isDominant) PANEL else Color.Transparent
    Column(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(10.dp)).background(bg).padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(affinity.buildClass.displayName, color = INK, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (!affinity.active) {
                    Text("Potential · Yet to Awaken", color = FAINT, fontSize = 11.sp)
                }
            }
            if (affinity.dominantEligible) {
                Text("${pct(affinity.affinity)}%", color = ACCENT, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                // Coverage is too thin to trust the score — say so louder than the number.
                Text("Insufficient evidence", color = MUTED, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ---- display helpers ----

private const val SCORE_MAX = 100.0

private fun pct(fraction: Double): Int = (fraction * SCORE_MAX).roundToInt()

private fun titleCase(characteristic: BuildCharacteristic): String = characteristic.name.lowercase().replaceFirstChar { it.uppercase() }

private fun trendLabel(trend: BuildTrend): String =
    when (trend) {
        BuildTrend.DEVELOPING -> "Developing ↑"
        BuildTrend.MAINTAINING -> "Maintaining →"
        BuildTrend.DE_EMPHASIZED -> "De-emphasized ↓"
        BuildTrend.UNKNOWN -> ""
    }

private fun confidenceLabel(confidence: Double): String =
    when {
        confidence >= 0.66 -> "High confidence"
        confidence >= 0.33 -> "Medium confidence"
        else -> "Low confidence"
    }

private fun stateShort(state: EvidenceState): String =
    when (state) {
        EvidenceState.INSUFFICIENT -> "Insufficient"
        EvidenceState.UNAVAILABLE -> "Not tracked"
        else -> ""
    }

private fun caption(score: CharacteristicScore): String =
    when (score.state) {
        EvidenceState.OK ->
            listOf(
                trendLabel(score.trend),
                confidenceLabel(score.confidence),
            ).filter { it.isNotEmpty() }.joinToString(" · ")
        EvidenceState.ZERO -> "No recent emphasis"
        EvidenceState.INSUFFICIENT -> "Insufficient evidence yet"
        EvidenceState.UNAVAILABLE -> "Not tracked yet"
    }
