package com.ascend.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.core.domain.classes.AwakeningSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v17 -> v18 keeps the three class rows, flags the starters, backfills legacy selections, and
 * leaves Class XP, proficiency, and workout ledgers unchanged.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ClassAwakeningMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AscendDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun `migration 17 to 18 is registered`() {
        assertTrue(AscendMigrations.ALL.any { it.startVersion == 17 && it.endVersion == 18 })
    }

    @Test
    fun `migrate 17 to 18 preserves classes ledgers and backfills awakening`() {
        val dbName = "migration-test-17-18.db"
        helper.createDatabase(dbName, 17).use { db -> seedV17(db) }

        val db = helper.runMigrationsAndValidate(dbName, 18, true, AscendMigrations.MIGRATION_17_18)
        try {
            assertClasses(db)
            assertBackfill(db)
            assertLedgersUnchanged(db)
        } finally {
            db.close()
        }
    }

    private fun seedV17(db: SupportSQLiteDatabase) {
        seedProfilesAndClasses(db)
        seedSelections(db)
        seedLedgers(db)
    }

    private fun seedProfilesAndClasses(db: SupportSQLiteDatabase) {
        listOf("u1", "u2", "u3", "u4").forEach { id ->
            db.execSQL(
                "INSERT INTO user_profile (id, displayName, createdAt, updatedAt, onboardingCompleted, " +
                    "measurementSystem, localOnly, cloudSyncEnabled) " +
                    "VALUES ('$id', 'Tester', 0, 0, 1, 'METRIC', 1, 0)",
            )
        }
        listOf("berserker", "monk", "mage", "wanderer").forEach { id ->
            db.execSQL(
                "INSERT INTO class_definition (id, name, classTitle, description, fitnessIdentity, " +
                    "favoredWorkoutCategories, favoredTags, primaryAttributes, secondaryAttributes, " +
                    "attributeMultipliers, uniqueProficiencyKey, uniqueProficiencyName, " +
                    "favoredClassXpMultiplier, nonFavoredClassXpMultiplier, statusThemeKey, frameVariantKey, " +
                    "accentTokenKey, proficiencyIconKey, idleEffectKey, progressionEffectKey, " +
                    "classQuestTemplateIds, expeditionTemplateIds, achievementPathIds, titleIds, " +
                    "enabled, createdAt, updatedAt) " +
                    "VALUES ('$id', '$id', 't', 'd', 'fi', '', '', '', '', '', 'KEY', 'Name', 1.3, 0.75, " +
                    "'$id', 'frame', 'accent', 'icon', 'idle', 'prog', '', '', '', '', 1, 10, 20)",
            )
        }
    }

    private fun seedSelections(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO player_class (userId, primaryClassId, secondaryClassId, primaryStartedAt, " +
                "secondaryStartedAt, changeSource, updatedAt) " +
                "VALUES ('u1', 'monk', 'berserker', 111, 222, 'ONBOARDING', 333)",
        )
        db.execSQL("INSERT INTO player_class (userId, primaryClassId, updatedAt) VALUES ('u2', 'mage', 444)")
        db.execSQL("INSERT INTO player_class (userId, updatedAt) VALUES ('u3', 1)")
        db.execSQL(
            "INSERT INTO player_class (userId, primaryClassId, secondaryClassId, primaryStartedAt, " +
                "secondaryStartedAt, updatedAt) VALUES ('u4', 'berserker', 'berserker', 555, 666, 777)",
        )
    }

    private fun seedLedgers(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO class_xp_transaction (id, userId, classId, amount, transactionType, sourceType, " +
                "sourceId, rewardType, createdAt) " +
                "VALUES ('xp1', 'u1', 'monk', 240, 'AWARD', 'WORKOUT_COMPLETION', 'w1', 'CLASS_XP', 50)",
        )
        db.execSQL(
            "INSERT INTO class_proficiency_transaction (id, userId, proficiencyKey, amount, transactionType, " +
                "sourceType, sourceId, rewardType, createdAt) " +
                "VALUES ('p1', 'u1', 'BODY_MASTERY', 40, 'AWARD', 'WORKOUT_COMPLETION', 'w1', " +
                "'UNIQUE_PROFICIENCY', 60)",
        )
        db.execSQL(
            "INSERT INTO exercise (id, name, category, primaryAttribute, measurementType, defaultUnit, " +
                "isWeighted, tags, isBuiltIn, createdAt, updatedAt) " +
                "VALUES ('ex-pushup', 'Push-ups', 'Bodyweight', 'STRENGTH', 'REPETITIONS', 'reps', 0, " +
                "'BODYWEIGHT', 1, 0, 0)",
        )
        db.execSQL(
            "INSERT INTO workout (id, userId, title, difficulty, status, performedAt, createdAt, updatedAt) " +
                "VALUES ('w1', 'u1', 'Session', 'MODERATE', 'COMPLETED', 100, 0, 0)",
        )
        db.execSQL(
            "INSERT INTO workout_set (id, workoutId, exerciseId, orderIndex, reps, volume, unit, " +
                "completedAt, createdAt, updatedAt) " +
                "VALUES ('s1', 'w1', 'ex-pushup', 0, 20, 20.0, 'reps', 100, 0, 0)",
        )
    }

    private fun assertClasses(db: SupportSQLiteDatabase) {
        assertEquals(4, intAt(db, "SELECT COUNT(*) FROM class_definition"))
        assertEquals(1, intAt(db, "SELECT startsAwake FROM class_definition WHERE id = 'berserker'"))
        assertEquals(1, intAt(db, "SELECT startsAwake FROM class_definition WHERE id = 'monk'"))
        assertEquals(1, intAt(db, "SELECT startsAwake FROM class_definition WHERE id = 'mage'"))
        assertEquals(0, intAt(db, "SELECT startsAwake FROM class_definition WHERE id = 'wanderer'"))
        assertEquals("t", stringAt(db, "SELECT classTitle FROM class_definition WHERE id = 'monk'"))
        assertEquals(10L, longAt(db, "SELECT createdAt FROM class_definition WHERE id = 'mage'"))
    }

    private fun assertBackfill(db: SupportSQLiteDatabase) {
        assertEquals(4, intAt(db, "SELECT COUNT(*) FROM class_awakening"))
        assertAwakening(db, "u1", "monk", 111)
        assertAwakening(db, "u1", "berserker", 222)
        assertAwakening(db, "u2", "mage", 444)
        assertAwakening(db, "u4", "berserker", 555)
        assertEquals(0, intAt(db, "SELECT COUNT(*) FROM class_awakening WHERE userId = 'u3'"))
        assertEquals(1, intAt(db, "SELECT COUNT(*) FROM class_awakening WHERE userId = 'u4'"))
    }

    private fun assertLedgersUnchanged(db: SupportSQLiteDatabase) {
        assertEquals(240L, longAt(db, "SELECT amount FROM class_xp_transaction WHERE id = 'xp1'"))
        assertEquals(50L, longAt(db, "SELECT createdAt FROM class_xp_transaction WHERE id = 'xp1'"))
        assertEquals("monk", stringAt(db, "SELECT classId FROM class_xp_transaction WHERE id = 'xp1'"))
        assertEquals(40L, longAt(db, "SELECT amount FROM class_proficiency_transaction WHERE id = 'p1'"))
        assertEquals(60L, longAt(db, "SELECT createdAt FROM class_proficiency_transaction WHERE id = 'p1'"))
        assertEquals(
            "BODY_MASTERY",
            stringAt(db, "SELECT proficiencyKey FROM class_proficiency_transaction WHERE id = 'p1'"),
        )
        assertEquals("Session", stringAt(db, "SELECT title FROM workout WHERE id = 'w1'"))
        assertEquals(20.0, doubleAt(db, "SELECT volume FROM workout_set WHERE id = 's1'"), 0.0001)
        assertEquals("monk", stringAt(db, "SELECT primaryClassId FROM player_class WHERE userId = 'u1'"))
        assertEquals("berserker", stringAt(db, "SELECT secondaryClassId FROM player_class WHERE userId = 'u1'"))
        assertDuplicateClassXpRejected(db)
    }

    private fun assertDuplicateClassXpRejected(db: SupportSQLiteDatabase) {
        var rejected = false
        try {
            db.execSQL(
                "INSERT INTO class_xp_transaction (id, userId, classId, amount, transactionType, sourceType, " +
                    "sourceId, rewardType, createdAt) " +
                    "VALUES ('xp2', 'u1', 'monk', 1, 'AWARD', 'WORKOUT_COMPLETION', 'w1', 'CLASS_XP', 99)",
            )
        } catch (expected: android.database.sqlite.SQLiteConstraintException) {
            rejected = true
        }
        assertTrue("class XP exactly-once guard survived the migration", rejected)
    }

    private fun assertAwakening(
        db: SupportSQLiteDatabase,
        userId: String,
        classId: String,
        awakenedAt: Long,
    ) {
        db.query(
            "SELECT awakenedAt, source, evidenceJson FROM class_awakening " +
                "WHERE userId = '$userId' AND classId = '$classId'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(awakenedAt, cursor.getLong(0))
            assertEquals(AwakeningSource.LEGACY_SELECTION.name, cursor.getString(1))
            assertTrue(cursor.isNull(2))
        }
    }

    private fun intAt(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Int =
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getInt(0)
        }

    private fun longAt(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun stringAt(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getString(0)
        }

    private fun doubleAt(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Double =
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getDouble(0)
        }
}
