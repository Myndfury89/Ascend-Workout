package com.ascend.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ascend.core.domain.classes.ClassAffinityVocabulary
import com.ascend.core.domain.classes.PreviewClass
import com.ascend.core.domain.classes.PreviewClassCatalog
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.onboarding.ClassAffinityResult

/*
 * The class-selection step. Every implemented class is a rich, selectable card (affinity summary,
 * training styles, unique proficiency, strongest attributes, a match indicator, and a full-details
 * sheet). The recommended card is marked and shows the "why" derived from the player's own answers.
 * Planned-but-unbuilt classes appear in a clearly-unavailable "Paths Yet to Awaken" preview that can
 * never be saved as a selection. Nothing here branches on a class id — cards render from definition
 * data, so a new seeded class needs no UI change.
 */

@Composable
fun ClassAffinityStep(
    draft: OnboardingDraft,
    affinity: ClassAffinityResult?,
    definitions: List<ClassDefinition>,
    onSelectClass: (String) -> Unit,
) {
    OnboardingHint(
        "Your class shapes training priorities, Class XP, and a unique proficiency — it never locks " +
            "exercises, attributes, Skills, or rank. Any class is available regardless of the suggestion, " +
            "and you can change it later.",
    )

    val maxScore = affinity?.classScores?.values?.maxOrNull() ?: 0.0
    val ordered = orderedForDisplay(definitions, affinity)

    var detailsFor by remember { mutableStateOf<ClassDefinition?>(null) }
    var previewFor by remember { mutableStateOf<PreviewClass?>(null) }

    ordered.forEach { def ->
        ClassCard(
            def = def,
            recommended = def.id == affinity?.recommendedClassId,
            selected = def.id == draft.selectedClassId,
            score = affinity?.classScores?.get(def.id) ?: 0.0,
            maxScore = maxScore,
            rationale = if (def.id == affinity?.recommendedClassId) affinity.rationale else null,
            onSelect = { onSelectClass(def.id) },
            onDetails = { detailsFor = def },
        )
        Spacer(Modifier.height(12.dp))
    }

    if (PreviewClassCatalog.ALL.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Paths Yet to Awaken", color = OnboardingPalette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("In development — previewed here, not yet selectable.", color = OnboardingPalette.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        PreviewClassCatalog.ALL.forEach { preview ->
            PreviewClassCard(preview = preview, onDetails = { previewFor = preview })
            Spacer(Modifier.height(12.dp))
        }
    }

    detailsFor?.let { ClassDetailsDialog(def = it, onDismiss = { detailsFor = null }) }
    previewFor?.let { PreviewDetailsDialog(preview = it, onDismiss = { previewFor = null }) }
}

/** Recommended class first, then the rest by descending match score (stable) — no id conditionals. */
private fun orderedForDisplay(
    definitions: List<ClassDefinition>,
    affinity: ClassAffinityResult?,
): List<ClassDefinition> {
    if (affinity == null) return definitions
    return definitions.sortedWith(
        compareByDescending<ClassDefinition> { it.id == affinity.recommendedClassId }
            .thenByDescending { affinity.classScores[it.id] ?: 0.0 },
    )
}

private fun strongestAttributes(def: ClassDefinition): String =
    (def.primaryAttributes + def.secondaryAttributes).distinct().joinToString(" • ") { it.displayName }

private fun matchLabel(
    score: Double,
    maxScore: Double,
): String =
    when {
        maxScore <= 0.0 || score <= 0.0 -> "Even match"
        score >= maxScore -> "Strong match"
        score >= maxScore / 2 -> "Moderate match"
        else -> "Light match"
    }

@Composable
private fun ClassCard(
    def: ClassDefinition,
    recommended: Boolean,
    selected: Boolean,
    score: Double,
    maxScore: Double,
    rationale: String?,
    onSelect: () -> Unit,
    onDetails: () -> Unit,
) {
    val border = if (selected) OnboardingPalette.accent else OnboardingPalette.cardBorder
    val selectionState = if (selected) "selected" else "not selected"
    val recommendedLabel = if (recommended) "Recommended. " else ""
    val cardDescription = "${def.name} class, $selectionState. $recommendedLabel${def.classTitle}"
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OnboardingPalette.card)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(16.dp)
            .semantics { contentDescription = cardDescription },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = OnboardingPalette.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(def.name, color = OnboardingPalette.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (recommended) Badge("Recommended")
        }
        Spacer(Modifier.height(4.dp))
        Text(def.classTitle, color = OnboardingPalette.textMuted, fontSize = 12.sp)

        Spacer(Modifier.height(10.dp))
        MatchIndicator(score = score, maxScore = maxScore)

        Spacer(Modifier.height(10.dp))
        Text(ClassAffinityVocabulary.summary(def), color = OnboardingPalette.textPrimary, fontSize = 14.sp)

        Spacer(Modifier.height(10.dp))
        LabelledLine("Training styles", def.favoredWorkoutCategories.take(3).joinToString(" • "))
        LabelledLine("Strongest attributes", strongestAttributes(def))
        LabelledLine("Unique proficiency", def.uniqueProficiencyName, valueAccent = true)

        if (rationale != null) {
            Spacer(Modifier.height(10.dp))
            Text("Why this was recommended", color = OnboardingPalette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(rationale, color = OnboardingPalette.textMuted, fontSize = 13.sp, fontStyle = FontStyle.Italic)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "You can still train cardio, mobility, and techniques from other paths.",
            color = OnboardingPalette.textMuted,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDetails) { Text("Details", color = OnboardingPalette.accent, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            Text(
                if (selected) "Selected" else "Tap to choose",
                color = if (selected) OnboardingPalette.accent else OnboardingPalette.textMuted,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun PreviewClassCard(
    preview: PreviewClass,
    onDetails: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OnboardingPalette.ground)
            .border(1.dp, OnboardingPalette.cardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
            .semantics { contentDescription = "${preview.name}, coming later, not selectable" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(preview.name, color = OnboardingPalette.textMuted, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Badge("Coming later", muted = true)
        }
        Spacer(Modifier.height(8.dp))
        Text(preview.affinitySummary, color = OnboardingPalette.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        LabelledLine("Typical styles", preview.primaryStyles.take(3).joinToString(" • "), muted = true)
        LabelledLine("Suggested proficiency", preview.suggestedProficiencyName, muted = true)
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onDetails) { Text("Details", color = OnboardingPalette.textMuted, fontSize = 13.sp) }
    }
}

@Composable
private fun Badge(
    text: String,
    muted: Boolean = false,
) {
    val fg = if (muted) OnboardingPalette.textMuted else OnboardingPalette.accent
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(OnboardingPalette.accentFill)
            .border(1.dp, fg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MatchIndicator(
    score: Double,
    maxScore: Double,
) {
    val fraction = if (maxScore > 0.0) (score / maxScore).toFloat().coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(OnboardingPalette.accentFill),
        ) {
            // A minimum visible sliver so "even/light" is still legible without relying on colour.
            Box(
                Modifier
                    .fillMaxWidth(if (fraction <= 0f) 0.06f else fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(OnboardingPalette.accent),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(matchLabel(score, maxScore), color = OnboardingPalette.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun LabelledLine(
    label: String,
    value: String,
    valueAccent: Boolean = false,
    muted: Boolean = false,
) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", color = OnboardingPalette.textMuted, fontSize = 13.sp)
        Text(
            value,
            color =
                when {
                    valueAccent -> OnboardingPalette.accent
                    muted -> OnboardingPalette.textMuted
                    else -> OnboardingPalette.textPrimary
                },
            fontSize = 13.sp,
            fontWeight = if (valueAccent) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun ClassDetailsDialog(
    def: ClassDefinition,
    onDismiss: () -> Unit,
) {
    DetailsScaffold(title = def.name, subtitle = def.classTitle, onDismiss = onDismiss) {
        DetailParagraph(def.description)
        Spacer(Modifier.height(12.dp))
        DetailSection("Fitness identity", def.fitnessIdentity)
        DetailSection("Affinity", ClassAffinityVocabulary.summary(def))
        DetailSection("Training styles", def.favoredWorkoutCategories.joinToString(" • "))
        DetailSection("Strongest attributes", strongestAttributes(def))
        DetailSection("Unique proficiency", def.uniqueProficiencyName)
        Spacer(Modifier.height(12.dp))
        DetailParagraph(
            "Choosing ${def.name} emphasises these priorities and grows its unique proficiency faster — " +
                "it does not restrict any exercise, universal attribute, transferable Skill, workout type, or rank. " +
                "You can train and progress anything from any path.",
        )
    }
}

@Composable
private fun PreviewDetailsDialog(
    preview: PreviewClass,
    onDismiss: () -> Unit,
) {
    DetailsScaffold(title = preview.name, subtitle = "Coming later — not yet selectable", onDismiss = onDismiss) {
        DetailSection("Direction", preview.affinitySummary)
        DetailSection("Typical styles", preview.primaryStyles.joinToString(" • "))
        DetailSection("Suggested proficiency", preview.suggestedProficiencyName)
        Spacer(Modifier.height(12.dp))
        DetailParagraph(
            "This path is still being designed. Its full progression rules aren't finalised, so it can't be " +
                "selected yet. When it's ready it will appear as a full, selectable class.",
        )
    }
}

@Composable
private fun DetailsScaffold(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(OnboardingPalette.card)
                .border(1.dp, OnboardingPalette.cardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(title, color = OnboardingPalette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = OnboardingPalette.textMuted, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .heightForDetails()
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Close", color = OnboardingPalette.accent, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

private fun Modifier.heightForDetails(): Modifier = this.height(360.dp)

@Composable
private fun DetailSection(
    label: String,
    value: String,
) {
    if (value.isBlank()) return
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(label.uppercase(), color = OnboardingPalette.textMuted, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = OnboardingPalette.textPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun DetailParagraph(text: String) {
    Text(text, color = OnboardingPalette.textMuted, fontSize = 13.sp)
}
