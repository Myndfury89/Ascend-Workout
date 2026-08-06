package com.ascend.feature.ascended.prototype

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ascend.feature.ascended.prototype.body.BaseMannequin
import com.ascend.feature.ascended.prototype.layers.AscendedFieldBackground
import com.ascend.feature.ascended.prototype.layers.AuraLayer
import com.ascend.feature.ascended.prototype.layers.ClassSilhouetteLayer
import com.ascend.feature.ascended.prototype.layers.EquipmentOverlayLayer
import com.ascend.feature.ascended.prototype.layers.FloorSigilLayer
import com.ascend.feature.ascended.prototype.layers.SkillManifestationLayer
import com.ascend.feature.ascended.prototype.model.AscendedState
import com.ascend.feature.ascended.prototype.model.ambientRunning
import com.ascend.feature.dashboard.prototype.StatusPalette

/**
 * The "Your Ascended" character field. Composes the fixed layer order — atmosphere → floor sigil →
 * aura → base mannequin → class → equipment → Skill — so later checkpoints only fill in the empty
 * seams. Ambient motion (atmosphere drift, floor-sigil rotation, aura pulse) is driven here and
 * stops entirely under reduced motion; the mannequin geometry itself is always static.
 */
@Composable
fun AscendedCharacterViewport(
    state: AscendedState,
    modifier: Modifier = Modifier,
    seams: Boolean = false,
) {
    val running = ambientRunning(state)
    val accent = accentFor(state)

    val infinite = rememberInfiniteTransition(label = "ascended")
    val sweep by infinite.animateFloat(
        0f,
        if (running) 1f else 0f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep",
    )
    val rotation by infinite.animateFloat(
        0f,
        if (running) (2f * Math.PI.toFloat()) else 0f,
        infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
        label = "floorRotation",
    )
    val pulse by infinite.animateFloat(
        0f,
        if (running) 1f else 0f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "auraPulse",
    )

    Box(modifier) {
        AscendedFieldBackground(sweep = sweep, running = running)
        FloorSigilLayer(accent = accent, rotation = rotation)
        AuraLayer(state = state, accent = accent, pulse = pulse)
        BaseMannequin(bodyBase = state.bodyBase, modifier = Modifier.fillMaxSize(), seams = seams)
        ClassSilhouetteLayer(state = state)
        EquipmentOverlayLayer(state = state)
        SkillManifestationLayer(state = state)
    }
}

/** Base state is class-neutral, so it uses a neutral violet; class accents arrive in CP2. */
private fun accentFor(state: AscendedState): Color = if (state.hasClassIdentity) StatusPalette.violetBright else StatusPalette.violet
