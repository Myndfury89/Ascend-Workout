package com.ascend.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.domain.onboarding.AgeSafetyClassifier
import com.ascend.core.domain.onboarding.AssessmentQuestSuggester
import com.ascend.core.domain.onboarding.ClassAffinityAssessor
import com.ascend.core.domain.onboarding.InitialQuestPlanGenerator
import com.ascend.core.domain.onboarding.QuestActivityProfileCatalog
import com.ascend.core.domain.onboarding.QuestPlanCandidate
import com.ascend.core.domain.repository.ClassRepository
import com.ascend.core.domain.repository.OnboardingRepository
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.domain.repository.QuestTemplateRepository
import com.ascend.core.model.ClassChangeSource
import com.ascend.core.model.onboarding.AgeRange
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.ONBOARDING_VERSION
import com.ascend.core.model.onboarding.OnboardingState
import com.ascend.core.model.onboarding.OnboardingStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the resumable onboarding stepper. Persists progress + provisional answers after each step
 * so an interruption resumes in place, gates below-minimum-age users out of profile creation, and on
 * completion writes the profile, provisional assessment, class selection, affinity, and plan — then
 * routes to the existing production Status via the app-entry gate. Awards nothing.
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val onboardingRepository: OnboardingRepository,
        private val playerRepository: PlayerRepository,
        private val classRepository: ClassRepository,
        private val questTemplateRepository: QuestTemplateRepository,
        private val ageClassifier: AgeSafetyClassifier,
        private val affinityAssessor: ClassAffinityAssessor,
        private val planGenerator: InitialQuestPlanGenerator,
        private val assessmentSuggester: AssessmentQuestSuggester,
    ) : ViewModel() {
        private val userId = LOCAL_USER_ID
        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch { classRepository.seedDefinitions() }
            viewModelScope.launch { questTemplateRepository.seed() }
            viewModelScope.launch {
                val saved = onboardingRepository.getState(userId)
                val assessment = onboardingRepository.getAssessment(userId)
                val draft = assessment?.let { it.toDraft() } ?: OnboardingDraft()
                val category = assessment?.ageSafetyCategory ?: AgeSafetyCategory.NOT_PROVIDED
                _uiState.update {
                    it.copy(
                        loading = false,
                        currentStep = saved?.currentStep ?: OnboardingStep.WELCOME,
                        draft = draft,
                        ageSafetyCategory = category,
                    )
                }
            }
        }

        fun updateDraft(transform: (OnboardingDraft) -> OnboardingDraft) {
            _uiState.update { it.copy(draft = transform(it.draft), validationError = null) }
        }

        fun setReducedMotion(value: Boolean) = _uiState.update { it.copy(reducedMotion = value) }

        /** Selecting an age range immediately classifies it; below-minimum routes to eligibility. */
        fun selectAgeRange(range: AgeRange) {
            val category = ageClassifier.classify(range)
            _uiState.update {
                it.copy(
                    draft = it.draft.copy(ageRange = range),
                    ageSafetyCategory = category,
                    ageIneligible = !ageClassifier.canOnboard(category),
                    validationError = null,
                )
            }
        }

        fun back() {
            val current = _uiState.value.currentStep
            val previous = OnboardingStep.entries.getOrNull(current.ordinal - 1) ?: return
            _uiState.update { it.copy(currentStep = previous, validationError = null) }
        }

        /** Advance if the current step validates; optionally record it as skipped. */
        fun next(skip: Boolean = false) {
            val state = _uiState.value
            val error = if (skip) null else validate(state)
            if (error != null) {
                _uiState.update { it.copy(validationError = error) }
                return
            }
            val current = state.currentStep
            val nextStep = OnboardingStep.entries.getOrNull(current.ordinal + 1) ?: return
            viewModelScope.launch { advanceTo(current, nextStep, skip) }
        }

        private suspend fun advanceTo(
            from: OnboardingStep,
            to: OnboardingStep,
            skipped: Boolean,
        ) {
            // Persist progress + provisional answers so an interruption resumes in place. Never for a
            // below-minimum-age user (no persisted profile/assessment is created for them).
            if (!_uiState.value.ageIneligible) {
                persistProgress(from, to, skipped)
            }
            when (to) {
                OnboardingStep.CLASS_AFFINITY -> computeAffinity()
                OnboardingStep.PLAN_REVIEW -> generatePlan()
                else -> Unit
            }
            _uiState.update { it.copy(currentStep = to, validationError = null) }
        }

        private suspend fun persistProgress(
            from: OnboardingStep,
            to: OnboardingStep,
            skipped: Boolean,
        ) {
            val now = System.currentTimeMillis()
            val existing = onboardingRepository.getState(userId)
            val started = existing?.startedAt ?: now
            val completedSteps = (existing?.completedSteps ?: emptySet()) + if (!skipped) setOf(from) else emptySet()
            val skippedSteps = (existing?.skippedSteps ?: emptySet()) + if (skipped) setOf(from) else emptySet()
            onboardingRepository.saveState(
                userId,
                OnboardingState(
                    currentStep = to,
                    completedSteps = completedSteps,
                    skippedSteps = skippedSteps,
                    startedAt = started,
                ),
            )
            onboardingRepository.saveAssessment(
                _uiState.value.draft.toAssessment(userId, _uiState.value.ageSafetyCategory, now),
            )
        }

        private suspend fun computeAffinity() {
            val assessment = _uiState.value.draft.toAssessment(userId, _uiState.value.ageSafetyCategory, System.currentTimeMillis())
            val result = affinityAssessor.assess(assessment, classRepository.definitions())
            _uiState.update {
                it.copy(
                    affinity = result,
                    draft = it.draft.copy(selectedClassId = it.draft.selectedClassId ?: result.recommendedClassId),
                )
            }
        }

        private suspend fun generatePlan() {
            val now = System.currentTimeMillis()
            val assessment = _uiState.value.draft.toAssessment(userId, _uiState.value.ageSafetyCategory, now)
            val candidates =
                questTemplateRepository.getTemplates().mapNotNull { template ->
                    QuestActivityProfileCatalog.forTemplate(template.id)?.let { QuestPlanCandidate(template, it) }
                }
            val plan = planGenerator.generate(assessment, candidates, now)
            val skipped = onboardingRepository.getState(userId)?.skippedSteps ?: emptySet()
            val suggestions = assessmentSuggester.suggest(assessment, skipped)
            _uiState.update { it.copy(plan = plan.copy(assessmentSuggestions = suggestions)) }
        }

        /** Finalize onboarding: create the profile + persist everything, then route to Status. */
        fun complete() {
            val state = _uiState.value
            if (state.ageIneligible) return
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val category = state.ageSafetyCategory
                val draft = state.draft
                // Profile must exist first (safety/assessment/class/completion all attach to it).
                playerRepository.ensureLocalPlayer(draft.displayName.ifBlank { "Hunter" })
                onboardingRepository.saveDisplayName(userId, draft.displayName.ifBlank { "Hunter" })
                playerRepository.setWeightUnit(userId, draft.weightUnit)
                onboardingRepository.saveSafetyProfile(userId, category, ageClassifier.socialDefaults(category))
                onboardingRepository.saveAssessment(draft.toAssessment(userId, category, now))
                draft.selectedClassId?.let {
                    classRepository.setClasses(
                        userId,
                        it,
                        null,
                        selectionReason = "onboarding",
                        changeSource = ClassChangeSource.ONBOARDING,
                    )
                }
                state.affinity?.let { onboardingRepository.saveAffinity(userId, it, now) }
                state.plan?.let { onboardingRepository.savePlan(it) }
                onboardingRepository.saveState(
                    userId,
                    (onboardingRepository.getState(userId) ?: OnboardingState(startedAt = now)).copy(
                        currentStep = OnboardingStep.COMPLETION,
                        completedAt = now,
                    ),
                )
                onboardingRepository.markCompleted(userId, ONBOARDING_VERSION)
                _uiState.update { it.copy(completed = true, currentStep = OnboardingStep.COMPLETION) }
            }
        }

        private fun validate(state: OnboardingUiState): String? {
            val d = state.draft
            return when (state.currentStep) {
                OnboardingStep.BASIC_PROFILE ->
                    when {
                        d.displayName.isBlank() -> "Enter a display name to continue."
                        d.ageRange == null -> "Select an age range to continue."
                        else -> null
                    }
                OnboardingStep.GOALS -> if (d.primaryGoal == null) "Choose a primary goal to continue." else null
                OnboardingStep.TRAINING_BACKGROUND ->
                    if (d.trainingFrequency == null) "Tell us how often you've trained recently." else null
                OnboardingStep.EQUIPMENT_ENVIRONMENT -> if (d.environment == null) "Select where you'll train." else null
                OnboardingStep.AVAILABILITY ->
                    when {
                        d.trainingDays == null -> "Choose how many days you can train."
                        d.sessionDuration == null -> "Choose a typical session length."
                        else -> null
                    }
                OnboardingStep.PHYSIOLOGY_LIMITATIONS ->
                    if (!d.safetyAcknowledged) "Please acknowledge the safety note to continue." else null
                else -> null
            }
        }
    }
