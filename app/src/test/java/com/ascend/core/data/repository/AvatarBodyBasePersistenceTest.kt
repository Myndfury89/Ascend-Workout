package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.PlayerDao
import com.ascend.core.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Round-trips the cosmetic avatarBodyBase through the real Room schema (v16). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AvatarBodyBasePersistenceTest {
    private lateinit var db: AscendDatabase
    private lateinit var dao: PlayerDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        dao = db.playerDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedProfile() {
        dao.upsertProfile(UserProfileEntity(id = "u1", displayName = "Hunter", createdAt = 0, updatedAt = 0))
    }

    @Test
    fun `a fresh profile has no chosen body base`() =
        runTest {
            seedProfile()
            assertNull(dao.getProfile("u1")!!.avatarBodyBase)
            assertNull(dao.observeAvatarBodyBase("u1").first())
        }

    @Test
    fun `MALE and FEMALE both persist and can be changed later`() =
        runTest {
            seedProfile()

            dao.updateAvatarBodyBase("u1", "MALE", 1L)
            assertEquals("MALE", dao.getProfile("u1")!!.avatarBodyBase)
            assertEquals("MALE", dao.observeAvatarBodyBase("u1").first())

            dao.updateAvatarBodyBase("u1", "FEMALE", 2L)
            assertEquals("FEMALE", dao.getProfile("u1")!!.avatarBodyBase)
            assertEquals("FEMALE", dao.observeAvatarBodyBase("u1").first())
        }
}
