package com.ascend.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ascend.feature.calendar.CalendarScreen
import com.ascend.feature.dashboard.StatusScreen
import com.ascend.feature.progress.ProgressScreen
import com.ascend.feature.quests.ActiveQuestScreen
import com.ascend.feature.quests.QuestsScreen
import com.ascend.feature.workouts.LogWorkoutScreen
import com.ascend.feature.workouts.WorkoutScreen

@Composable
fun AscendApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected =
                        currentDestination?.hierarchy?.any {
                            it.hasRoute(destination.route::class)
                        } == true
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Status,
            modifier = Modifier.padding(padding),
        ) {
            composable<Status> { StatusScreen() }
            composable<Quests> {
                QuestsScreen(onQuestClick = { questId -> navController.navigate(ActiveQuest(questId)) })
            }
            composable<Workout> {
                WorkoutScreen(onLogWorkout = { navController.navigate(LogWorkout) })
            }
            composable<Calendar> { CalendarScreen() }
            composable<Progress> { ProgressScreen() }
            composable<ActiveQuest> {
                ActiveQuestScreen(onBack = { navController.popBackStack() })
            }
            composable<LogWorkout> {
                LogWorkoutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
