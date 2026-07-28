package com.ascend.core.domain.quest

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.data.repository.ClassRepositoryImpl
import com.ascend.core.data.repository.ProgressionEventRepositoryImpl
import com.ascend.core.data.repository.ProgressionRepositoryImpl
import com.ascend.core.data.repository.QuestRepositoryImpl
import com.ascend.core.data.repository.QuestTemplateRepositoryImpl
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.classes.ClassRewardApplier
import com.ascend.core.domain.classes.MulticlassRewardCalculator
import com.ascend.core.domain.progression.AttributeProgressCalculator
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.ProgressionEventFactory
import com.ascend.core.domain.progression.ProgressionEventPublisher
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.progression.XpCalculator
import com.ascend.core.domain.repository.AddProgressResult
import com.ascend.core.domain.usecase.SeedExerciseCatalogUseCase
import com.ascend.core.model.QuestTargetValidation
import com.ascend.core.model.QuestTemplate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CreateQuestFromTemplateFlowTest {
    private lateinit var db: AscendDatabase
    private lateinit var quests: QuestRepositoryImpl
    private lateinit var templates: QuestTemplateRepositoryImpl
    private lateinit var useCase: CreateQuestFromTemplateUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        val level = LevelCalculator()
        val progression = ProgressionRepositoryImpl(db, db.playerDao(), db.xpDao(), db.attributeDao(), level, RankCalculator())
        val classRepo = ClassRepositoryImpl(db, db.classDao(), level)
        val publisher =
            ProgressionEventPublisher(
                ProgressionEventRepositoryImpl(db, db.progressionEventDao()),
                ProgressionEventFactory(),
                classRepo,
                level,
            )
        quests =
            QuestRepositoryImpl(
                db, db.questDao(), db.exerciseDao(), progression, XpCalculator(),
                AttributeProgressCalculator(), ClassRewardApplier(classRepo, MulticlassRewardCalculator()), publisher,
            )
        templates = QuestTemplateRepositoryImpl(db.questTemplateDao())
        useCase = CreateQuestFromTemplateUseCase(quests, templates, QuestTargetValidator())
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = LOCAL_USER_ID, displayName = "Tester", createdAt = 0, updatedAt = 0))
            SeedExerciseCatalogUseCase(db.exerciseDao())()
            templates.seed()
        }
    }

    @After
    fun tearDown() = db.close()

    private fun request(
        target: Int,
        confirmed: Boolean = false,
        recurrence: String? = null,
    ) = QuestFromTemplateRequest(
        userId = LOCAL_USER_ID,
        templateId = "tmpl-pushups",
        target = target,
        confirmedHighTarget = confirmed,
        recurrenceRule = recurrence,
    )

    @Test
    fun `creates a quest from a template with a valid custom target`() =
        runTest {
            val result = useCase.create(request(125)) as CreateFromTemplateResult.Created
            val quest = quests.getQuest(result.questId)!!
            assertEquals("Push-ups", quest.title)
            assertEquals(125.0, quest.primaryObjective!!.target, 0.0001)
            assertEquals(350L, quest.baseRewardXp)
            assertEquals(25, quest.primaryObjective!!.preferredSetSize)
        }

    @Test
    fun `rejects targets outside the configured range`() =
        runTest {
            val low = useCase.create(request(10)) as CreateFromTemplateResult.Rejected
            assertEquals(QuestTargetValidation.Rejected.Reason.BELOW_MINIMUM, low.validation.reason)
            val high = useCase.create(request(600)) as CreateFromTemplateResult.Rejected
            assertEquals(QuestTargetValidation.Rejected.Reason.ABOVE_MAXIMUM, high.validation.reason)
        }

    @Test
    fun `a high target needs confirmation, then creates`() =
        runTest {
            assertTrue(useCase.create(request(400)) is CreateFromTemplateResult.NeedsConfirmation)
            assertTrue(useCase.create(request(400, confirmed = true)) is CreateFromTemplateResult.Created)
        }

    @Test
    fun `a recurring customized quest persists its recurrence rule`() =
        runTest {
            val result = useCase.create(request(100, recurrence = "FREQ=DAILY")) as CreateFromTemplateResult.Created
            assertEquals("FREQ=DAILY", db.questDao().getQuest(result.questId)!!.recurrenceRule)
        }

    @Test
    fun `a customized quest can be saved as a reusable template`() =
        runTest {
            val custom =
                QuestTemplate(
                    id = "tmpl-custom-1", name = "My Push Day", objectiveType = com.ascend.core.model.ObjectiveType.REPETITIONS,
                    unit = "reps", primaryAttribute = com.ascend.core.model.AttributeType.STRENGTH, exerciseId = "ex-pushup",
                    minimumTarget = 25, maximumTarget = 500, defaultTarget = 150, targetStep = 5, defaultQuickAddValues = listOf(25),
                    defaultPreferredSetSize = 30, minimumAllowedSetSize = 10, maximumAllowedSetSize = 50,
                    supportsAutomaticProgress = false, supportsManualProgress = true, supportedVariations = emptyList(),
                    safetyWarningThreshold = 300, baseRewardXp = 350,
                )
            templates.saveCustomTemplate(custom)
            val loaded = templates.getTemplate("tmpl-custom-1")!!
            assertEquals("My Push Day", loaded.name)
            assertTrue(!loaded.isBuiltIn)
        }

    @Test
    fun `target can be reduced or increased after progress, preserving logged sets`() =
        runTest {
            val questId = (useCase.create(request(100)) as CreateFromTemplateResult.Created).questId
            val objectiveId = quests.getQuest(questId)!!.primaryObjective!!.id
            quests.addProgress(objectiveId, 60.0) as AddProgressResult.Added

            // Reduce the target below current progress -> objective completes, progress kept.
            assertTrue(quests.updateTarget(objectiveId, 50.0))
            var objective = quests.getQuest(questId)!!.primaryObjective!!
            assertEquals(50.0, objective.target, 0.0001)
            assertEquals(60.0, objective.current, 0.0001)
            assertTrue(objective.isComplete)

            // Increase the target -> remaining recomputed from preserved progress.
            assertTrue(quests.updateTarget(objectiveId, 150.0))
            objective = quests.getQuest(questId)!!.primaryObjective!!
            assertEquals(150.0, objective.target, 0.0001)
            assertEquals(90.0, objective.remaining, 0.0001)
        }

    @Test
    fun `seed loads the five built-in templates`() =
        runTest {
            val all = templates.getTemplates()
            assertEquals(5, all.size)
            assertEquals(25..500, all.first { it.id == "tmpl-pushups" }.allowedTargetRange)
        }
}
