package com.ascend.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MIGRATION_14_15 re-points every persisted "magician" class reference to "mage", preserving data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class MageRenameMigrationTest {
    private lateinit var db: SupportSQLiteDatabase

    private fun openInMemory(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null) // in-memory
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    private fun queryOne(sql: String): String? =
        db.query(sql).use { c -> if (c.moveToFirst()) c.getString(0) else null }

    private fun count(sql: String): Int = db.query(sql).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `magician rows are repointed to mage across every class table`() {
        db = openInMemory()
        db.execSQL("CREATE TABLE class_definition (id TEXT PRIMARY KEY, statusThemeKey TEXT, name TEXT)")
        db.execSQL("CREATE TABLE player_class (userId TEXT PRIMARY KEY, primaryClassId TEXT, secondaryClassId TEXT)")
        db.execSQL("CREATE TABLE class_history (id TEXT PRIMARY KEY, classId TEXT)")
        db.execSQL("CREATE TABLE class_xp_transaction (id TEXT PRIMARY KEY, classId TEXT)")
        db.execSQL("CREATE TABLE class_proficiency_transaction (id TEXT PRIMARY KEY, classId TEXT)")
        db.execSQL("INSERT INTO class_definition VALUES ('magician', 'magician', 'Magician')")
        db.execSQL("INSERT INTO player_class VALUES ('u1', 'magician', 'magician')")
        db.execSQL("INSERT INTO class_history VALUES ('h1', 'magician')")
        db.execSQL("INSERT INTO class_xp_transaction VALUES ('x1', 'magician')")
        db.execSQL("INSERT INTO class_proficiency_transaction VALUES ('p1', 'magician')")

        AscendMigrations.MIGRATION_14_15.migrate(db)

        assertEquals("mage", queryOne("SELECT id FROM class_definition"))
        assertEquals("mage", queryOne("SELECT statusThemeKey FROM class_definition"))
        assertEquals("Mage", queryOne("SELECT name FROM class_definition"))
        assertEquals("mage", queryOne("SELECT primaryClassId FROM player_class"))
        assertEquals("mage", queryOne("SELECT secondaryClassId FROM player_class"))
        assertEquals("mage", queryOne("SELECT classId FROM class_history"))
        assertEquals("mage", queryOne("SELECT classId FROM class_xp_transaction"))
        assertEquals("mage", queryOne("SELECT classId FROM class_proficiency_transaction"))
        // The row/data survived the rename (not deleted).
        assertEquals(1, count("SELECT COUNT(*) FROM class_xp_transaction WHERE id = 'x1'"))
        // No lingering magician anywhere.
        assertEquals(0, count("SELECT COUNT(*) FROM class_definition WHERE id = 'magician'"))
    }

    @Test
    fun `a non-magician selection is untouched`() {
        db = openInMemory()
        db.execSQL("CREATE TABLE player_class (userId TEXT PRIMARY KEY, primaryClassId TEXT, secondaryClassId TEXT)")
        db.execSQL("CREATE TABLE class_definition (id TEXT PRIMARY KEY, statusThemeKey TEXT, name TEXT)")
        db.execSQL("CREATE TABLE class_history (id TEXT PRIMARY KEY, classId TEXT)")
        db.execSQL("CREATE TABLE class_xp_transaction (id TEXT PRIMARY KEY, classId TEXT)")
        db.execSQL("CREATE TABLE class_proficiency_transaction (id TEXT PRIMARY KEY, classId TEXT)")
        db.execSQL("INSERT INTO player_class VALUES ('u1', 'monk', null)")

        AscendMigrations.MIGRATION_14_15.migrate(db)

        assertEquals("monk", queryOne("SELECT primaryClassId FROM player_class"))
    }
}
