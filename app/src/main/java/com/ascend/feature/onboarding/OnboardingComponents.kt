package com.ascend.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Onboarding palette — related to Status (near-black blue-violet) but lighter and calmer. */
object OnboardingPalette {
    val ground = Color(0xFF06070E)
    val card = Color(0xFF0E1220)
    val cardBorder = Color(0xFF232B47)
    val accent = Color(0xFF8C9BFF)
    val accentFill = Color(0xFF20264A)
    val textPrimary = Color(0xFFEAF0FF)
    val textMuted = Color(0xFF8A93B5)
    val danger = Color(0xFFE8735A)
}

/**
 * The shared step frame: a subtle "Step N of M" progress row, a title/subtitle, the scrollable step
 * content, an optional validation message, and a Back / (Skip) / Next control bar. Portrait-first;
 * no ambient animation, so it is unaffected by reduced motion.
 */
@Composable
fun OnboardingScaffold(
    title: String,
    subtitle: String?,
    progressIndex: Int,
    progressTotal: Int,
    onBack: (() -> Unit)?,
    onNext: (() -> Unit)?,
    nextLabel: String = "Next",
    onSkip: (() -> Unit)? = null,
    validationError: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(OnboardingPalette.ground)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 16.dp),
    ) {
        if (progressTotal > 0 && progressIndex in 1..progressTotal) {
            val label = "Step $progressIndex of $progressTotal"
            Text(
                label.uppercase(),
                color = OnboardingPalette.textMuted,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.semantics { contentDescription = label },
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progressIndex.toFloat() / progressTotal },
                color = OnboardingPalette.accent,
                trackColor = OnboardingPalette.accentFill,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).clearAndSetSemantics {},
            )
            Spacer(Modifier.height(20.dp))
        }
        Text(title, color = OnboardingPalette.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = OnboardingPalette.textMuted, fontSize = 14.sp)
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            content()
        }
        if (validationError != null) {
            Spacer(Modifier.height(8.dp))
            Text(validationError, color = OnboardingPalette.danger, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
            Spacer(Modifier.weight(1f))
            if (onSkip != null) {
                TextButton(onClick = onSkip) { Text("Skip", color = OnboardingPalette.textMuted) }
                Spacer(Modifier.size(8.dp))
            }
            if (onNext != null) {
                Button(
                    onClick = onNext,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = OnboardingPalette.accent,
                            contentColor = OnboardingPalette.ground,
                        ),
                ) { Text(nextLabel, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** A section hint / helper line. */
@Composable
fun OnboardingHint(text: String) {
    Text(text, color = OnboardingPalette.textMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
}

/**
 * A single selectable pill. Selection is shown by a check icon + filled border (never colour alone),
 * and exposed to accessibility as "<label>, selected/not selected".
 */
@Composable
fun SelectPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) OnboardingPalette.accent else OnboardingPalette.cardBorder
    val fill = if (selected) OnboardingPalette.accentFill else OnboardingPalette.card
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "$label, ${if (selected) "selected" else "not selected"}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = OnboardingPalette.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
        }
        Text(label, color = OnboardingPalette.textPrimary, fontSize = 14.sp)
    }
}

/** Single-choice pill group. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SingleChoice(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            SelectPill(label(option), option == selected, { onSelect(option) })
        }
    }
}

/** Multi-choice pill group (toggles membership). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MultiChoice(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            SelectPill(label(option), option in selected, { onToggle(option) })
        }
    }
}

/** A titled block wrapper used inside steps. */
@Composable
fun FieldBlock(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(label, color = OnboardingPalette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** Centered message used by the Welcome / Completion / eligibility states. */
@Composable
fun CenteredMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(OnboardingPalette.ground).padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                color = OnboardingPalette.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(body, color = OnboardingPalette.textMuted, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            content()
        }
    }
}
