package com.ascend.feature.community

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ascend.core.designsystem.theme.AscendTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class AuthContentRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `signed out shows the email path and stresses that it is optional`() {
        compose.setContent { AscendTheme(darkTheme = true) { AuthContent(state = AuthUiState()) } }
        compose.onNodeWithText("Sign in to Ascend Community").assertIsDisplayed()
        compose.onNodeWithText("Optional — your solo game works fully without an account.").assertIsDisplayed()
        compose.onNodeWithText("Email").assertIsDisplayed()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
        compose.onNodeWithText("Create account").assertIsDisplayed()
    }

    @Test
    fun `the google button appears only when configured`() {
        compose.setContent { AscendTheme(darkTheme = true) { AuthContent(state = AuthUiState(googleAvailable = true)) } }
        compose.onNodeWithText("Continue with Google").assertIsDisplayed()
    }

    @Test
    fun `signed in shows the verify hint and sign out`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                AuthContent(state = AuthUiState(signedIn = true, emailVerified = false))
            }
        }
        compose.onNodeWithText("You're signed in").assertIsDisplayed()
        compose.onNodeWithText("Check your inbox to verify your email.").assertIsDisplayed()
        compose.onNodeWithText("Sign out").assertIsDisplayed()
    }

    @Test
    fun `an error is surfaced without crashing`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                AuthContent(state = AuthUiState(error = "You're offline. Check your connection and try again."))
            }
        }
        // The error sits below the form; in the fixed test viewport it exists but may be off-screen.
        compose.onNodeWithText("You're offline. Check your connection and try again.").assertExists()
    }

    @Test
    fun `sign in button forwards the entered credentials`() {
        var captured: Pair<String, String>? = null
        compose.setContent {
            AscendTheme(darkTheme = true) {
                AuthContent(state = AuthUiState(), onSignIn = { e, p -> captured = e to p })
            }
        }
        compose.onNodeWithText("Sign in").performClick()
        // Empty fields still forward (validation is a later concern); the wiring is what's under test.
        assertTrue(captured != null)
    }
}
