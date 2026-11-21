package com.ascend.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ascend.core.designsystem.theme.AscendTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class CommunityNavigationFlagTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `community route keys are empty when the flag is off`() {
        assertTrue(communityRouteKeys(communityEnabled = false).isEmpty())
    }

    @Test
    fun `community route keys include every social surface when the flag is on`() {
        val keys = communityRouteKeys(communityEnabled = true)
        assertTrue(keys.any { it.endsWith("Account") })
        assertTrue(keys.any { it.endsWith("Friends") })
        assertTrue(keys.any { it.endsWith("FriendProfile") })
        assertTrue(keys.any { it.endsWith("ShareSettings") })
    }

    @Test
    fun `community destinations are absent from the graph when the flag is off`() {
        lateinit var navController: NavHostController
        compose.setContent {
            navController = rememberNavController()
            AscendTheme(darkTheme = true) {
                NavHost(navController = navController, startDestination = Status) {
                    composable<Status> { Text("status") }
                    communityDestinations(navController, communityEnabled = false)
                }
            }
        }
        compose.runOnIdle {
            val routes = navController.graph.mapNotNull { it.route }
            assertFalse(routes.any { it.contains("Account") })
            assertFalse(routes.any { it.contains("Friends") })
            assertFalse(routes.any { it.contains("FriendProfile") })
            assertFalse(routes.any { it.contains("ShareSettings") })
        }
    }

    @Test
    fun `community destinations are registered when the flag is on`() {
        lateinit var navController: NavHostController
        compose.setContent {
            navController = rememberNavController()
            AscendTheme(darkTheme = true) {
                NavHost(navController = navController, startDestination = Status) {
                    composable<Status> { Text("status") }
                    communityDestinations(navController, communityEnabled = true)
                }
            }
        }
        compose.runOnIdle {
            val routes = navController.graph.mapNotNull { it.route }
            assertTrue(routes.any { it.contains("Account") })
            assertTrue(routes.any { it.contains("Friends") })
            assertTrue(routes.any { it.contains("FriendProfile") })
            assertTrue(routes.any { it.contains("ShareSettings") })
        }
    }

    @Test
    fun `navigating to Account is rejected when Community is disabled`() {
        lateinit var navController: NavHostController
        compose.setContent {
            navController = rememberNavController()
            AscendTheme(darkTheme = true) {
                NavHost(navController = navController, startDestination = Status) {
                    composable<Status> { Text("status") }
                    communityDestinations(navController, communityEnabled = false)
                }
            }
        }
        compose.runOnIdle {
            try {
                navController.navigate(Account)
                fail("Expected navigation to Account to be rejected when Community is disabled")
            } catch (e: IllegalArgumentException) {
                // Route is unregistered; the Status > Account entry point cannot be reached.
            }
        }
    }
}
