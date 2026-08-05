package com.ascend.feature.onboarding

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.core.common.WeightUnit
import com.ascend.core.common.WeightUnits
import com.ascend.core.designsystem.component.WeightUnitSelector
import com.ascend.core.model.onboarding.ActivityDomain
import com.ascend.core.model.onboarding.ActivityPreference
import com.ascend.core.model.onboarding.AgeRange
import com.ascend.core.model.onboarding.ClassAffinityResult
import com.ascend.core.model.onboarding.Equipment
import com.ascend.core.model.onboarding.ExperienceLevel
import com.ascend.core.model.onboarding.InitialQuestPlan
import com.ascend.core.model.onboarding.Limitation
import com.ascend.core.model.onboarding.OptionalSex
import com.ascend.core.model.onboarding.PrimaryGoal
import com.ascend.core.model.onboarding.SessionDuration
import com.ascend.core.model.onboarding.TrainingDaysPerWeek
import com.ascend.core.model.onboarding.TrainingEnvironment
import com.ascend.core.model.onboarding.TrainingFrequency

/** Humanize an enum constant name, e.g. RETURNING_FROM_INJURY -> "Returning from injury". */
internal fun pretty(name: String): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private val SAFETY_NOTE =
    "Stop exercising and seek appropriate help if you experience chest pain, faintness, severe " +
        "shortness of breath, sharp pain, or signs of injury."

@Composable
private fun DarkNumberField(
    label: String,
    initial: String,
    onValue: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValue(it)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnboardingPalette.textPrimary,
                unfocusedTextColor = OnboardingPalette.textPrimary,
                focusedBorderColor = OnboardingPalette.accent,
                unfocusedBorderColor = OnboardingPalette.cardBorder,
                focusedLabelColor = OnboardingPalette.accent,
                unfocusedLabelColor = OnboardingPalette.textMuted,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun BasicProfileStep(
    draft: OnboardingDraft,
    onDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onAge: (AgeRange) -> Unit,
) {
    FieldBlock("Display name") {
        var name by remember { mutableStateOf(draft.displayName) }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onDraft { d -> d.copy(displayName = it) }
            },
            label = { Text("What should we call you?") },
            singleLine = true,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnboardingPalette.textPrimary,
                    unfocusedTextColor = OnboardingPalette.textPrimary,
                    focusedBorderColor = OnboardingPalette.accent,
                    unfocusedBorderColor = OnboardingPalette.cardBorder,
                    focusedLabelColor = OnboardingPalette.accent,
                    unfocusedLabelColor = OnboardingPalette.textMuted,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    FieldBlock("Age range") {
        OnboardingHint("We store only a general age band — never your birthday.")
        SingleChoice(AgeRange.entries.toList(), draft.ageRange, ::ageLabel) { onAge(it) }
    }
    FieldBlock("Weight unit") {
        WeightUnitSelector(selected = draft.weightUnit, onSelect = { onDraft { d -> d.copy(weightUnit = it) } })
    }
    FieldBlock("Height & weight (optional)") {
        OnboardingHint("Helps personalize recommendations. Stored privately; you can add these later.")
        DarkNumberField("Height (cm)", draft.heightCm?.let { it.toInt().toString() } ?: "") { s ->
            onDraft { d -> d.copy(heightCm = s.toDoubleOrNull()) }
        }
        Spacer(Modifier.height(12.dp))
        val unit = draft.weightUnit
        DarkNumberField("Current weight (${unit.symbol})", weightText(draft.currentWeightKg, unit)) { s ->
            onDraft { d -> d.copy(currentWeightKg = s.toDoubleOrNull()?.let { WeightUnits.toCanonicalKg(it, unit) }) }
        }
        Spacer(Modifier.height(12.dp))
        DarkNumberField("Goal weight (${unit.symbol}, optional)", weightText(draft.goalWeightKg, unit)) { s ->
            onDraft { d -> d.copy(goalWeightKg = s.toDoubleOrNull()?.let { WeightUnits.toCanonicalKg(it, unit) }) }
        }
    }
}

private fun weightText(
    canonicalKg: Double?,
    unit: WeightUnit,
): String = canonicalKg?.let { WeightUnits.formatValue(it, unit) } ?: ""

private fun ageLabel(range: AgeRange): String =
    when (range) {
        AgeRange.UNDER_13 -> "Under 13"
        AgeRange.AGE_13_15 -> "13–15"
        AgeRange.AGE_16_17 -> "16–17"
        AgeRange.AGE_18_24 -> "18–24"
        AgeRange.AGE_25_34 -> "25–34"
        AgeRange.AGE_35_49 -> "35–49"
        AgeRange.AGE_50_PLUS -> "50+"
        AgeRange.PREFER_NOT_TO_SAY -> "Prefer not to say"
    }

@Composable
fun GoalsStep(
    draft: OnboardingDraft,
    onDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    FieldBlock("Primary goal") {
        SingleChoice(PrimaryGoal.entries.toList(), draft.primaryGoal, { it.displayName }) { goal ->
            onDraft { d -> d.copy(primaryGoal = goal, secondaryGoals = d.secondaryGoals - goal) }
        }
    }
    FieldBlock("Secondary goals (optional)") {
        MultiChoice(
            PrimaryGoal.entries.filter { it != draft.primaryGoal },
            draft.secondaryGoals,
            { it.displayName },
        ) { goal -> onDraft { d -> d.copy(secondaryGoals = d.secondaryGoals.toggle(goal)) } }
    }
}

@Composable
fun TrainingBackgroundStep(
    draft: OnboardingDraft,
    onDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    FieldBlock("How consistently have you trained in the last three months?") {
        SingleChoice(TrainingFrequency.entries.toList(), draft.trainingFrequency, { pretty(it.name) }) { freq ->
            onDraft { d -> d.copy(trainingFrequency = freq) }
        }
    }
    FieldBlock("Experience (optional)") {
        OnboardingHint("No experience never limits any class, Skill, or rank — it only tunes your start.")
        ActivityDomain.entries.forEach { domain ->
            Text(pretty(domain.name), color = OnboardingPalette.textMuted, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            SingleChoice(ExperienceLevel.entries.toList(), draft.experience[domain], { pretty(it.name) }) { level ->
                onDraft { d -> d.copy(experience = d.experience + (domain to level)) }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
fun ActivityPreferencesStep(
    draft: OnboardingDraft,
    onDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    FieldBlock("What do you enjoy or want to explore?") {
        MultiChoice(ActivityPreference.entries.toList(), draft.preferences, { pretty(it.name) }) { pref ->
            onDraft { d -> d.copy(preferences = d.preferences.toggle(pref)) }
        }
    }
}

@Composable
fun EquipmentEnvironmentStep(
    draft: OnboardingDraft,
    onDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    FieldBlock("Where will you train?") {
        SingleChoice(TrainingEnvironment.entries.toList(), draft.environment, { pretty(it.name) }) { env ->
            onDraft { d -> d.copy(environment = env) }
        }
    }
    FieldBlock("Available equipment") {
        OnboardingHint("Your plan will only suggest activities you can actually do.")
        MultiChoice(Equipment.entries.toList(), draft.equipment, { pretty(it.name) }) { eq ->
            onDraft { d -> d.copy(equipment = d.equipment.toggle(eq)) }
        }
    }
}

@Composable
fun AvailabilityStep(
    draft: OnboardingDraft,
    onDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    FieldBlock("Training days per week") {
        SingleChoice(TrainingDaysPerWeek.entries.toList(), draft.trainingDays, { it.count?.toString() ?: "Flexible" }) { days ->
            onDraft { d -> d.copy(trainingDays = days) }
        }
    }
    FieldBlock("Typical session length") {
        SingleChoice(SessionDuration.entries.toList(), draft.sessionDuration, ::durationLabel) { dur ->
            onDraft { d -> d.copy(sessionDuration = dur) }
        }
    }
}

private fun durationLabel(duration: SessionDuration): String =
    when (duration) {
        SessionDuration.UNDER_20 -> "Under 20 min"
        SessionDuration.MIN_20_30 -> "20–30 min"
        SessionDuration.MIN_30_45 -> "30–45 min"
        SessionDuration.MIN_45_60 -> "45–60 min"
        SessionDuration.OVER_60 -> "60+ min"
    }

@Composable
fun AbilitySnapshotStep(
    draft: OnboardingDraft,
    onDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    OnboardingHint(
        "Optional and comfortable only — never a max test. These set provisional starting points and " +
            "are replaced by your real workouts over time.",
    )
    FieldBlock("Comfortable push-ups in a set") {
        DarkNumberField("Reps (optional)", draft.ability.comfortablePushUps?.toString() ?: "") { s ->
            onDraft { d -> d.copy(ability = d.ability.copy(comfortablePushUps = s.toIntOrNull())) }
        }
    }
    FieldBlock("Comfortable bodyweight squats in a set") {
        DarkNumberField("Reps (optional)", draft.ability.comfortableSquats?.toString() ?: "") { s ->
            onDraft { d -> d.copy(ability = d.ability.copy(comfortableSquats = s.toIntOrNull())) }
        }
    }
    FieldBlock("Longest recent comfortable cardio") {
        DarkNumberField("Minutes (optional)", draft.ability.longestRecentCardioMinutes?.toString() ?: "") { s ->
            onDraft { d -> d.copy(ability = d.ability.copy(longestRecentCardioMinutes = s.toIntOrNull())) }
        }
    }
}

@Composable
fun PhysiologyLimitationsStep(
    draft: OnboardingDraft,
    onDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
) {
    FieldBlock("Physiology (optional)") {
        OnboardingHint(
            "This optional information may improve certain energy-use or body-composition estimates. It " +
                "does not affect which classes, Skills, exercises, or ranks you can access.",
        )
        SingleChoice(OptionalSex.entries.toList(), draft.sex.takeIf { it != OptionalSex.NOT_SET }, { pretty(it.name) }) { sex ->
            onDraft { d -> d.copy(sex = sex) }
        }
    }
    FieldBlock("Anything Ascend should consider when suggesting exercises?") {
        OnboardingHint("These help us avoid unsuitable suggestions and offer substitutions. They are private.")
        MultiChoice(Limitation.entries.filter { it != Limitation.NONE_REPORTED }, draft.limitations, { pretty(it.name) }) { lim ->
            onDraft { d -> d.copy(limitations = d.limitations.toggle(lim)) }
        }
    }
    FieldBlock("Safety") {
        Text(SAFETY_NOTE, color = OnboardingPalette.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { contentDescription = "I understand the safety note" },
        ) {
            Switch(checked = draft.safetyAcknowledged, onCheckedChange = { onDraft { d -> d.copy(safetyAcknowledged = it) } })
            Spacer(Modifier.height(0.dp))
            Text("  I understand", color = OnboardingPalette.textPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
fun ClassAffinityStep(
    draft: OnboardingDraft,
    affinity: ClassAffinityResult?,
    onSelectClass: (String) -> Unit,
) {
    if (affinity?.recommendedClassId != null) {
        Text(
            "Suggested affinity: ${pretty(affinity.recommendedClassId)}",
            color = OnboardingPalette.accent,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(affinity.rationale, color = OnboardingPalette.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
    }
    FieldBlock("Choose your starting class") {
        OnboardingHint("Any class is available regardless of the suggestion — you can change later.")
        val classes = affinity?.classScores?.keys?.toList() ?: listOf("berserker", "monk", "magician")
        SingleChoice(classes, draft.selectedClassId, ::pretty) { onSelectClass(it) }
    }
}

@Composable
fun PlanReviewStep(
    draft: OnboardingDraft,
    plan: InitialQuestPlan?,
) {
    ReviewLine("Name", draft.displayName.ifBlank { "Hunter" })
    ReviewLine("Class", draft.selectedClassId?.let { pretty(it) } ?: "None")
    ReviewLine("Primary goal", draft.primaryGoal?.displayName ?: "—")
    ReviewLine("Environment", draft.environment?.let { pretty(it.name) } ?: "—")
    ReviewLine(
        "Schedule",
        "${draft.trainingDays?.count?.toString() ?: "Flexible"} days · ${draft.sessionDuration?.let { durationLabel(it) } ?: "—"}",
    )
    Spacer(Modifier.height(16.dp))
    Text("Starting quests", color = OnboardingPalette.textPrimary, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp))
    if (plan == null || plan.questDefinitions.isEmpty()) {
        Text("We'll suggest starter quests once your details are in.", color = OnboardingPalette.textMuted, fontSize = 13.sp)
    } else {
        Text("${pretty(plan.difficultyBand.name)} intensity · provisional", color = OnboardingPalette.textMuted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        plan.questDefinitions.forEach { def ->
            Text(
                "• ${def.name}: ${def.target} ${def.unit}",
                color = OnboardingPalette.textPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
        if (plan.assessmentSuggestions.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Optional assessment quests", color = OnboardingPalette.textPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            plan.assessmentSuggestions.forEach { s ->
                Text("• ${s.title}", color = OnboardingPalette.textMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun ReviewLine(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = OnboardingPalette.textMuted, fontSize = 13.sp, modifier = Modifier.padding(end = 12.dp))
        Text(value, color = OnboardingPalette.textPrimary, fontSize = 14.sp)
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
