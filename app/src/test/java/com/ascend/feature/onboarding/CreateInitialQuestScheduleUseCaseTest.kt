package com.ascend.feature.onboarding

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.data.repository.QuestTemplateRepositoryImpl
import com.ascend.core.database.AscendDatabase
import com.ascend.core.domain.onboarding.CreateInitialQuestScheduleUseCase
import com.ascend.core.domain.quest.CreateQuestFromTemplateUseCase
import com.ascend.core.domain.quest.QuestTargetValidator
import com.ascend.core.model.onboarding.DifficultyBand
import com.ascend.core.model.onboarding.InitialQuestDefinition
import com.ascend.core.model.onboarding.InitialQuestPlan
import kotlinx.coroutines.flow.first
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

/** The provisional plan is realized into real quests via existing infra — idempotently, no rewards. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CreateInitialQuestScheduleUseCaseTest {
    private lateinit var db: AscendDatabase
    private lateinit var templateRepo: QuestTemplateRepositoryImpl
    private lateinit var quests: FakeQuestRepository
    private lateinit var useCase: CreateInitialQuestScheduleUseCase
    private val userId = "u1"

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AscendDatabase::class.java).allowMainThreadQueries().build()
        templateRepo = QuestTemplateRepositoryImpl(db.questTemplateDao())
        quests = FakeQuestRepository()
        useCase =
            CreateInitialQuestScheduleUseCase(
                CreateQuestFromTemplateUseCase(quests, templateRepo, QuestTargetValidator()),
                quests,
            )
        runBlocking { templateRepo.seed() }
    }

    @After
    fun tearDown() = db.close()

    private fun plan(vararg defs: InitialQuestDefinition) =
        InitialQuestPlan(userId, defs.toList(), emptyList(), "rationale", DifficultyBand.FOUNDATION, emptyList())

    @Test
    fun `plan definitions become real starting quests`() =
        runTest {
            val ids =
                useCase.create(
                    userId,
                    plan(
                        InitialQuestDefinition("tmpl-pushups", "Push-ups", "reps", 25, 10, "r"),
                        InitialQuestDefinition("tmpl-steps", "Walking Steps", "steps", 6000, null, "r"),
                    ),
                )
            assertEquals(2, ids.size)
            val titles = quests.observeQuestsForUser(userId).first().map { it.title }.toSet()
            assertEquals(setOf("Push-ups", "Walking Steps"), titles)
        }

    @Test
    fun `re-running the schedule does not duplicate quests`() =
        runTest {
            val p = plan(InitialQuestDefinition("tmpl-pushups", "Push-ups", "reps", 25, 10, "r"))
            useCase.create(userId, p)
            useCase.create(userId, p)
            assertEquals(1, quests.observeQuestsForUser(userId).first().size)
        }

    @Test
    fun `creating the schedule awards nothing`() =
        runTest {
            useCase.create(userId, plan(InitialQuestDefinition("tmpl-pushups", "Push-ups", "reps", 25, 10, "r")))
            // The fake never completes a quest; the created quest is ACTIVE and unearned.
            val quest = quests.observeQuestsForUser(userId).first().single()
            assertTrue("a starting quest is created but not completed", quest.status.name == "ACTIVE")
        }
}
