package com.ascend.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.core.designsystem.motion.MotionSpec
import com.ascend.core.domain.progression.LevelState
import com.ascend.core.model.AttributeType
import kotlin.math.roundToInt

private val AttributeAccents: Map<AttributeType, Color> =
    mapOf(
        AttributeType.STRENGTH to Color(0xFFE8735A),
        AttributeType.ENDURANCE to Color(0xFF3FD9C7),
        AttributeType.AGILITY to Color(0xFF7EC46B),
        AttributeType.DISCIPLINE to Color(0xFF9B8CFF),
        AttributeType.RECOVERY to Color(0xFF62B6E8),
    )

// Soft ceiling for the per‑attribute meter fill (values can exceed it).
private const val ATTRIBUTE_METER_CEILING = 120f

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    viewModel: StatusMotionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.phase == StatusPhase.LOADING || state.domain == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val motion = MotionSpec(reducedMotion = state.reducedMotion)
    val anim = remember { StatusMotion(AttributeType.entries) }

    LaunchedEffect(state.entranceKey, state.domain != null) {
        state.domain?.let { anim.playEntrance(it, motion) }
    }
    LaunchedEffect(state.pendingBatch) {
        val batch = state.pendingBatch
        if (batch.isNotEmpty()) {
            anim.playBatch(batch, motion)
            viewModel.onBatchPlayed(batch.map { it.id })
        }
    }

    val lifetimeNow = anim.lifetime.value.toLong()
    val levelState = viewModel.levelState(lifetimeNow)
    val busy = state.isSimulating || state.pendingBatch.isNotEmpty()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
    ) {
        HunterHeader(
            hunterName = state.hunterName,
            rankLabel = anim.displayedRank.displayName,
            level = levelState.level,
            headerAlpha = anim.headerAlpha.value,
            levelFlash = anim.levelFlash.value,
            rankFlash = anim.rankFlash.value,
        )

        Spacer(Modifier.height(20.dp))
        XpBar(levelState = levelState, alpha = anim.xpBarAlpha.value)

        Spacer(Modifier.height(24.dp))
        Text("Attributes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        AttributeType.entries.forEach { attribute ->
            AttributeRow(
                attribute = attribute,
                value = anim.attrValue.getValue(attribute).value.roundToInt(),
                reveal = anim.attrReveal.getValue(attribute).value,
                pulse = anim.attrPulse.getValue(attribute).value,
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        PrototypeControls(
            reducedMotion = state.reducedMotion,
            busy = busy,
            onSimulate = viewModel::simulate,
            onReplay = viewModel::replayEntrance,
            onReset = viewModel::reset,
            onReducedMotionChange = viewModel::setReducedMotion,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HunterHeader(
    hunterName: String,
    rankLabel: String,
    level: Int,
    headerAlpha: Float,
    levelFlash: Float,
    rankFlash: Float,
) {
    val accent = MaterialTheme.colorScheme.primary
    val ember = MaterialTheme.colorScheme.secondary
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = headerAlpha }
                .drawBehind {
                    if (levelFlash > 0f) {
                        drawRect(accent.copy(alpha = 0.28f * levelFlash))
                    }
                    if (rankFlash > 0f) {
                        drawRect(ember.copy(alpha = 0.28f * rankFlash))
                    }
                },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(rankLabel.uppercase(), style = MaterialTheme.typography.labelLarge, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(hunterName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Level $level", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (levelFlash > 0.05f) {
                Text(
                    "LEVEL UP",
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    modifier = Modifier.graphicsLayer { alpha = levelFlash },
                )
            } else if (rankFlash > 0.05f) {
                Text(
                    "RANK UP",
                    style = MaterialTheme.typography.titleSmall,
                    color = ember,
                    modifier = Modifier.graphicsLayer { alpha = rankFlash },
                )
            }
        }
    }
}

@Composable
private fun XpBar(
    levelState: LevelState,
    alpha: Float,
) {
    Column(Modifier.graphicsLayer { this.alpha = alpha }) {
        LinearProgressIndicator(
            progress = { levelState.progressFraction },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${levelState.currentLevelXp} / ${levelState.xpToNextLevel} XP",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AttributeRow(
    attribute: AttributeType,
    value: Int,
    reveal: Float,
    pulse: Float,
) {
    val accent = AttributeAccents[attribute] ?: MaterialTheme.colorScheme.primary
    val meterFraction = (value / ATTRIBUTE_METER_CEILING).coerceIn(0f, 1f)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = reveal
                    translationY = (1f - reveal) * 28.dp.toPx()
                    scaleX = pulse
                    scaleY = pulse
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).drawBehind { drawRect(accent) })
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(attribute.displayName, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { meterFraction },
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            )
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun PrototypeControls(
    reducedMotion: Boolean,
    busy: Boolean,
    onSimulate: (SimulatedCompletion) -> Unit,
    onReplay: () -> Unit,
    onReset: () -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Prototype controls", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSimulate(SimulatedCompletion.LIGHT_QUEST) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(SimulatedCompletion.LIGHT_QUEST.label)
                }
                Button(onClick = { onSimulate(SimulatedCompletion.HEAVY_QUEST) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(SimulatedCompletion.HEAVY_QUEST.label)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onReplay, modifier = Modifier.weight(1f)) { Text("Replay entrance") }
                OutlinedButton(onClick = onReset, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Reset") }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = reducedMotion, onCheckedChange = onReducedMotionChange)
                Text("  Reduced motion", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
