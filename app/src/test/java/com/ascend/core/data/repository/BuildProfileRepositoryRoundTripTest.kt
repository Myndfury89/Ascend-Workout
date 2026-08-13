package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.domain.build.BuildAffinityCalculator
import com.ascend.core.domain.build.BuildCharacteristic
import com.ascend.core.domain.build.BuildProfile
import com.ascend.core.domain.build.BuildProfileSnapshot
import com.ascend.core.domain.build.BuildTrend
import com.ascend.core.domain.build.CharacteristicScore
import com.ascend.core.domain.build.EvidenceState
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

/** Round-trips a computed Build snapshot through Room (schema v17) and back to domain. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BuildProfileRepositoryRoundTripTest {
    private lateinit var db: AscendDatabase
    private lateinit var repo: BuildProfileRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        repo = BuildProfileRepositoryImpl(db.buildProfileDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleSnapshot(): BuildProfileSnapshot {
        val profile =
            BuildProfile(
                scores =
                    mapOf(
                        BuildCharacteristic.STRENGTH to
                            CharacteristicScore(
                                BuildCharacteristic.STRENGTH,
                                85.0,
                                EvidenceState.OK,
                                BuildTrend.DEVELOPING,
                                0.9,
                            ),
                        BuildCharacteristic.DISTANCE to
                            CharacteristicScore(
                                BuildCharacteristic.DISTANCE,
                                0.0,
                                EvidenceState.ZERO,
                                BuildTrend.UNKNOWN,
                                0.8,
                            ),
                    ),
                overallConfidence = 0.83,
                windowDays = 28,
            )
        return BuildProfileSnapshot(computedAt = 12_345L, profile = profile, affinities = BuildAffinityCalculator().calculate(profile))
    }

    @Test
    fun `no snapshot yet returns null`() =
        runTest {
            assertNull(repo.latest("u1"))
        }

    @Test
    fun `a saved snapshot round-trips through the JSON payload`() =
        runTest {
            val original = sampleSnapshot()
            repo.save("u1", original)

            val restored = repo.latest("u1")!!
            assertEquals(original.computedAt, restored.computedAt)
            assertEquals(original.profile.windowDays, restored.profile.windowDays)
            assertEquals(original.profile.overallConfidence, restored.profile.overallConfidence, 0.0001)
            assertEquals(
                original.profile[BuildCharacteristic.STRENGTH]!!.state,
                restored.profile[BuildCharacteristic.STRENGTH]!!.state,
            )
            assertEquals(original.affinities.ranked.size, restored.affinities.ranked.size)
            assertEquals(original.affinities.dominant?.buildClass, restored.affinities.dominant?.buildClass)
        }

    @Test
    fun `observe emits the latest saved snapshot`() =
        runTest {
            repo.save("u1", sampleSnapshot())
            val emitted = repo.observe("u1").first()
            assertEquals(12_345L, emitted!!.computedAt)
        }
}
