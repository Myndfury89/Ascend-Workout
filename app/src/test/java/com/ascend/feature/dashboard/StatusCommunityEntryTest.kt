package com.ascend.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ascend.core.designsystem.theme.AscendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class StatusCommunityEntryTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `account button is hidden when Community is disabled`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                StatusUtilityActions(
                    communityEnabled = false,
                    onViewBuild = {},
                    onAccount = {},
                )
            }
        }
        compose.onNodeWithText("View your build").assertIsDisplayed()
        compose.onNodeWithText("Account").assertDoesNotExist()
    }

    @Test
    fun `account button is shown when Community is enabled`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                StatusUtilityActions(
                    communityEnabled = true,
                    onViewBuild = {},
                    onAccount = {},
                )
            }
        }
        compose.onNodeWithText("View your build").assertIsDisplayed()
        compose.onNodeWithText("Account").assertIsDisplayed()
    }
}
