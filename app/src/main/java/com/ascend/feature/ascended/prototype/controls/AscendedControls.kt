package com.ascend.feature.ascended.prototype.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.feature.ascended.model.AscendedClass
import com.ascend.feature.ascended.model.BodyBase
import com.ascend.feature.ascended.model.EvolutionStage

/**
 * Review controls for the image-driven "Your Ascended" prototype: class, body base, evolution stage,
 * and the Perception Skill level. Prototype-only — deterministic, no production wiring.
 */
@Composable
fun AscendedControls(
    ascendedClass: AscendedClass,
    bodyBase: BodyBase,
    stage: EvolutionStage,
    perceptionLevel: Int,
    onClass: (AscendedClass) -> Unit,
    onBodyBase: (BodyBase) -> Unit,
    onStage: (EvolutionStage) -> Unit,
    onPerception: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = Color(0xFFFFFFFF), shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Label("CLASS")
            ClassRow(AscendedClass.entries.take(4), ascendedClass, onClass)
            Spacer(Modifier.height(8.dp))
            ClassRow(AscendedClass.entries.drop(4), ascendedClass, onClass)

            Spacer(Modifier.height(16.dp))
            Label("BODY BASE")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BodyBase.entries.forEach { b ->
                    Toggle(b.label, b == bodyBase, { onBodyBase(b) }, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(16.dp))
            Label("EVOLUTION STAGE")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EvolutionStage.entries.forEach { s ->
                    Toggle(s.displayName, s == stage, { onStage(s) }, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(16.dp))
            Label("SKILL · PERCEPTION")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Off", 1 to "L1", 5 to "L5", 10 to "L10").forEach { (lvl, lbl) ->
                    Toggle(lbl, lvl == perceptionLevel, { onPerception(lvl) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ClassRow(
    classes: List<AscendedClass>,
    selected: AscendedClass,
    onSelect: (AscendedClass) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        classes.forEach { c ->
            Toggle(c.displayName, c == selected, { onSelect(c) }, Modifier.weight(1f))
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
            contentPadding = PaddingValues(horizontal = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23262E)),
        ) {
            Text(label, color = Color(0xFFF2F3F5), fontSize = 12.sp, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(42.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Text(label, color = Color(0xFF3A3D45), fontSize = 12.sp, maxLines = 1)
        }
    }
}
