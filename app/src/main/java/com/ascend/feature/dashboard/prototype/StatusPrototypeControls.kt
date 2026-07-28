package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The transient event overlay pill raised over the panel; animated by [reveal] and [flash]. */
@Composable
internal fun EventOverlayChip(
    overlay: StatusEventOverlay,
    accent: Color,
    reveal: Float,
    flash: Float,
) {
    if (overlay.kind == StatusOverlayKind.NONE || reveal <= 0.01f) return
    Surface(
        color = Color(0xFF10151F).copy(alpha = 0.92f),
        shape = RoundedCornerShape(10.dp),
        modifier =
            Modifier.graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * -16.dp.toPx()
                val s = 0.96f + 0.04f * reveal + 0.04f * flash
                scaleX = s
                scaleY = s
            },
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp).semantics {
                contentDescription = "Event: ${overlay.title}. ${overlay.detail}"
            },
        ) {
            Text(overlay.title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (overlay.detail.isNotEmpty()) {
                Text(overlay.detail, color = Color(0xFF9AA6B8), fontSize = 12.sp)
            }
        }
    }
}

/**
 * Debug-only review controls, grouped by concern. All state lives in [PrototypeReviewController];
 * these are never placed inside production Status components. Selecting a state re-triggers its
 * animation, so the state chips double as fake-event triggers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatusPrototypeControls(
    controller: PrototypeReviewController,
    modifier: Modifier = Modifier,
) {
    Surface(color = Color(0xFF10141C), shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Section("State / fake event") {
                Chips(StatusPrototypeStateId.entries, controller.stateId, { it.label }) { controller.selectState(it) }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Action("‹ Prev", Modifier.weight(1f)) { controller.previousState() }
                    Action("Replay", Modifier.weight(1f)) { controller.replay() }
                    Action("Next ›", Modifier.weight(1f)) { controller.nextState() }
                }
            }
            Section("Class") {
                Chips(StatusClassVariant.entries, controller.variant, { it.displayName }) { controller.variant = it }
                ToggleRow("Force secondary class", controller.forceSecondary) { controller.forceSecondary = it }
            }
            Section("Ornate sigil") {
                Chips(rankOptions, controller.rankTierOverride, { it?.name?.lowercase()?.replaceFirstChar(Char::uppercase) ?: "Auto" }) {
                    controller.rankTierOverride = it
                }
                Spacer(Modifier.height(6.dp))
                Chips(
                    medallionOptions,
                    controller.activeMedallionOverride ?: -1,
                    ::medallionLabel,
                ) { controller.activeMedallionOverride = it }
                Spacer(Modifier.height(6.dp))
                Chips(ringOptions, controller.playerRingOverride, { ringLabel("Player", it) }) { controller.playerRingOverride = it }
                Chips(ringOptions, controller.classRingOverride, { ringLabel("Class", it) }) { controller.classRingOverride = it }
                Chips(opacityOptions, controller.sigilOpacity, ::opacityLabel) { controller.sigilOpacity = it }
                ToggleRow(
                    "Show proficiency medallion",
                    controller.showProficiencyOverride ?: true,
                ) { controller.showProficiencyOverride = it }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Action("Level-up", Modifier.weight(1f)) { controller.selectState(StatusPrototypeStateId.PLAYER_LEVEL_UP) }
                    Action("Class LvUp", Modifier.weight(1f)) { controller.selectState(StatusPrototypeStateId.CLASS_LEVEL_UP) }
                    Action("Rank up", Modifier.weight(1f)) { controller.selectState(StatusPrototypeStateId.RANK_PROMOTION) }
                }
            }
            Section("Entrance mode") {
                Chips(EntranceMode.entries, controller.entranceMode, { it.label }) { controller.entranceMode = it }
            }
            Section("Motion speed") {
                Chips(MotionSpeed.entries, controller.motionSpeed, { it.label }) { controller.motionSpeed = it }
            }
            Section("Effects quality") {
                Chips(EffectsQuality.entries, controller.effectsQuality, { it.label }) { controller.effectsQuality = it }
            }
            Section("Device width") {
                Chips(DeviceWidth.entries, controller.deviceWidth, { it.label }) { controller.deviceWidth = it }
            }
            Section("Display") {
                ToggleRow("Reduced motion", controller.reducedMotion) { controller.reducedMotion = it }
                ToggleRow("Text-stress data", controller.textStress) { controller.textStress = it }
                ToggleRow("Hierarchy overlay", controller.hierarchyOverlay) { controller.hierarchyOverlay = it }
                ToggleRow("Diagnostics overlay", controller.diagnosticsOverlay) { controller.diagnosticsOverlay = it }
            }
            Section("Looped review") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Action(if (controller.loopRunning) "Stop" else "Start", Modifier.weight(1f)) {
                        if (controller.loopRunning) controller.stopLoop() else controller.startLoop()
                    }
                    Action(if (controller.loopPaused) "Resume" else "Pause", Modifier.weight(1f)) {
                        if (controller.loopPaused) controller.resumeLoop() else controller.pauseLoop()
                    }
                    Action("Restart", Modifier.weight(1f)) { controller.restartSequence() }
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(title.uppercase(), color = Color(0xFF6E7C90), fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    content()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option), fontSize = 11.sp) },
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "$label ${if (checked) "on" else "off"}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = checked, onCheckedChange = onChange)
        Text("  $label", color = Color(0xFFCAD4E2), fontSize = 13.sp)
    }
}

private val rankOptions: List<RankTier?> = listOf(null) + RankTier.entries
private val medallionOptions: List<Int> = listOf(-1, 0, 1, 2, 3, 4)
private val ringOptions: List<Float?> = listOf(null, 0f, 0.5f, 1f)
private val opacityOptions: List<Float> = listOf(0.12f, 0.16f, 0.30f)

private fun medallionLabel(index: Int): String =
    when (index) {
        0 -> "Str"
        1 -> "End"
        2 -> "Agi"
        3 -> "Dis"
        4 -> "Rec"
        else -> "None"
    }

private fun ringLabel(
    prefix: String,
    fraction: Float?,
): String = if (fraction == null) "$prefix auto" else "$prefix ${(fraction * 100).toInt()}%"

private fun opacityLabel(value: Float): String =
    when {
        value <= 0.13f -> "Faint"
        value <= 0.17f -> "Default"
        else -> "Strong"
    }

@Composable
private fun Action(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        color = Color(0xFF1B2130),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Text(
            label,
            color = Color(0xFF3FD9C7),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
        )
    }
}
