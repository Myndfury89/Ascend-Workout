package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ascend.core.designsystem.theme.AscendTheme

/*
 * Static previews of the revised composition for Android Studio. They render each state fully
 * "settled" (all reveals at 1, bars at their target) so the layout, sigil, edge-lit panel, and
 * hierarchy can be reviewed without running the entrance animation. Live motion is exercised in
 * the debug StatusPrototypeActivity.
 */

/** A non-animated render of the panel at its final values — preview + smoke-test friendly. */
@Composable
internal fun StaticStatusPanel(
    data: StatusPrototypeData,
    modifier: Modifier = Modifier,
) {
    val sigil = StatusSigilVariant.of(data.variant)
    val accent = sigil.core
    Box(modifier.background(Color(0xFF06080D)).padding(16.dp)) {
        Box {
            EdgeLitStatusPanel(accent = accent, materialize = 1f, scan = 0f, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    IdentityBlock(data, accent, 1f)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        StatusSigil(
                            state =
                                StatusSigilState(
                                    variant = sigil,
                                    tier = data.trainingTier,
                                    assembly = 1f,
                                    majorUnlock = data.majorUnlock,
                                    progressionActive = data.pendingRecommendation != null,
                                ),
                            animation = StatusSigilAnimation(1f, 18f, 1f),
                            modifier = Modifier.fillMaxWidth(0.5f).then(Modifier.padding(4.dp)),
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(10.dp))
                    ProgressionBars(data, accent, data.playerXpFraction, data.classXpFraction, data.secondaryClassXpFraction ?: 0f)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(12.dp))
                    AttributeMeters(
                        data,
                        reveals = List(data.attributes.size) { 1f },
                        values = data.attributes.map { it.value.toFloat() },
                        pulses = List(data.attributes.size) { 1f },
                    )
                    ProficiencyBlock(data, accent, 1f, 1f)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(10.dp))
                    ProgressionInfo(data, accent, 1f)
                }
            }
            Box(Modifier.align(Alignment.TopCenter)) {
                EventOverlayChip(data.overlay, accent, 1f, 0f)
            }
        }
    }
}

@Composable
private fun preview(
    state: StatusPrototypeStateId,
    variant: StatusClassVariant,
) {
    AscendTheme(darkTheme = true) {
        StaticStatusPanel(FakeStatusPrototype.dataFor(state, variant, reducedMotion = false))
    }
}

@Preview(name = "Standard · Berserker", backgroundColor = 0xFF06080D, showBackground = true, heightDp = 900)
@Composable
private fun PreviewStandardBerserker() = preview(StatusPrototypeStateId.STANDARD, StatusClassVariant.BERSERKER)

@Preview(name = "Standard · Monk", backgroundColor = 0xFF06080D, showBackground = true, heightDp = 900)
@Composable
private fun PreviewStandardMonk() = preview(StatusPrototypeStateId.STANDARD, StatusClassVariant.MONK)

@Preview(name = "Standard · Magician", backgroundColor = 0xFF06080D, showBackground = true, heightDp = 940)
@Composable
private fun PreviewStandardMagician() = preview(StatusPrototypeStateId.STANDARD, StatusClassVariant.MAGICIAN)

@Preview(name = "Player level-up", backgroundColor = 0xFF06080D, showBackground = true, heightDp = 900)
@Composable
private fun PreviewLevelUp() = preview(StatusPrototypeStateId.PLAYER_LEVEL_UP, StatusClassVariant.MONK)

@Preview(name = "Rank promotion", backgroundColor = 0xFF06080D, showBackground = true, heightDp = 900)
@Composable
private fun PreviewRankPromotion() = preview(StatusPrototypeStateId.RANK_PROMOTION, StatusClassVariant.MAGICIAN)

@Preview(name = "Personal record", backgroundColor = 0xFF06080D, showBackground = true, heightDp = 900)
@Composable
private fun PreviewPersonalRecord() = preview(StatusPrototypeStateId.PERSONAL_RECORD, StatusClassVariant.BERSERKER)

@Preview(name = "Recommendation", backgroundColor = 0xFF06080D, showBackground = true, heightDp = 900)
@Composable
private fun PreviewRecommendation() = preview(StatusPrototypeStateId.RECOMMENDATION, StatusClassVariant.BERSERKER)

@Preview(name = "Neutral (no class)", backgroundColor = 0xFF06080D, showBackground = true, heightDp = 900)
@Composable
private fun PreviewNeutral() = preview(StatusPrototypeStateId.STANDARD, StatusClassVariant.NEUTRAL)
