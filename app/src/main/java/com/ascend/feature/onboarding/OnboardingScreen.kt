package com.ascend.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.core.model.onboarding.OnboardingStep

/**
 * The onboarding stepper. A below-minimum-age selection routes to a neutral eligibility screen (no
 * profile is created); otherwise the flow walks the steps, generating the class affinity and
 * provisional plan at the right points, and completes into the app-entry gate which routes to the
 * existing production Status. Reduced motion is honored from the first screen (there is no ambient
 * animation here regardless).
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.loading) {
        Box(Modifier.fillMaxSize().background(OnboardingPalette.ground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OnboardingPalette.accent)
        }
        return
    }
    if (state.ageIneligible) {
        AgeIneligibleScreen()
        return
    }

    when (state.currentStep) {
        OnboardingStep.WELCOME ->
            WelcomeScreen(
                reducedMotion = state.reducedMotion,
                onReducedMotionChange = viewModel::setReducedMotion,
                onBegin = { viewModel.next() },
            )
        OnboardingStep.COMPLETION ->
            CenteredMessage(
                title = "Your Ascension begins",
                body = "Your profile is ready. Your provisional plan will sharpen as you log real workouts.",
            )
        else -> StepScaffold(state, viewModel)
    }
}

@Composable
private fun StepScaffold(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
) {
    val step = state.currentStep
    val skippable = step == OnboardingStep.ACTIVITY_PREFERENCES || step == OnboardingStep.ABILITY_SNAPSHOT
    val isReview = step == OnboardingStep.PLAN_REVIEW
    OnboardingScaffold(
        title = titleFor(step),
        subtitle = subtitleFor(step),
        progressIndex = state.progressIndex,
        progressTotal = state.progressTotal,
        onBack = { viewModel.back() },
        onNext = { if (isReview) viewModel.complete() else viewModel.next() },
        nextLabel = if (isReview) "Begin Ascending" else "Next",
        onSkip =
            if (skippable) {
                { viewModel.next(skip = true) }
            } else {
                null
            },
        validationError = state.validationError,
    ) {
        val draft = state.draft
        when (step) {
            OnboardingStep.BASIC_PROFILE -> BasicProfileStep(draft, viewModel::updateDraft, viewModel::selectAgeRange)
            OnboardingStep.GOALS -> GoalsStep(draft, viewModel::updateDraft)
            OnboardingStep.TRAINING_BACKGROUND -> TrainingBackgroundStep(draft, viewModel::updateDraft)
            OnboardingStep.ACTIVITY_PREFERENCES -> ActivityPreferencesStep(draft, viewModel::updateDraft)
            OnboardingStep.EQUIPMENT_ENVIRONMENT -> EquipmentEnvironmentStep(draft, viewModel::updateDraft)
            OnboardingStep.AVAILABILITY -> AvailabilityStep(draft, viewModel::updateDraft)
            OnboardingStep.ABILITY_SNAPSHOT -> AbilitySnapshotStep(draft, viewModel::updateDraft)
            OnboardingStep.PHYSIOLOGY_LIMITATIONS -> PhysiologyLimitationsStep(draft, viewModel::updateDraft)
            OnboardingStep.CLASS_AFFINITY ->
                ClassAffinityStep(draft, state.affinity) { id -> viewModel.updateDraft { it.copy(selectedClassId = id) } }
            OnboardingStep.PLAN_REVIEW -> PlanReviewStep(draft, state.plan)
            else -> Unit
        }
    }
}

@Composable
private fun WelcomeScreen(
    reducedMotion: Boolean,
    onReducedMotionChange: (Boolean) -> Unit,
    onBegin: () -> Unit,
) {
    CenteredMessage(
        title = "Your Ascension Begins",
        body =
            "Ascend turns your real training into an original dark-fantasy rise. As you go, it adapts your " +
                "Daily Quests, workout recommendations, class progression, Skills, recovery guidance, and " +
                "milestone tracking. This short assessment sets a safe, personalized start.",
    ) {
        Button(
            onClick = onBegin,
            colors = ButtonDefaults.buttonColors(containerColor = OnboardingPalette.accent, contentColor = OnboardingPalette.ground),
        ) { Text("Begin Assessment", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = reducedMotion, onCheckedChange = onReducedMotionChange)
            Text("  Reduced motion", color = OnboardingPalette.textMuted, fontSize = 14.sp)
        }
    }
}

/** Neutral, non-shaming age-eligibility screen. No profile, class, schedule, or presence is created. */
@Composable
fun AgeIneligibleScreen() {
    CenteredMessage(
        title = "Not available yet",
        body =
            "Ascend isn't available for your age group right now. Nothing has been saved. We hope to " +
                "welcome you in the future.",
    )
}

private fun titleFor(step: OnboardingStep): String =
    when (step) {
        OnboardingStep.BASIC_PROFILE -> "Your basics"
        OnboardingStep.GOALS -> "Your goals"
        OnboardingStep.TRAINING_BACKGROUND -> "Training background"
        OnboardingStep.ACTIVITY_PREFERENCES -> "What you enjoy"
        OnboardingStep.EQUIPMENT_ENVIRONMENT -> "Equipment & environment"
        OnboardingStep.AVAILABILITY -> "Your availability"
        OnboardingStep.ABILITY_SNAPSHOT -> "Ability snapshot"
        OnboardingStep.PHYSIOLOGY_LIMITATIONS -> "Safety & physiology"
        OnboardingStep.CLASS_AFFINITY -> "Your class"
        OnboardingStep.PLAN_REVIEW -> "Review your plan"
        else -> ""
    }

private fun subtitleFor(step: OnboardingStep): String? =
    when (step) {
        OnboardingStep.ABILITY_SNAPSHOT -> "Optional — comfortable recent numbers only."
        OnboardingStep.ACTIVITY_PREFERENCES -> "Optional — pick anything that appeals to you."
        OnboardingStep.PLAN_REVIEW -> "You can edit anything later; this plan is provisional."
        else -> null
    }
