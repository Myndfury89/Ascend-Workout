package com.ascend.feature.dashboard

import app.cash.turbine.test
import com.ascend.core.designsystem.motion.MotionPreferences
import com.ascend.core.designsystem.motion.ReducedMotionController
import com.ascend.core.designsystem.motion.SystemMotionSettings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StatusMotionReducedMotionTest {
    private class FakePrefs : MotionPreferences {
        val stored = MutableStateFlow(false)

        override fun observeInAppReducedMotion(): Flow<Boolean> = stored

        override suspend fun setInAppReducedMotion(value: Boolean) {
            stored.value = value
        }
    }

    private val system = MutableStateFlow(false)
    private val prefs = FakePrefs()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): StatusMotionViewModel {
        val dataSource =
            mockk<StatusDataSource>(relaxed = true) {
                every { hunterName } returns "Test"
                coEvery { userId() } returns "u1"
                every { observeProgress() } returns emptyFlow()
                every { observeStats() } returns emptyFlow()
            }
        return StatusMotionViewModel(
            dataSource = dataSource,
            simulator = mockk(relaxed = true),
            eventRepository = mockk(relaxed = true) { every { observePending(any()) } returns emptyFlow() },
            levelCalculator = mockk(relaxed = true),
            classRepository = mockk(relaxed = true) { every { observeSelection(any()) } returns emptyFlow() },
            reducedMotionController =
                ReducedMotionController(
                    systemSettings =
                        object : SystemMotionSettings {
                            override fun observeSystemReducedMotion(): Flow<Boolean> = system
                        },
                    preferences = prefs,
                ),
        )
    }

    @Test
    fun `system animator scale zero reduces motion without the in-app toggle`() =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                skipItems(1)
                system.value = true
                val state = expectMostRecentItem()
                assertEquals(true, state.reducedMotion)
                assertEquals(true, state.systemReducedMotion)
                assertEquals(false, state.inAppReducedMotion)
            }
        }

    @Test
    fun `toggle writes through to persisted preferences`() =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                skipItems(1)
                vm.setReducedMotion(true)
                assertEquals(true, prefs.stored.value)
                val state = expectMostRecentItem()
                assertEquals(true, state.reducedMotion)
                assertEquals(true, state.inAppReducedMotion)
                vm.setReducedMotion(false)
                assertEquals(false, expectMostRecentItem().reducedMotion)
            }
        }

    @Test
    fun `restored preference is applied on start`() =
        runTest {
            prefs.stored.value = true
            val vm = viewModel()
            vm.uiState.test {
                assertEquals(true, expectMostRecentItem().reducedMotion)
            }
        }
}
