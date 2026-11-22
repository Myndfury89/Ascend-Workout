package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.data.mapper.toEntity
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.ClassDefinitionEntity
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.classes.ClassCatalog
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.model.ClassChangeSource
import com.ascend.core.model.ClassHistory
import com.ascend.core.model.PlayerClassSelection
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

/**
 * Existing installs keep customized class rows and player progression when seeding
 * runs again. Only catalog ids that are not already stored are inserted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ClassSeedInsertMissingTest {
    private lateinit var db: AscendDatabase
    private lateinit var classes: ClassRepositoryImpl

    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        classes = ClassRepositoryImpl(db, db.classDao(), LevelCalculator())
        runBlocking {
            db.playerDao().upsertProfile(
                UserProfileEntity(id = userId, displayName = "Tester", createdAt = 0, updatedAt = 0),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `fresh seed inserts each catalog class once and a repeat is a no-op`() =
        runTest {
            classes.seedDefinitions()
            val first = db.classDao().getDefinitions()

            assertEquals(ClassCatalog.ALL.map { it.id }.toSet(), first.map { it.id }.toSet())
            assertEquals(ClassCatalog.ALL.size, db.classDao().countDefinitions())
            assertNoDuplicateIds(first)

            classes.seedDefinitions()

            assertEquals(first, db.classDao().getDefinitions())
        }

    @Test
    fun `existing three class install keeps edits and custom rows`() =
        runTest {
            classes.seedDefinitions()
            val originals = db.classDao().getDefinitions().associateBy { it.id }
            val edited =
                originals.getValue("berserker").copy(
                    name = "Custom Berserker",
                    description = "Edited in place",
                    favoredTags = "CUSTOM_TAG",
                    attributeMultipliers = "STRENGTH:2.0",
                    favoredClassXpMultiplier = 9.9,
                    enabled = false,
                    updatedAt = 99L,
                )
            val custom =
                originals.getValue("mage").copy(
                    id = "custom-path",
                    name = "My Path",
                    classTitle = "Homebrew",
                    description = "User-authored class row",
                    createdAt = 5L,
                    updatedAt = 5L,
                )
            db.classDao().upsertDefinitions(listOf(edited, custom))
            val progress = installPlayerProgress(secondaryClassId = "berserker")
            val before = db.classDao().getDefinitions()

            classes.seedDefinitions()
            classes.seedDefinitions()

            assertEquals(before, db.classDao().getDefinitions())
            assertEquals(4, db.classDao().countDefinitions())
            assertNoDuplicateIds(db.classDao().getDefinitions())
            assertPlayerProgressUntouched(progress)
            assertEquals("Custom Berserker", classes.definition("berserker")!!.name)
            assertEquals(false, classes.definition("berserker")!!.enabled)
            assertEquals(9.9, classes.definition("berserker")!!.favoredClassXpMultiplier, 0.0)
            assertEquals("My Path", classes.definition("custom-path")!!.name)
            assertEquals(setOf("CUSTOM_TAG"), classes.definition("berserker")!!.favoredTags)
        }

    @Test
    fun `missing catalog class is inserted without resetting edits custom rows or player progress`() =
        runTest {
            val seededAt = 1_700_000_000_000L
            val berserker = ClassCatalog.BERSERKER.toEntity(seededAt)
            val monk =
                ClassCatalog.MONK.toEntity(seededAt).copy(
                    name = "Edited Monk",
                    classTitle = "Custom Title",
                    description = "Kept edit",
                    favoredClassXpMultiplier = 9.9,
                    updatedAt = 11L,
                )
            val custom =
                ClassCatalog.MAGE.toEntity(seededAt).copy(
                    id = "custom-path",
                    name = "My Path",
                    classTitle = "Homebrew",
                    description = "User-authored class row",
                    fitnessIdentity = "Custom identity",
                    createdAt = 5L,
                    updatedAt = 5L,
                )
            db.classDao().upsertDefinitions(listOf(berserker, monk, custom))
            assertNull(db.classDao().getDefinition("mage"))
            assertEquals(
                setOf("berserker", "monk", "custom-path"),
                classes.definitions().map { it.id }.toSet(),
            )
            val progress = installPlayerProgress(secondaryClassId = "custom-path")

            classes.seedDefinitions()

            assertEquals(berserker, db.classDao().getDefinition("berserker"))
            assertEquals(monk, db.classDao().getDefinition("monk"))
            assertEquals(custom, db.classDao().getDefinition("custom-path"))
            val mage = db.classDao().getDefinition("mage")!!
            assertEquals(ClassCatalog.MAGE.toEntity(mage.createdAt), mage)
            assertTrue(mage.createdAt > seededAt)
            assertEquals(ClassCatalog.MAGE.name, classes.definition("mage")!!.name)
            assertEquals(ClassCatalog.MAGE.uniqueProficiencyKey, classes.definition("mage")!!.uniqueProficiencyKey)
            assertEquals("Edited Monk", classes.definition("monk")!!.name)
            assertEquals(9.9, classes.definition("monk")!!.favoredClassXpMultiplier, 0.0)
            assertEquals(
                setOf("berserker", "monk", "mage", "custom-path"),
                classes.definitions().map { it.id }.toSet(),
            )
            assertEquals(4, db.classDao().countDefinitions())
            assertNoDuplicateIds(db.classDao().getDefinitions())
            assertPlayerProgressUntouched(progress)

            val snapshot = db.classDao().getDefinitions()
            classes.seedDefinitions()

            assertEquals(snapshot, db.classDao().getDefinitions())
            assertPlayerProgressUntouched(progress)
        }

    private suspend fun installPlayerProgress(secondaryClassId: String): PlayerProgress {
        classes.setClasses(
            userId = userId,
            primaryClassId = "monk",
            secondaryClassId = secondaryClassId,
            selectionReason = "existing install",
            changeSource = ClassChangeSource.ONBOARDING,
        )
        classes.awardClassXp(userId, "monk", 250, XpSourceType.WORKOUT_COMPLETION, "w-monk")
        classes.awardClassXp(userId, "berserker", 80, XpSourceType.WORKOUT_COMPLETION, "w-berserker")
        classes.awardClassXp(userId, "custom-path", 10, XpSourceType.WORKOUT_COMPLETION, "w-custom")
        classes.awardProficiency(userId, "BODY_MASTERY", 40, XpSourceType.WORKOUT_COMPLETION, "w-monk")
        classes.awardProficiency(userId, "FORCE", 15, XpSourceType.WORKOUT_COMPLETION, "w-berserker")
        assertEquals(250L, classes.totalClassXp(userId, "monk"))
        assertEquals(80L, classes.totalClassXp(userId, "berserker"))
        assertEquals(10L, classes.totalClassXp(userId, "custom-path"))
        assertEquals(40L, classes.totalProficiency(userId, "BODY_MASTERY"))
        assertEquals(15L, classes.totalProficiency(userId, "FORCE"))
        val selection = classes.getSelection(userId)
        assertEquals("monk", selection.primaryClassId)
        assertEquals(secondaryClassId, selection.secondaryClassId)
        return PlayerProgress(
            selection = selection,
            history = classes.getHistory(userId).toSet(),
            monkXp = 250L,
            berserkerXp = 80L,
            customXp = 10L,
            bodyMastery = 40L,
            force = 15L,
        )
    }

    private suspend fun assertPlayerProgressUntouched(before: PlayerProgress) {
        assertEquals(before.selection, classes.getSelection(userId))
        assertEquals(before.history, classes.getHistory(userId).toSet())
        assertEquals(before.monkXp, classes.totalClassXp(userId, "monk"))
        assertEquals(before.berserkerXp, classes.totalClassXp(userId, "berserker"))
        assertEquals(before.customXp, classes.totalClassXp(userId, "custom-path"))
        assertEquals(before.bodyMastery, classes.totalProficiency(userId, "BODY_MASTERY"))
        assertEquals(before.force, classes.totalProficiency(userId, "FORCE"))
    }

    private fun assertNoDuplicateIds(rows: List<ClassDefinitionEntity>) {
        val ids = rows.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    private data class PlayerProgress(
        val selection: PlayerClassSelection,
        val history: Set<ClassHistory>,
        val monkXp: Long,
        val berserkerXp: Long,
        val customXp: Long,
        val bodyMastery: Long,
        val force: Long,
    )
}
