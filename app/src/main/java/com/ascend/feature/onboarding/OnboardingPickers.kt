package com.ascend.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ascend.core.common.HeightUnits
import com.ascend.core.common.WeightUnit
import com.ascend.core.common.WeightUnits
import com.ascend.core.model.MeasurementSystem
import com.ascend.core.model.onboarding.ProfileMeasurementLimits

/*
 * Unit-aware height/weight picker fields for onboarding. Each field looks like the other inputs but,
 * when tapped, opens a structured picker: a scrollable bounded selector plus direct keyboard entry.
 * Values are held canonically (cm / kg) by the caller; these composables only convert at the display
 * and commit boundaries via [HeightUnits] / [WeightUnits], so a unit switch never rewrites the stored
 * value. Validation limits come from [ProfileMeasurementLimits] (configuration), never hardcoded here.
 */

private val PICKER_LIST_HEIGHT = 236.dp

/** A read-out field that opens a picker. Selection state and value are shown as text, never colour alone. */
@Composable
private fun PickerField(
    valueText: String?,
    placeholder: String,
    accessibility: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, OnboardingPalette.cardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .semantics { contentDescription = accessibility },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = valueText ?: placeholder,
            color = if (valueText != null) OnboardingPalette.textPrimary else OnboardingPalette.textMuted,
            fontSize = 15.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(if (valueText != null) "Change" else "Select", color = OnboardingPalette.accent, fontSize = 13.sp)
    }
}

/** The dialog chrome shared by both pickers: title, content, and a Clear / Cancel / Confirm bar. */
@Composable
private fun PickerDialog(
    title: String,
    error: String?,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(OnboardingPalette.card)
                .border(1.dp, OnboardingPalette.cardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(title, color = OnboardingPalette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            content()
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error, color = OnboardingPalette.danger, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClear) { Text("Clear", color = OnboardingPalette.textMuted) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCancel) { Text("Cancel", color = OnboardingPalette.textMuted) }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onConfirm) { Text("Confirm", color = OnboardingPalette.accent, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun darkFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = OnboardingPalette.textPrimary,
        unfocusedTextColor = OnboardingPalette.textPrimary,
        focusedBorderColor = OnboardingPalette.accent,
        unfocusedBorderColor = OnboardingPalette.cardBorder,
        focusedLabelColor = OnboardingPalette.accent,
        unfocusedLabelColor = OnboardingPalette.textMuted,
    )

/** One selectable numeric row. Selection is a check icon + fill, never colour alone. */
@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) OnboardingPalette.accentFill else OnboardingPalette.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = "$label, ${if (selected) "selected" else "not selected"}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(16.dp)) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = OnboardingPalette.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = OnboardingPalette.textPrimary, fontSize = 15.sp)
    }
}

/** A lazy, bounded, keyboard-searchable single-column value picker. */
@Composable
private fun ValueColumn(
    options: List<String>,
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    LaunchedEffect(options, selectedIndex) {
        if (selectedIndex >= 0) state.scrollToItem(maxOf(0, selectedIndex - 2))
    }
    LazyColumn(state = state, modifier = modifier.height(PICKER_LIST_HEIGHT), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        itemsIndexed(options) { index, label ->
            PickerRow(label = label, selected = index == selectedIndex, onClick = { onPick(index) })
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Weight
// -------------------------------------------------------------------------------------------------

@Composable
fun WeightPickerField(
    canonicalKg: Double?,
    unit: WeightUnit,
    onCommit: (Double?) -> Unit,
    limits: ProfileMeasurementLimits = ProfileMeasurementLimits.DEFAULT,
) {
    var open by remember { mutableStateOf(false) }
    PickerField(
        valueText = canonicalKg?.let { WeightUnits.format(it, unit) },
        placeholder = "Add weight (${unit.symbol})",
        accessibility = canonicalKg?.let { WeightUnits.accessibilityLabel(it, unit) } ?: "Weight, not set. Button.",
        onClick = { open = true },
    )
    if (open) {
        WeightPickerDialog(
            canonicalKg = canonicalKg,
            unit = unit,
            limits = limits,
            onDismiss = { open = false },
            onCommit = {
                onCommit(it)
                open = false
            },
        )
    }
}

@Composable
private fun WeightPickerDialog(
    canonicalKg: Double?,
    unit: WeightUnit,
    limits: ProfileMeasurementLimits,
    onDismiss: () -> Unit,
    onCommit: (Double?) -> Unit,
) {
    val optionsKg = remember(unit, limits) { limits.weightOptionsKg(unit) }
    val labels = remember(optionsKg, unit) { optionsKg.map { WeightUnits.formatValue(it, unit) } }
    // Free-form entry text; never rewritten while the user is typing an incomplete number.
    var entry by remember { mutableStateOf(canonicalKg?.let { WeightUnits.formatValue(it, unit) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    // As the user types, narrow the list; the selected row is the exact match if present.
    val filtered = if (entry.isBlank()) labels else labels.filter { it.startsWith(entry.trim()) }
    val selectedIndex = filtered.indexOf(entry.trim())

    PickerDialog(
        title = "Weight",
        error = error,
        onClear = { onCommit(null) },
        onCancel = onDismiss,
        onConfirm = {
            val display = entry.trim().toDoubleOrNull()
            if (display == null) {
                error = "Enter a weight in ${unit.displayName.lowercase()}."
            } else {
                val kg = WeightUnits.toCanonicalKg(display, unit)
                if (limits.isWeightValid(kg)) {
                    onCommit(kg)
                } else {
                    val lo = WeightUnits.formatValue(limits.weightKgMin, unit)
                    val hi = WeightUnits.formatValue(limits.weightKgMax, unit)
                    error = "Enter a weight between $lo and $hi ${unit.symbol}."
                }
            }
        },
    ) {
        OutlinedTextField(
            value = entry,
            onValueChange = {
                entry = it
                error = null
            },
            label = { Text("Weight (${unit.symbol})") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = darkFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("weightPickerEntry"),
        )
        Spacer(Modifier.height(12.dp))
        ValueColumn(
            options = filtered.map { "$it ${unit.symbol}" },
            selectedIndex = selectedIndex,
            onPick = { i ->
                entry = filtered[i]
                error = null
            },
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Height
// -------------------------------------------------------------------------------------------------

@Composable
fun HeightPickerField(
    canonicalCm: Double?,
    system: MeasurementSystem,
    onCommit: (Double?) -> Unit,
    limits: ProfileMeasurementLimits = ProfileMeasurementLimits.DEFAULT,
) {
    var open by remember { mutableStateOf(false) }
    PickerField(
        valueText = canonicalCm?.let { HeightUnits.format(it, system) },
        placeholder = if (system == MeasurementSystem.IMPERIAL) "Add height (ft / in)" else "Add height (cm)",
        accessibility = canonicalCm?.let { HeightUnits.accessibilityLabel(it, system) } ?: "Height, not set. Button.",
        onClick = { open = true },
    )
    if (open) {
        HeightPickerDialog(
            canonicalCm = canonicalCm,
            system = system,
            limits = limits,
            onDismiss = { open = false },
            onCommit = {
                onCommit(it)
                open = false
            },
        )
    }
}

@Composable
private fun HeightPickerDialog(
    canonicalCm: Double?,
    system: MeasurementSystem,
    limits: ProfileMeasurementLimits,
    onDismiss: () -> Unit,
    onCommit: (Double?) -> Unit,
) {
    if (system == MeasurementSystem.IMPERIAL) {
        ImperialHeightDialog(canonicalCm, limits, onDismiss, onCommit)
    } else {
        MetricHeightDialog(canonicalCm, limits, onDismiss, onCommit)
    }
}

@Composable
private fun ImperialHeightDialog(
    canonicalCm: Double?,
    limits: ProfileMeasurementLimits,
    onDismiss: () -> Unit,
    onCommit: (Double?) -> Unit,
) {
    val feetOptions = remember(limits) { limits.heightFeetOptions.toList() }
    val inchOptions = remember { (0..11).toList() }
    val initial = remember(canonicalCm) { canonicalCm?.let { HeightUnits.cmToFeetInches(it) } }
    var feet by remember { mutableStateOf(initial?.feet ?: feetOptions.getOrElse(feetOptions.size / 2) { 5 }) }
    var inches by remember { mutableStateOf(initial?.inches ?: 0) }
    var error by remember { mutableStateOf<String?>(null) }

    PickerDialog(
        title = "Height",
        error = error,
        onClear = { onCommit(null) },
        onCancel = onDismiss,
        onConfirm = {
            val cm = HeightUnits.feetInchesToCm(feet, inches)
            if (limits.isHeightValid(cm)) {
                onCommit(cm)
            } else {
                error = "Choose a height between ${limits.heightFeetOptions.first} and ${limits.heightFeetOptions.last} ft."
            }
        },
    ) {
        Text(
            "$feet ft $inches in",
            color = OnboardingPalette.accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clearAndSetSemantics { contentDescription = "Selected height, $feet feet $inches inches" },
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Feet", color = OnboardingPalette.textMuted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                ValueColumn(
                    options = feetOptions.map { it.toString() },
                    selectedIndex = feetOptions.indexOf(feet),
                    onPick = {
                        feet = feetOptions[it]
                        error = null
                    },
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Inches", color = OnboardingPalette.textMuted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                ValueColumn(
                    options = inchOptions.map { it.toString() },
                    selectedIndex = inchOptions.indexOf(inches),
                    onPick = {
                        inches = inchOptions[it]
                        error = null
                    },
                )
            }
        }
    }
}

@Composable
private fun MetricHeightDialog(
    canonicalCm: Double?,
    limits: ProfileMeasurementLimits,
    onDismiss: () -> Unit,
    onCommit: (Double?) -> Unit,
) {
    val options = remember(limits) { limits.heightCmOptions.toList() }
    val labels = remember(options) { options.map { it.toString() } }
    var entry by remember { mutableStateOf(canonicalCm?.let { HeightUnits.cmWhole(it).toString() } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val filtered = if (entry.isBlank()) labels else labels.filter { it.startsWith(entry.trim()) }
    val selectedIndex = filtered.indexOf(entry.trim())

    PickerDialog(
        title = "Height",
        error = error,
        onClear = { onCommit(null) },
        onCancel = onDismiss,
        onConfirm = {
            val cm = entry.trim().toDoubleOrNull()
            if (cm == null) {
                error = "Enter a height in centimetres."
            } else if (limits.isHeightValid(cm)) {
                onCommit(cm)
            } else {
                error = "Enter a height between ${limits.heightCmOptions.first} and ${limits.heightCmOptions.last} cm."
            }
        },
    ) {
        OutlinedTextField(
            value = entry,
            onValueChange = {
                entry = it
                error = null
            },
            label = { Text("Height (cm)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = darkFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("heightPickerEntry"),
        )
        Spacer(Modifier.height(12.dp))
        ValueColumn(
            options = filtered.map { "$it cm" },
            selectedIndex = selectedIndex,
            onPick = { i ->
                entry = filtered[i]
                error = null
            },
        )
    }
}
