package com.ascend.feature.ascended.prototype.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.feature.ascended.prototype.body.SilhouetteFidelity
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.EvolutionStage

/**
 * CP2 review controls for the class silhouette pass, on the clean light review surface. Class
 * selector (Base anatomical + the seven classes), male/female base, the visible-but-inactive
 * evolution stages (only Base is implemented this pass), and the review toggles: silhouette-only
 * (flatten to solid black for recognition) and show shape layers. Prototype-only.
 */
@Composable
fun ClassReviewControls(
    selectedClass: AscendedClass?,
    bodyBase: BodyBase,
    silhouetteOnly: Boolean,
    showLayers: Boolean,
    reducedMotion: Boolean,
    onSelectClass: (AscendedClass?) -> Unit,
    onBodyBase: (BodyBase) -> Unit,
    onSilhouetteOnly: (Boolean) -> Unit,
    onShowLayers: (Boolean) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    // CP3 review additions (defaulted so earlier call sites/tests stay valid).
    fidelity: SilhouetteFidelity = SilhouetteFidelity.BLOCKOUT,
    compare: Boolean = false,
    outlineOnly: Boolean = false,
    shapeCount: Int = 0,
    refinedAvailable: Boolean = false,
    onFidelity: (SilhouetteFidelity) -> Unit = {},
    onCompare: (Boolean) -> Unit = {},
    onOutline: (Boolean) -> Unit = {},
    stage: EvolutionStage = EvolutionStage.MASTERED,
    perceptionLevel: Int = 0,
    onStage: (EvolutionStage) -> Unit = {},
    onPerception: (Int) -> Unit = {},
) {
    Surface(color = Color(0xFFFFFFFF), shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Label("CLASS")
            ClassRow(listOf(null, AscendedClass.MAGICIAN, AscendedClass.BERSERKER, AscendedClass.MONK), selectedClass, onSelectClass)
            Spacer(Modifier.height(8.dp))
            ClassRow(
                listOf(AscendedClass.ASSASSIN, AscendedClass.FIGHTER, AscendedClass.RANGER, AscendedClass.GUARDIAN),
                selectedClass,
                onSelectClass,
            )

            Spacer(Modifier.height(16.dp))
            Label("BODY BASE")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BodyBase.entries.forEach { base ->
                    Toggle(base.name, base == bodyBase, { onBodyBase(base) }, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(16.dp))
            Label("EVOLUTION STAGE")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EvolutionStage.entries.forEach { s ->
                    Toggle(stageLabel(s), s == stage, { onStage(s) }, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(16.dp))
            Label("SKILL · PERCEPTION")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Off", 1 to "L1", 5 to "L5", 10 to "L10").forEach { (lvl, lbl) ->
                    Toggle(lbl, lvl == perceptionLevel, { onPerception(lvl) }, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(16.dp))
            Label(if (refinedAvailable) "FIDELITY · $shapeCount shapes" else "FIDELITY · $shapeCount shapes (blockout only)")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Toggle(
                    "CP2 Blockout",
                    fidelity == SilhouetteFidelity.BLOCKOUT,
                    { onFidelity(SilhouetteFidelity.BLOCKOUT) },
                    Modifier.weight(1f),
                )
                Toggle(
                    "Refined Base",
                    fidelity == SilhouetteFidelity.REFINED,
                    { onFidelity(SilhouetteFidelity.REFINED) },
                    Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))
            SwitchRow("Compare blockout vs refined", compare, onCompare)
            SwitchRow("Outline only", outlineOnly, onOutline)
            SwitchRow("Silhouette only", silhouetteOnly, onSilhouetteOnly)
            SwitchRow("Show shape layers", showLayers, onShowLayers)
            SwitchRow("Reduced motion", reducedMotion, onReducedMotion)
        }
    }
}

@Composable
private fun ClassRow(
    choices: List<AscendedClass?>,
    selected: AscendedClass?,
    onSelect: (AscendedClass?) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            Toggle(choice?.displayName ?: "Mannequin", choice == selected, { onSelect(choice) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = Color(0xFF6A6E78), fontSize = 11.sp, letterSpacing = 2.sp)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Toggle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(42.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23262E)),
        ) {
            Text(label, color = Color(0xFFF2F3F5), fontSize = 12.sp, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(42.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
        ) {
            Text(label, color = Color(0xFF3A3D45), fontSize = 12.sp, maxLines = 1)
        }
    }
}

private fun stageLabel(stage: EvolutionStage): String =
    when (stage) {
        EvolutionStage.BASE -> "Base"
        EvolutionStage.EARLY_GROWTH -> "Early"
        EvolutionStage.ADVANCED -> "Adv"
        EvolutionStage.MASTERED -> "Master"
    }

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onChange)
        Text("  $label", color = Color(0xFF3A3D45), fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(4.dp))
}
