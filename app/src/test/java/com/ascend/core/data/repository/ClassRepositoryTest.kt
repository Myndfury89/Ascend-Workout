package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassSlot
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ClassRepositoryTest {
    private lateinit var db: AscendDatabase
    private lateinit var classes: ClassRepositoryImpl

    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        classes = ClassRepositoryImpl(db, db.classDao(), LevelCalculator())
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = userId, displayName = "Tester", createdAt = 0, updatedAt = 0))
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `seed data loads correctly and round-trips the definition fields`() =
        runTest {
            classes.seedDefinitions()
            classes.seedDefinitions() // idempotent

            val defs = classes.definitions()
            assertEquals(3, defs.size)

            val berserker = classes.definition("berserker")!!
            assertEquals(1.50, berserker.attributeMultipliers[AttributeType.STRENGTH]!!, 1e-9)
            assertEquals("FORCE", berserker.uniqueProficiencyKey)
            assertTrue(berserker.favoredTags.contains("HEAVY_STRENGTH"))
            assertEquals(1.30, berserker.favoredClassXpMultiplier, 1e-9)
            assertEquals("berserker", berserker.presentation.statusThemeKey)
        }

    @Test
    fun `switching classes records history and preserves earned class XP`() =
        runTest {
            classes.setClasses(userId, primaryClassId = "monk", secondaryClassId = null)
            classes.awardClassXp(userId, "monk", 200, XpSourceType.WORKOUT_COMPLETION, "w1")

            // Switch primary class to Berserker.
            classes.setClasses(userId, primaryClassId = "berserker", secondaryClassId = null)

            // Earned Monk XP survives the switch.
            assertEquals(200L, classes.totalClassXp(userId, "monk"))

            val history = classes.getHistory(userId).filter { it.slot == ClassSlot.PRIMARY }
            assertEquals(listOf("monk", "berserker"), history.map { it.classId })
            // The first selection is closed, the current one is open.
            assertTrue(history[0].endedAt != null)
            assertNull(history[1].endedAt)

            val selection = classes.getSelection(userId)
            assertEquals("berserker", selection.primaryClassId)
            assertTrue(selection.primaryStartedAt != null)
        }

    @Test
    fun `class progress derives level and proficiency from the ledgers`() =
        runTest {
            classes.setClasses(userId, primaryClassId = "monk", secondaryClassId = null)
            classes.awardClassXp(userId, "monk", 250, XpSourceType.WORKOUT_COMPLETION, "w1")
            classes.awardProficiency(userId, "BODY_MASTERY", 40, XpSourceType.WORKOUT_COMPLETION, "w1")

            val progress = classes.classProgress(userId, "monk")
            assertEquals(250L, progress.classXp)
            assertEquals(2, progress.classLevel) // 250 >= 200 (level-2 threshold)
            assertEquals("BODY_MASTERY", progress.uniqueProficiencyKey)
            assertEquals(40L, progress.uniqueProficiency)
        }
}
