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
import com.ascend.feature.ascended.prototype.controls.AscendedReviewControls
import com.ascend.feature.ascended.prototype.model.AscendedState
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.FakeAscended
import com.ascend.feature.dashboard.prototype.StatusPalette

/**
 * The debug-only "Your Ascended" review prototype (CP1). A portrait HUD: a header, the dominant
 * character field (the layered mannequin viewport), and the review controls. Deterministic fake
 * data only — no repositories, no navigation, no physiology, no schema. The male/female toggle
 * switches only the mannequin body base; everything else (fake identity) stays identical so the two
 * bases can be reviewed apples-to-apples.
 */
@Composable
fun YourAscendedPrototypeScreen(modifier: Modifier = Modifier) {
    var bodyBase by remember { mutableStateOf(BodyBase.MALE) }
    var reducedMotion by remember { mutableStateOf(false) }
    var seams by remember { mutableStateOf(false) }

    val state =
        remember(bodyBase, reducedMotion) {
            FakeAscended.base(bodyBase).copy(reducedMotion = reducedMotion)
        }

    Box(modifier.fillMaxSize().background(StatusPalette.groundDeep)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AscendedHeader(state)
            Spacer(Modifier.height(12.dp))
            AscendedCharacterViewport(
                state = state,
                seams = seams,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.56f),
            )
            Spacer(Modifier.height(16.dp))
            AscendedReviewControls(
                bodyBase = bodyBase,
                reducedMotion = reducedMotion,
                seams = seams,
                onBodyBase = { bodyBase = it },
                onReducedMotion = { reducedMotion = it },
                onSeams = { seams = it },
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AscendedHeader(state: AscendedState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("YOUR ASCENDED", color = StatusPalette.label, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(6.dp))
        Text("Kaiden", color = StatusPalette.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        // Base state is class-neutral — the class is only suggested, not yet expressed.
        Text("Unbound · The Unawakened", color = StatusPalette.violetBright, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeaderStat("LEVEL", "1")
            HeaderStat("RANK", "Iron")
            HeaderStat("STAGE", state.stage.displayName)
            HeaderStat("BASE", state.bodyBase.name)
        }
    }
}

@Composable
private fun HeaderStat(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = StatusPalette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(label, color = StatusPalette.label, fontSize = 9.sp, letterSpacing = 1.5.sp)
    }
}
