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
import com.ascend.feature.ascended.prototype.body.BaseMannequin
import com.ascend.feature.ascended.prototype.body.ClassSilhouetteFigure
import com.ascend.feature.ascended.prototype.controls.ClassReviewControls
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase

/**
 * The debug-only "Your Ascended" review prototype — CP2: the flat class-silhouette shape-language
 * pass. On a clean LIGHT review background (per the shape-language brief) so the dark cut-paper
 * figures read by silhouette. Switch between the seven classes (plus the CP1 anatomical Base) and
 * the male/female body base; toggle silhouette-only and shape-layer review. Deterministic — no
 * repositories, no navigation, no physiology, no schema. Integrating these figures onto the dark
 * holographic Your Ascended field (value inversion) is a later checkpoint.
 */
private val REVIEW_BG = Color(0xFFEDEBE6)
private val REVIEW_INK = Color(0xFF23262E)
private val REVIEW_MUTED = Color(0xFF6A6E78)

@Composable
fun YourAscendedPrototypeScreen(modifier: Modifier = Modifier) {
    var bodyBase by remember { mutableStateOf(BodyBase.MALE) }
    var selectedClass by remember { mutableStateOf<AscendedClass?>(AscendedClass.MAGICIAN) }
    var silhouetteOnly by remember { mutableStateOf(false) }
    var showLayers by remember { mutableStateOf(false) }
    var reducedMotion by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(REVIEW_BG)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReviewHeader(selectedClass, bodyBase)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(0.62f), contentAlignment = Alignment.Center) {
                val figureModifier = Modifier.fillMaxSize()
                if (selectedClass != null) {
                    ClassSilhouetteFigure(selectedClass!!, bodyBase, figureModifier, silhouetteOnly, showLayers)
                } else {
                    BaseMannequin(bodyBase, figureModifier, seams = showLayers)
                }
            }
            Spacer(Modifier.height(16.dp))
            ClassReviewControls(
                selectedClass = selectedClass,
                bodyBase = bodyBase,
                silhouetteOnly = silhouetteOnly,
                showLayers = showLayers,
                reducedMotion = reducedMotion,
                onSelectClass = { selectedClass = it },
                onBodyBase = { bodyBase = it },
                onSilhouetteOnly = { silhouetteOnly = it },
                onShowLayers = { showLayers = it },
                onReducedMotion = { reducedMotion = it },
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ReviewHeader(
    selectedClass: AscendedClass?,
    bodyBase: BodyBase,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("YOUR ASCENDED", color = REVIEW_MUTED, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(6.dp))
        Text(selectedClass?.displayName ?: "Base Mannequin", color = REVIEW_INK, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("BASE", bodyBase.name)
            Stat("STAGE", "Base")
            Stat("SET", if (selectedClass?.production == false) "Preview" else "Live")
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
