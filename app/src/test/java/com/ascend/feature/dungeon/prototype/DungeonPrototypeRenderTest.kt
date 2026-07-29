package com.ascend.feature.dungeon.prototype

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ascend.core.designsystem.theme.AscendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM render smoke test (no emulator) for the fake party Dungeon prototype lobby. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class DungeonPrototypeRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the lobby renders with its simulated-only disclaimer`() {
        compose.setContent { AscendTheme(darkTheme = true) { DungeonPrototypeScreen() } }
        compose.onNodeWithText("Fake Party Dungeon").assertExists()
        compose.onNodeWithText("Simulated only", substring = true).assertExists()
        compose.onNodeWithText("Begin ready check").assertExists()
    }
}
