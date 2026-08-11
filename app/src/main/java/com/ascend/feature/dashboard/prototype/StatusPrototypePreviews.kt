package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ascend.core.designsystem.theme.AscendTheme

/*
 * Static, settled previews of the revised composition for Android Studio — screenshot-friendly:
 * they render each state fully settled (reveals at 1, bars at target) rather than relying on the
 * entrance animation finishing inside Preview. Live motion is exercised in the debug
 * StatusPrototypeActivity.
 */

/** A non-animated render of the panel at its final values — preview + smoke-test friendly. */
@Composable
internal fun StaticStatusPanel(
    data: StatusPrototypeData,
    modifier: Modifier = Modifier,
) {
    val accent = StatusSigilVariant.of(data.variant).core
    val hasClass = data.variant != StatusClassVariant.NEUTRAL
    Box(modifier.background(StatusPalette.groundDeep)) {
        StatusAtmosphere(sweep = 0.5f)
        StatusEnergyFrame(energy = 1f, pulse = 0f, rotation = 18f, frameMotion = false, modifier = Modifier.padding(12.dp)) {
            EdgeLitStatusPanel(accent = accent, materialize = 1f, scan = 0f, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().clipToBounds()) {
                    StaticOrnateSigil(data, hasClass, Modifier.align(Alignment.TopCenter))
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        IdentityBlock(data, accent, 1f)
                        Spacer(Modifier.padding(10.dp))
                        ProgressionBars(data, accent, data.playerXpFraction, data.classXpFraction, data.secondaryClassXpFraction ?: 0f)
                        Spacer(Modifier.padding(12.dp))
                        AttributeMeters(
                            data,
                            reveals = List(data.attributes.size) { 1f },
                            values = data.attributes.map { it.value.toFloat() },
                            pulses = List(data.attributes.size) { 1f },
                        )
                        ProficiencyBlock(data, accent, 1f, 1f)
                        Spacer(Modifier.padding(10.dp))
                        ProgressionInfo(data, accent, 1f)
                    }
                }
            }
            Box(Modifier.align(Alignment.TopCenter)) {
                EventOverlayChip(data.overlay, accent, 1f, 0f)
            }
        }
    }
}

/** A settled (non-animated) ornate sigil for static previews and smoke tests. */
@Composable
private fun StaticOrnateSigil(
    data: StatusPrototypeData,
    hasClass: Boolean,
    modifier: Modifier = Modifier,
) {
    OrnateSigil(
        state =
            OrnateSigilState(
                rankTier = data.rankTier,
                variant = data.variant,
                playerRing = data.playerXpFraction,
                classRing = if (hasClass) data.classXpFraction else null,
                activeMedallion = data.activeMedallionIndex,
                showProficiency = data.showProficiencyMedallion && hasClass,
                newRankLayer = data.stateId == StatusPrototypeStateId.RANK_PROMOTION,
            ),
        animation =
            OrnateSigilAnimation(
                assembly = 1f,
                rotation = 18f,
                glow = 0f,
                playerRingTrim = data.playerXpFraction,
                classRingTrim = data.classXpFraction,
                medallionPulse = List(5) { 1f },
                proficiencyPulse = 1f,
            ),
        semanticDescription =
            sigilDescription(data, data.playerXpFraction, if (hasClass) data.classXpFraction else null, data.activeMedallionIndex),
        modifier = modifier.fillMaxWidth(0.98f).aspectRatio(1f).offset(y = (-28).dp),
    )
}

@Composable
private fun preview(
    state: StatusPrototypeStateId,
    variant: StatusClassVariant,
    forceSecondary: Boolean = false,
) {
    AscendTheme(darkTheme = true) {
        StaticStatusPanel(FakeStatusPrototype.dataFor(state, variant, reducedMotion = false, forceSecondary = forceSecondary))
    }
}

@Preview(name = "1 · Neutral standard", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewNeutralStandard() = preview(StatusPrototypeStateId.STANDARD, StatusClassVariant.NEUTRAL)

@Preview(name = "2 · Berserker quest completion", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewBerserkerQuestComplete() = preview(StatusPrototypeStateId.QUEST_COMPLETE, StatusClassVariant.BERSERKER)

@Preview(name = "3 · Monk class level-up", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewMonkClassLevelUp() = preview(StatusPrototypeStateId.CLASS_LEVEL_UP, StatusClassVariant.MONK)

@Preview(name = "4 · Mage cardio progression", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 980)
@Composable
private fun PreviewMageCardio() = preview(StatusPrototypeStateId.RECOMMENDATION, StatusClassVariant.MAGE)

@Preview(name = "5 · Player level-up", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewPlayerLevelUp() = preview(StatusPrototypeStateId.PLAYER_LEVEL_UP, StatusClassVariant.MONK)

@Preview(name = "6 · Rank promotion", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewRankPromotion() = preview(StatusPrototypeStateId.RANK_PROMOTION, StatusClassVariant.MAGE)

@Preview(name = "7 · Personal record", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewPersonalRecord() = preview(StatusPrototypeStateId.PERSONAL_RECORD, StatusClassVariant.BERSERKER)

@Preview(name = "8 · Progression recommendation", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewRecommendation() = preview(StatusPrototypeStateId.RECOMMENDATION, StatusClassVariant.BERSERKER)

@Preview(name = "9 · Reduced motion", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewReducedMotion() = preview(StatusPrototypeStateId.REDUCED_MOTION, StatusClassVariant.MONK)

@Preview(name = "10 · Text-stress scenario", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 1040)
@Composable
private fun PreviewTextStress() {
    AscendTheme(darkTheme = true) { StaticStatusPanel(FakeStatusPrototype.stressData(StatusClassVariant.BERSERKER, reducedMotion = false)) }
}

@Preview(name = "11 · Small phone", showBackground = true, backgroundColor = 0xFF06080D, widthDp = 340, heightDp = 960)
@Composable
private fun PreviewSmallPhone() = preview(StatusPrototypeStateId.STANDARD, StatusClassVariant.MONK)

@Preview(name = "12 · Normal phone", showBackground = true, backgroundColor = 0xFF06080D, widthDp = 400, heightDp = 960)
@Composable
private fun PreviewNormalPhone() = preview(StatusPrototypeStateId.STANDARD, StatusClassVariant.BERSERKER)

@Preview(name = "13 · Large phone", showBackground = true, backgroundColor = 0xFF06080D, widthDp = 520, heightDp = 960)
@Composable
private fun PreviewLargePhone() = preview(StatusPrototypeStateId.STANDARD, StatusClassVariant.MAGE)

@Preview(name = "14 · Secondary-class layout", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 960)
@Composable
private fun PreviewSecondaryClass() = preview(StatusPrototypeStateId.STANDARD, StatusClassVariant.BERSERKER, forceSecondary = true)

@Preview(name = "Font scale 1.5×", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 1040, fontScale = 1.5f)
@Composable
private fun PreviewLargeFontScale() = preview(StatusPrototypeStateId.RECOMMENDATION, StatusClassVariant.MONK, forceSecondary = true)

// ---- ornate sigil focus previews ----

@Preview(name = "Sigil · low rank", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewSigilLowRank() = preview(StatusPrototypeStateId.LOW_RANK, StatusClassVariant.NEUTRAL)

@Preview(name = "Sigil · mid rank", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewSigilMidRank() = preview(StatusPrototypeStateId.MID_RANK, StatusClassVariant.MONK)

@Preview(name = "Sigil · high rank dense", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewSigilHighRank() = preview(StatusPrototypeStateId.HIGH_RANK, StatusClassVariant.MAGE)

@Preview(name = "Sigil · attribute gain", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewSigilAttributeGain() = preview(StatusPrototypeStateId.ATTRIBUTE_UP, StatusClassVariant.BERSERKER)

@Preview(name = "Sigil · rank promotion", showBackground = true, backgroundColor = 0xFF06080D, heightDp = 940)
@Composable
private fun PreviewSigilRankPromotion() = preview(StatusPrototypeStateId.RANK_PROMOTION, StatusClassVariant.BERSERKER)
