package com.ascend.feature.onboarding

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.data.repository.ClassRepositoryImpl
import com.ascend.core.data.repository.OnboardingRepositoryImpl
import com.ascend.core.data.repository.PlayerRepositoryImpl
import com.ascend.core.data.repository.QuestTemplateRepositoryImpl
import com.ascend.core.database.AscendDatabase
import com.ascend.core.domain.classes.ClassRecommendationEngine
import com.ascend.core.domain.onboarding.AgeSafetyClassifier
import com.ascend.core.domain.onboarding.AssessmentQuestSuggester
import com.ascend.core.domain.onboarding.ClassAffinityAssessor
import com.ascend.core.domain.onboarding.InitialQuestPlanGenerator
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.model.onboarding.AgeRange
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.Equipment
import com.ascend.core.model.onboarding.OnboardingState
import com.ascend.core.model.onboarding.OnboardingStep
import com.ascend.core.model.onboarding.PrimaryGoal
import com.ascend.core.model.onboarding.Provenance
import com.ascend.core.model.onboarding.SessionDuration
import com.ascend.core.model.onboarding.TrainingDaysPerWeek
import com.ascend.core.model.onboarding.TrainingEnvironment
import com.ascend.core.model.onboarding.TrainingFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class OnboardingViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var db: AscendDatabase
    private lateinit var onboardingRepo: OnboardingRepositoryImpl
    private lateinit var playerRepo: PlayerRepositoryImpl
    private lateinit var classRepo: ClassRepositoryImpl
    private lateinit var templateRepo: QuestTemplateRepositoryImpl

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // Route Room's suspend query/transaction work onto the test dispatcher so it runs
        // synchronously with the ViewModel's coroutines (otherwise Room hops to its own executor).
        db =
            Room.inMemoryDatabaseBuilder(ctx, AscendDatabase::class.java)
                .setQueryExecutor(mainRule.dispatcher.asExecutor())
                .setTransactionExecutor(mainRule.dispatcher.asExecutor())
                .allowMainThreadQueries()
                .build()
        onboardingRepo = OnboardingRepositoryImpl(db.onboardingDao(), db.playerDao()) { 100L }
        playerRepo = PlayerRepositoryImpl(db, db.playerDao(), LevelCalculator())
        classRepo = ClassRepositoryImpl(db, db.classDao(), LevelCalculator())
        templateRepo = QuestTemplateRepositoryImpl(db.questTemplateDao())
    }

    @After
    fun tearDown() = db.close()

    private fun idle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    private fun newViewModel() =
        OnboardingViewModel(
            onboardingRepo,
            playerRepo,
            classRepo,
            templateRepo,
            AgeSafetyClassifier(),
            ClassAffinityAssessor(ClassRecommendationEngine()),
            InitialQuestPlanGenerator(),
            AssessmentQuestSuggester(),
        )

    /** Advance one step (validate current + run the launched transition). */
    private fun OnboardingViewModel.step() {
        next()
        idle()
    }

    /** Fill required fields and advance from Welcome to the given target step. */
    private fun OnboardingViewModel.walkTo(target: OnboardingStep) {
        step() // Welcome -> Basic profile
        updateDraft { it.copy(displayName = "Kai") }
        selectAgeRange(AgeRange.AGE_25_34)
        if (target == OnboardingStep.BASIC_PROFILE) return
        step() // -> Goals
        updateDraft {
            it.copy(
                primaryGoal = PrimaryGoal.STRENGTH,
                preferences = setOf(com.ascend.core.model.onboarding.ActivityPreference.WEIGHTLIFTING),
            )
        }
        if (target == OnboardingStep.GOALS) return
        step() // -> Training background
        updateDraft { it.copy(trainingFrequency = TrainingFrequency.THREE_TO_FOUR_WEEKLY) }
        if (target == OnboardingStep.TRAINING_BACKGROUND) return
        step() // -> Activity preferences
        if (target == OnboardingStep.ACTIVITY_PREFERENCES) return
        step() // -> Equipment & environment
        updateDraft { it.copy(environment = TrainingEnvironment.HOME, equipment = setOf(Equipment.PULL_UP_BAR)) }
        if (target == OnboardingStep.EQUIPMENT_ENVIRONMENT) return
        step() // -> Availability
        updateDraft { it.copy(trainingDays = TrainingDaysPerWeek.THREE, sessionDuration = SessionDuration.MIN_30_45) }
        if (target == OnboardingStep.AVAILABILITY) return
        step() // -> Ability snapshot
        if (target == OnboardingStep.ABILITY_SNAPSHOT) return
        step() // -> Physiology & limitations
        updateDraft { it.copy(safetyAcknowledged = true) }
        if (target == OnboardingStep.PHYSIOLOGY_LIMITATIONS) return
        step() // -> Class affinity
        if (target == OnboardingStep.CLASS_AFFINITY) return
        step() // -> Plan review
    }

    @Test
    fun `below-minimum age blocks onboarding and creates no profile`() {
        val vm = newViewModel()
        idle()
        vm.selectAgeRange(AgeRange.UNDER_13)
        assertTrue(vm.uiState.value.ageIneligible)
        assertEquals(AgeSafetyCategory.BELOW_MINIMUM, vm.uiState.value.ageSafetyCategory)
        vm.complete()
        idle()
        assertFalse(vm.uiState.value.completed)
        assertNull("no profile is created for a below-minimum-age user", runBlocking { db.playerDao().getProfile(LOCAL_USER_ID) })
    }

    @Test
    fun `a required field blocks advancing with a neutral message`() {
        val vm = newViewModel()
        idle()
        vm.next() // Welcome -> Basic profile
        idle()
        assertEquals(OnboardingStep.BASIC_PROFILE, vm.uiState.value.currentStep)
        vm.next() // blank name + no age
        idle()
        assertNotNull(vm.uiState.value.validationError)
        assertEquals(OnboardingStep.BASIC_PROFILE, vm.uiState.value.currentStep)
    }

    @Test
    fun `class affinity defaults to the recommendation yet any class stays choosable`() {
        val vm = newViewModel()
        idle()
        vm.walkTo(OnboardingStep.CLASS_AFFINITY)
        idle()
        assertEquals(OnboardingStep.CLASS_AFFINITY, vm.uiState.value.currentStep)
        assertEquals("berserker", vm.uiState.value.affinity?.recommendedClassId)
        assertEquals("berserker", vm.uiState.value.draft.selectedClassId)
        vm.updateDraft { it.copy(selectedClassId = "magician") }
        assertEquals("magician", vm.uiState.value.draft.selectedClassId)
    }

    @Test
    fun `completing persists a self-reported profile, class, and plan without any rewards`() {
        val vm = newViewModel()
        idle()
        vm.walkTo(OnboardingStep.PLAN_REVIEW)
        idle()
        assertNotNull("a provisional plan is generated", vm.uiState.value.plan)
        vm.complete()
        idle()

        assertTrue(vm.uiState.value.completed)
        val profile = runBlocking { db.playerDao().getProfile(LOCAL_USER_ID) }!!
        assertTrue("onboarding marked complete", profile.onboardingCompleted)
        assertEquals("Kai", profile.displayName)
        assertEquals("ADULT", profile.ageSafetyCategory)
        assertEquals("PRIVATE", profile.socialVisibility)
        assertFalse(profile.partyPresenceEnabled)

        val assessment = runBlocking { onboardingRepo.getAssessment(LOCAL_USER_ID) }!!
        assertEquals(Provenance.SELF_REPORTED, assessment.provenance)
        assertNull(assessment.replacedByEvidenceAt)
        assertEquals("berserker", runBlocking { classRepo.getSelection(LOCAL_USER_ID) }.primaryClassId)

        // Onboarding awards nothing: no progression events, no XP transactions.
        db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT COUNT(*) FROM progression_event")).use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        val progress = runBlocking { db.playerDao().getProgress(LOCAL_USER_ID) }!!
        assertEquals("no XP awarded from onboarding", 0L, progress.lifetimeXp)
    }

    @Test
    fun `onboarding resumes from the saved step with entered values`() {
        // Simulate an interrupted session: state at Goals + a persisted provisional assessment.
        runBlocking {
            onboardingRepo.saveState(LOCAL_USER_ID, OnboardingState(currentStep = OnboardingStep.GOALS, startedAt = 1L))
            onboardingRepo.saveAssessment(
                com.ascend.core.model.onboarding.InitialAssessment(
                    userId = LOCAL_USER_ID,
                    primaryGoal = PrimaryGoal.ENDURANCE,
                    equipment = setOf(Equipment.DUMBBELLS),
                    ageSafetyCategory = AgeSafetyCategory.ADULT,
                ),
            )
        }
        val vm = newViewModel()
        idle()
        assertEquals(OnboardingStep.GOALS, vm.uiState.value.currentStep)
        assertEquals(PrimaryGoal.ENDURANCE, vm.uiState.value.draft.primaryGoal)
        assertTrue(Equipment.DUMBBELLS in vm.uiState.value.draft.equipment)
    }
}
