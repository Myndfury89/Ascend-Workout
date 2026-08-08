package com.ascend.feature.dashboard.prototype.resources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.feature.dashboard.prototype.StatusPalette

/**
 * Debug-only prototype for the HP/MP/XP daily-resource HUD. A scenario switcher drives the REAL
 * resolvers (HP / MP-evidence / training-target) through every device-availability state so the
 * banded HUD, dedup, rest-day, and unavailable-vs-zero behaviour can be reviewed. Deterministic fake
 * inputs; read-only; no repositories, no schema.
 */
@Composable
fun DailyResourcePrototypeScreen(modifier: Modifier = Modifier) {
    var scenario by remember { mutableStateOf(DailyResourceScenario.FULL) }
    val inputs = DailyResourceFixtures.inputs(scenario)
    val hp = DailyHpResolver.resolve(inputs.movement)
    val mpTarget = DailyTrainingTargetResolver.resolve(inputs.targetInput)
    val mp = MpEvidenceResolver.resolve(inputs.evidence, mpTarget)

    Box(modifier.fillMaxSize().background(StatusPalette.groundDeep)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text("DAILY RESOURCES", color = StatusPalette.label, fontSize = 12.sp, letterSpacing = 4.sp)
            Spacer(Modifier.height(4.dp))
            Text("HP · MP · XP prototype", color = StatusPalette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            ScenarioRow(DailyResourceScenario.entries.take(3), scenario) { scenario = it }
            Spacer(Modifier.height(8.dp))
            ScenarioRow(DailyResourceScenario.entries.drop(3), scenario) { scenario = it }

            Spacer(Modifier.height(24.dp))
            DailyResourceHud(hp = hp, mp = mp, xp = inputs.xp)
            Spacer(Modifier.height(20.dp))
            Text("Tap a resource to see its real metric and source.", color = StatusPalette.textMuted, fontSize = 12.sp)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ScenarioRow(
    scenarios: List<DailyResourceScenario>,
    selected: DailyResourceScenario,
    onSelect: (DailyResourceScenario) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        scenarios.forEach { s ->
            if (s == selected) {
                Button(
                    onClick = { onSelect(s) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusPalette.violet),
                ) {
                    Text(s.label, color = StatusPalette.textPrimary, fontSize = 12.sp, maxLines = 1)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(s) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(s.label, color = StatusPalette.textMuted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}
