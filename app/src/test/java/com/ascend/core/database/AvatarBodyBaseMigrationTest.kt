package com.ascend.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MIGRATION_15_16 adds the nullable cosmetic avatarBodyBase column; existing users migrate to null. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AvatarBodyBaseMigrationTest {
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
    fun `existing users migrate with a null avatar body base`() {
        db = openInMemory()
        db.execSQL("CREATE TABLE user_profile (id TEXT PRIMARY KEY, displayName TEXT, updatedAt INTEGER)")
        db.execSQL("INSERT INTO user_profile (id, displayName, updatedAt) VALUES ('u1', 'Hunter', 0)")

        AscendMigrations.MIGRATION_15_16.migrate(db)

        val isNull =
            db.query("SELECT avatarBodyBase FROM user_profile WHERE id = 'u1'").use { c ->
                c.moveToFirst() && c.isNull(0)
            }
        assertTrue("migrated user must have avatarBodyBase = null (never silently MALE)", isNull)
    }

    @Test
    fun `the migrated column accepts a chosen value`() {
        db = openInMemory()
        db.execSQL("CREATE TABLE user_profile (id TEXT PRIMARY KEY, displayName TEXT, updatedAt INTEGER)")
        db.execSQL("INSERT INTO user_profile (id, displayName, updatedAt) VALUES ('u1', 'Hunter', 0)")
        AscendMigrations.MIGRATION_15_16.migrate(db)

        db.execSQL("UPDATE user_profile SET avatarBodyBase = 'FEMALE' WHERE id = 'u1'")

        val value =
            db.query("SELECT avatarBodyBase FROM user_profile WHERE id = 'u1'").use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        assertTrue(value == "FEMALE")
    }
}
