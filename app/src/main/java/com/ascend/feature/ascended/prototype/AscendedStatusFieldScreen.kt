package com.ascend.feature.ascended.prototype

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.feature.ascended.model.AscendedClass
import com.ascend.feature.ascended.model.BodyBase
import com.ascend.feature.ascended.model.EvolutionStage
import com.ascend.feature.ascended.presentation.AscendedStatusField
import com.ascend.feature.ascended.prototype.controls.AscendedControls
import com.ascend.feature.dashboard.prototype.StatusPalette

/**
 * Debug-only prototype for the "Your Ascended" DARK-FIELD integration — the imported figure on the
 * holographic Status field (deep atmosphere, drifting fog/particles, floor sigil), with a luminous
 * backing so the dark art reads. Class / body-base / stage / Perception controls, plus reduced
 * motion. Deterministic; no production Status wiring; art stays debug-only.
 */
@Composable
fun AscendedStatusFieldScreen(modifier: Modifier = Modifier) {
    var ascendedClass by remember { mutableStateOf(AscendedClass.GUARDIAN) }
    var bodyBase by remember { mutableStateOf(BodyBase.MALE) }
    var stage by remember { mutableStateOf(EvolutionStage.BASE) }
    var perceptionLevel by remember { mutableStateOf(0) }
    var reducedMotion by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(StatusPalette.groundDeep)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("YOUR ASCENDED", color = StatusPalette.label, fontSize = 12.sp, letterSpacing = 4.sp)
            Spacer(Modifier.height(6.dp))
            Text(ascendedClass.displayName, color = StatusPalette.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text("${bodyBase.label} · ${stage.displayName}", color = StatusPalette.violetBright, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))

            AscendedStatusField(
                ascendedClass = ascendedClass,
                bodyBase = bodyBase,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.62f),
                stage = stage,
                perceptionLevel = perceptionLevel,
                reducedMotion = reducedMotion,
            )

            Spacer(Modifier.height(16.dp))
            AscendedControls(
                ascendedClass = ascendedClass,
                bodyBase = bodyBase,
                stage = stage,
                perceptionLevel = perceptionLevel,
                onClass = { ascendedClass = it },
                onBodyBase = { bodyBase = it },
                onStage = { stage = it },
                onPerception = { perceptionLevel = it },
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = reducedMotion, onCheckedChange = { reducedMotion = it })
                Text("  Reduced motion", color = StatusPalette.textMuted)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
