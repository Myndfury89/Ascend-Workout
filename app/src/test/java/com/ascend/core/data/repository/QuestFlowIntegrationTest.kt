package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.classes.ClassProgressionCalculator
import com.ascend.core.domain.classes.ClassRewardApplier
import com.ascend.core.domain.progression.AttributeProgressCalculator
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.ProgressionEventFactory
import com.ascend.core.domain.progression.ProgressionEventPublisher
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.progression.XpCalculator
import com.ascend.core.domain.repository.AddProgressResult
import com.ascend.core.domain.repository.CompleteQuestResult
import com.ascend.core.domain.repository.NewObjectiveSpec
import com.ascend.core.domain.repository.NewQuestSpec
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.QuestType
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

/**
 * End-to-end proof of Milestone 1's 200-push-up flow (spec acceptance criteria 5-15):
 * create quest -> log uneven custom sets -> delete a set -> finish -> complete ->
 * XP exactly once -> level up -> Strength + Discipline rise.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuestFlowIntegrationTest {
    private lateinit var db: AscendDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var quests: QuestRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java)
                .allowMainThreadQueries().build()
        progression =
            ProgressionRepositoryImpl(
                db, db.playerDao(), db.xpDao(), db.attributeDao(), LevelCalculator(), RankCalculator(),
            )
        val levelCalc = LevelCalculator()
        val classRepo = ClassRepositoryImpl(db, db.classDao(), levelCalc)
        val classApplier = ClassRewardApplier(classRepo, ClassProgressionCalculator())
        val publisher =
            ProgressionEventPublisher(
                ProgressionEventRepositoryImpl(db, db.progressionEventDao()),
                ProgressionEventFactory(),
                classRepo,
                levelCalc,
            )
        quests =
            QuestRepositoryImpl(
                db, db.questDao(), db.exerciseDao(), progression, XpCalculator(),
                AttributeProgressCalculator(), classApplier, publisher,
            )
        runBlocking {
            db.playerDao().upsertProfile(
                UserProfileEntity(id = LOCAL_USER_ID, displayName = "Tester", createdAt = 0, updatedAt = 0),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun create200PushUpQuest(): Pair<String, String> {
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
                                preferredSetSize = 25,
                                minimumSetSize = 10,
                                maximumSetSize = 50,
                                primaryAttribute = AttributeType.STRENGTH,
                            ),
                        ),
                ),
            )
        val objectiveId = quests.getQuest(questId)!!.primaryObjective!!.id
        return questId to objectiveId
    }

    @Test
    fun `full 200 push-up flow awards xp once and grows strength and discipline`() =
        runTest {
            val (questId, objectiveId) = create200PushUpQuest()

            // Log uneven custom sets: 25, 25, 40, 30 = 120.
            quests.addProgress(objectiveId, 25.0)
            quests.addProgress(objectiveId, 25.0)
            val fortySet = quests.addProgress(objectiveId, 40.0) as AddProgressResult.Added
            quests.addProgress(objectiveId, 30.0)

            var quest = quests.getQuest(questId)!!
            assertEquals(120.0, quest.totalCurrent, 0.0001)
            assertEquals(80.0, quest.remaining, 0.0001)

            // Delete the 40 set -> 80 remaining becomes 120.
            assertTrue(quests.deleteProgress(fortySet.entryId))
            quest = quests.getQuest(questId)!!
            assertEquals(80.0, quest.totalCurrent, 0.0001)

            // Finish it off: 40 + 40 + 40 = 200 total.
            quests.addProgress(objectiveId, 40.0)
            quests.addProgress(objectiveId, 40.0)
            val last = quests.addProgress(objectiveId, 40.0) as AddProgressResult.Added
            assertTrue(last.objectiveComplete)
            assertEquals(200.0, quests.getQuest(questId)!!.totalCurrent, 0.0001)

            // Complete -> XP once, level up, attributes.
            val result = quests.completeQuest(questId) as CompleteQuestResult.Completed
            assertEquals(350L, result.xpAwarded)
            assertTrue(result.leveledUp)
            assertEquals(30L, result.attributeDeltas[AttributeType.STRENGTH])
            assertEquals(10L, result.attributeDeltas[AttributeType.DISCIPLINE])

            val progress = progression.getProgress(LOCAL_USER_ID)!!
            assertEquals(350L, progress.lifetimeXp)
            assertEquals(2, progress.level)
            val stats = db.playerDao().getStats(LOCAL_USER_ID)!!
            assertEquals(30L, stats.strength)
            assertEquals(10L, stats.discipline)

            // Completing again must not double-award XP or attributes.
            assertEquals(CompleteQuestResult.AlreadyCompleted, quests.completeQuest(questId))
            assertEquals(350L, progression.getProgress(LOCAL_USER_ID)!!.lifetimeXp)
            assertEquals(30L, db.playerDao().getStats(LOCAL_USER_ID)!!.strength)
        }

    @Test
    fun `imported duplicate progress does not double count`() =
        runTest {
            val (questId, objectiveId) = create200PushUpQuest()
            val first =
                quests.addProgress(
                    objectiveId,
                    50.0,
                    source = com.ascend.core.model.ProgressSource.HEALTH_CONNECT,
                    sourceApplication = "com.example.health",
                    externalRecordId = "rec-1",
                )
            val dup =
                quests.addProgress(
                    objectiveId,
                    50.0,
                    source = com.ascend.core.model.ProgressSource.HEALTH_CONNECT,
                    sourceApplication = "com.example.health",
                    externalRecordId = "rec-1",
                )
            assertTrue(first is AddProgressResult.Added)
            assertEquals(AddProgressResult.Duplicate, dup)
            assertEquals(50.0, quests.getQuest(questId)!!.totalCurrent, 0.0001)
        }
}
