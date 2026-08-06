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
import com.ascend.feature.ascended.prototype.body.ClassSilhouetteGeometry
import com.ascend.feature.ascended.prototype.body.SilhouetteFidelity
import com.ascend.feature.ascended.prototype.body.classShapeCount
import com.ascend.feature.ascended.prototype.controls.ClassReviewControls
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase

/**
 * The debug-only "Your Ascended" review prototype. CP3 opens with the silhouette-refinement pass:
 * the figures can be viewed as the CP2 blockout or the Refined Base, side-by-side compared, and
 * outlined — so the refinement is judged directly before any progression overlays. Clean LIGHT
 * review background so the dark cut-paper figures read. Deterministic — no repositories, no
 * navigation, no physiology, no schema. Only Guardian is refined so far; others fall back to blockout.
 */
private val REVIEW_BG = Color(0xFFEDEBE6)
private val REVIEW_INK = Color(0xFF23262E)
private val REVIEW_MUTED = Color(0xFF6A6E78)

@Composable
fun YourAscendedPrototypeScreen(modifier: Modifier = Modifier) {
    var bodyBase by remember { mutableStateOf(BodyBase.MALE) }
    var selectedClass by remember { mutableStateOf<AscendedClass?>(AscendedClass.GUARDIAN) }
    var fidelity by remember { mutableStateOf(SilhouetteFidelity.REFINED) }
    var compare by remember { mutableStateOf(false) }
    var outlineOnly by remember { mutableStateOf(false) }
    var silhouetteOnly by remember { mutableStateOf(false) }
    var showLayers by remember { mutableStateOf(false) }
    var reducedMotion by remember { mutableStateOf(false) }

    val cls = selectedClass
    val refinedAvailable = cls != null && ClassSilhouetteGeometry.hasRefined(cls)
    val shapeCount = if (cls != null) classShapeCount(cls, bodyBase, fidelity) else 0

    Box(modifier.fillMaxSize().background(REVIEW_BG)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReviewHeader(cls, bodyBase, fidelity)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(0.62f), contentAlignment = Alignment.Center) {
                when {
                    cls == null -> BaseMannequin(bodyBase, Modifier.fillMaxSize(), seams = showLayers)
                    compare && refinedAvailable ->
                        Row(Modifier.fillMaxSize()) {
                            CompareFigure(
                                "CP2 Blockout",
                                cls,
                                bodyBase,
                                SilhouetteFidelity.BLOCKOUT,
                                silhouetteOnly,
                                showLayers,
                                outlineOnly,
                                Modifier.weight(1f),
                            )
                            CompareFigure(
                                "Refined Base",
                                cls,
                                bodyBase,
                                SilhouetteFidelity.REFINED,
                                silhouetteOnly,
                                showLayers,
                                outlineOnly,
                                Modifier.weight(1f),
                            )
                        }
                    else ->
                        ClassSilhouetteFigure(cls, bodyBase, Modifier.fillMaxSize(), fidelity, silhouetteOnly, showLayers, outlineOnly)
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
                fidelity = fidelity,
                compare = compare,
                outlineOnly = outlineOnly,
                shapeCount = shapeCount,
                refinedAvailable = refinedAvailable,
                onFidelity = { fidelity = it },
                onCompare = { compare = it },
                onOutline = { outlineOnly = it },
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CompareFigure(
    label: String,
    ascendedClass: AscendedClass,
    bodyBase: BodyBase,
    fidelity: SilhouetteFidelity,
    silhouetteOnly: Boolean,
    showLayers: Boolean,
    outlineOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ClassSilhouetteFigure(
            ascendedClass,
            bodyBase,
            Modifier.fillMaxWidth().weight(1f),
            fidelity,
            silhouetteOnly,
            showLayers,
            outlineOnly,
        )
        Text(label, color = REVIEW_MUTED, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun ReviewHeader(
    selectedClass: AscendedClass?,
    bodyBase: BodyBase,
    fidelity: SilhouetteFidelity,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("YOUR ASCENDED", color = REVIEW_MUTED, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(6.dp))
        Text(selectedClass?.displayName ?: "Base Mannequin", color = REVIEW_INK, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("BASE", bodyBase.name)
            Stat("STAGE", "Base")
            Stat("FIDELITY", if (fidelity == SilhouetteFidelity.REFINED) "Refined" else "Blockout")
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
