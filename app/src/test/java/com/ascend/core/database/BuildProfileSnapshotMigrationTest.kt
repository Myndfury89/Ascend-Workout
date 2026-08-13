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

/** MIGRATION_16_17 creates the read-only build_profile_snapshot cache. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BuildProfileSnapshotMigrationTest {
    private lateinit var db: SupportSQLiteDatabase

    private fun openInMemory(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
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

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `the snapshot table is created and usable`() {
        db = openInMemory()
        AscendMigrations.MIGRATION_16_17.migrate(db)

        db.execSQL(
            "INSERT INTO build_profile_snapshot (userId, computedAt, windowDays, overallConfidence, payloadJson) " +
                "VALUES ('u1', 100, 28, 0.7, '{}')",
        )
        val windowDays =
            db.query("SELECT windowDays FROM build_profile_snapshot WHERE userId = 'u1'").use { c ->
                if (c.moveToFirst()) c.getInt(0) else -1
            }
        assertEquals(28, windowDays)
    }
}
