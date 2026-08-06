package com.ascend.feature.dashboard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.data.repository.ClassRepositoryImpl
import com.ascend.core.data.repository.ProgressionEventRepositoryImpl
import com.ascend.core.data.repository.ProgressionRepositoryImpl
import com.ascend.core.data.repository.QuestRepositoryImpl
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
import com.ascend.core.domain.repository.CompleteQuestResult
import com.ascend.core.domain.repository.NewObjectiveSpec
import com.ascend.core.domain.repository.NewQuestSpec
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.QuestType
import com.ascend.feature.dashboard.prototype.StatusOverlayKind
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end proof that a REAL quest completion — driven through QuestRepository → progression →
 * ProgressionEventPublisher → the persisted ProgressionEventQueue — presents as a Quest Complete
 * window exactly once, replays presentation only, and never re-awards. The Status layer performs no
 * reward mutation: it only reads the drained batch through [StatusComposition.planFor].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuestCompletionPipelineTest {
    private lateinit var db: AscendDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var quests: QuestRepositoryImpl
    private lateinit var eventRepo: ProgressionEventRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        progression =
            ProgressionRepositoryImpl(db, db.playerDao(), db.xpDao(), db.attributeDao(), LevelCalculator(), RankCalculator())
        val levelCalc = LevelCalculator()
        val classRepo = ClassRepositoryImpl(db, db.classDao(), levelCalc)
        val classApplier = ClassRewardApplier(classRepo, MulticlassRewardCalculator())
        eventRepo = ProgressionEventRepositoryImpl(db, db.progressionEventDao())
        val publisher = ProgressionEventPublisher(eventRepo, ProgressionEventFactory(), classRepo, levelCalc)
        quests =
            QuestRepositoryImpl(
                db, db.questDao(), db.exerciseDao(), progression, XpCalculator(),
                AttributeProgressCalculator(), classApplier, publisher,
            )
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = LOCAL_USER_ID, displayName = "Tester", createdAt = 0, updatedAt = 0))
        }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun completeAPushUpQuest(): String {
        val questId =
            quests.createQuest(
                NewQuestSpec(
                    userId = LOCAL_USER_ID,
                    title = "200 Push-Ups",
                    type = QuestType.ACCUMULATION,
                    baseRewardXp = 350,
                    objectives =
                        listOf(
                            NewObjectiveSpec(
                                title = "Push-ups",
                                type = ObjectiveType.REPETITIONS,
                                target = 200.0,
                                unit = "reps",
                                primaryAttribute = AttributeType.STRENGTH,
                            ),
                        ),
                ),
            )
        val objectiveId = quests.getQuest(questId)!!.primaryObjective!!.id
        quests.addProgress(objectiveId, 200.0)
        assertTrue(quests.completeQuest(questId) is CompleteQuestResult.Completed)
        return questId
    }

    @Test
    fun `a completed quest drains as a Quest Complete presentation with real title, identity and ordering`() =
        runTest {
            val questId = completeAPushUpQuest()

            val batch = eventRepo.getPending(LOCAL_USER_ID)
            assertTrue("the completion enqueued a batch", batch.isNotEmpty())
            val plan = StatusComposition.planFor(batch)

            assertNotNull("a real completion presents Quest Complete", plan.questComplete)
            assertEquals("200 Push-Ups", plan.questComplete!!.title)
            assertEquals(questId, plan.questComplete!!.questId)
            assertEquals(350, plan.questComplete!!.xpGained)
            // 350 base XP crosses level 1 -> 2, so the Ascension beat follows the quest window.
            assertEquals(StatusOverlayKind.PLAYER_LEVEL_UP, plan.ascensionOverlay!!.kind)
            assertNotNull("Quest Complete is presented first", plan.steps[0].questComplete)
        }

    @Test
    fun `the presentation is drained exactly once and replay does not re-award`() =
        runTest {
            val questId = completeAPushUpQuest()
            val xpBefore = progression.getProgress(LOCAL_USER_ID)!!.lifetimeXp

            val batch = eventRepo.getPending(LOCAL_USER_ID)
            assertNotNull(StatusComposition.planFor(batch).questComplete)

            // The animation layer marks the batch consumed once its plan finishes.
            eventRepo.markConsumed(batch.map { it.id })

            // Consumed batch does not reappear -> no second presentation.
            val afterConsume = eventRepo.getPending(LOCAL_USER_ID)
            assertTrue("consumed batch must not reappear", afterConsume.isEmpty())
            assertNull(StatusComposition.planFor(afterConsume).questComplete)

            // Re-completing the same quest awards nothing and enqueues no new presentation.
            assertEquals(CompleteQuestResult.AlreadyCompleted, quests.completeQuest(questId))
            assertTrue(eventRepo.getPending(LOCAL_USER_ID).isEmpty())
            assertEquals(xpBefore, progression.getProgress(LOCAL_USER_ID)!!.lifetimeXp)
        }

    @Test
    fun `an unconsumed quest presentation survives an app restart`() =
        runTest {
            completeAPushUpQuest()

            // Simulate process death + relaunch: a fresh queue instance over the same persisted DB.
            val afterRestart = ProgressionEventRepositoryImpl(db, db.progressionEventDao())
            val batch = afterRestart.getPending(LOCAL_USER_ID)
            assertTrue("the pending presentation persists across restart", batch.isNotEmpty())
            assertNotNull(StatusComposition.planFor(batch).questComplete)
        }
}
