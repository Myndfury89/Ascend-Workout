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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.feature.ascended.model.AscendedClass
import com.ascend.feature.ascended.model.BodyBase
import com.ascend.feature.ascended.model.EvolutionStage
import com.ascend.feature.ascended.presentation.AscendedFigure
import com.ascend.feature.ascended.prototype.controls.AscendedControls

/**
 * The debug-only "Your Ascended" review prototype — image-driven. The character field shows imported
 * per-class artwork (per body base, optional per-stage), with the aura + Perception effects layered
 * on top and evolution-stage / Skill controls below. Clean light review surface so the grayscale art
 * reads. Deterministic — no repositories, no navigation, no physiology, no schema. Until the figure
 * images are dropped into `app/src/debug/res/drawable/`, each figure shows a placeholder naming the
 * exact file to add.
 */
private val REVIEW_BG = Color(0xFFEDEBE6)
private val REVIEW_INK = Color(0xFF23262E)
private val REVIEW_MUTED = Color(0xFF6A6E78)

@Composable
fun YourAscendedPrototypeScreen(modifier: Modifier = Modifier) {
    var ascendedClass by remember { mutableStateOf(AscendedClass.GUARDIAN) }
    var bodyBase by remember { mutableStateOf(BodyBase.MALE) }
    var stage by remember { mutableStateOf(EvolutionStage.BASE) }
    var perceptionLevel by remember { mutableStateOf(0) }

    Box(modifier.fillMaxSize().background(REVIEW_BG)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReviewHeader(ascendedClass, bodyBase, stage)
            Spacer(Modifier.height(12.dp))
            AscendedFigure(
                ascendedClass = ascendedClass,
                bodyBase = bodyBase,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.62f),
                stage = stage,
                perceptionLevel = perceptionLevel,
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
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ReviewHeader(
    ascendedClass: AscendedClass,
    bodyBase: BodyBase,
    stage: EvolutionStage,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("YOUR ASCENDED", color = REVIEW_MUTED, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(6.dp))
        Text(ascendedClass.displayName, color = REVIEW_INK, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("BASE", bodyBase.label)
            Stat("STAGE", stage.displayName)
            Stat("SET", if (ascendedClass.production) "Live" else "Preview")
        }
    }
}

@Composable
private fun Stat(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = REVIEW_INK, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(label, color = REVIEW_MUTED, fontSize = 9.sp, letterSpacing = 1.5.sp)
    }
}
