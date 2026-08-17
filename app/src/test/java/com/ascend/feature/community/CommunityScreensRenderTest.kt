package com.ascend.feature.community

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.core.domain.community.FriendshipId
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.ShareSettings
import com.ascend.core.domain.community.ShareVisibility
import com.ascend.core.domain.community.SharedProfile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class CommunityScreensRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `friends screen shows requests and friends`() {
        val state =
            FriendsUiState(
                loading = false,
                friends = listOf(FriendListItem(FriendshipId("f1"), RemoteUserId("u1"), "alpha", "Alpha", incoming = false)),
                incoming = listOf(FriendListItem(FriendshipId("f2"), RemoteUserId("u2"), "bravo", "Bravo", incoming = true)),
            )
        compose.setContent { AscendTheme(darkTheme = true) { FriendsContent(state = state) } }
        compose.onNodeWithText("Alpha").assertIsDisplayed()
        compose.onNodeWithText("Bravo").assertIsDisplayed()
        compose.onNodeWithText("Accept").assertIsDisplayed()
    }

    @Test
    fun `friend profile shows only shared fields and moderation actions`() {
        val state =
            FriendProfileUiState(
                loading = false,
                name = "Alpha",
                profile = SharedProfile(RemoteUserId("u1"), selectedClass = "Mage", buildIdentity = "Mage-leaning"),
            )
        compose.setContent { AscendTheme(darkTheme = true) { FriendProfileContent(state = state) } }
        compose.onNodeWithText("Alpha").assertIsDisplayed()
        compose.onNodeWithText("Class: Mage").assertIsDisplayed()
        compose.onNodeWithText("Block").assertIsDisplayed()
        compose.onNodeWithText("Report").assertIsDisplayed()
    }

    @Test
    fun `share settings reveals sub-toggles only when sharing is on`() {
        val on = ShareSettingsUiState(loading = false, settings = ShareSettings(ShareVisibility.FRIENDS))
        compose.setContent { AscendTheme(darkTheme = true) { ShareSettingsContent(state = on) } }
        compose.onNodeWithText("Share with friends").assertIsDisplayed()
        compose.onNodeWithText("Build identity & affinities").assertIsDisplayed()
    }

    @Test
    fun `share settings hides sub-toggles when private`() {
        val off = ShareSettingsUiState(loading = false, settings = ShareSettings(ShareVisibility.PRIVATE))
        compose.setContent { AscendTheme(darkTheme = true) { ShareSettingsContent(state = off) } }
        compose.onNodeWithText("Share with friends").assertIsDisplayed()
        // The sub-toggle must be absent while private (nothing is shared until sharing is on).
        compose.onNodeWithText("Build identity & affinities").assertDoesNotExist()
    }
}
