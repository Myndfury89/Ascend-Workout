package com.ascend.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.ascend.R
import kotlinx.serialization.Serializable

// Type-safe navigation routes (Navigation-Compose 2.8+).
@Serializable
data object Status

@Serializable
data object Quests

@Serializable
data object Workout

@Serializable
data object Calendar

@Serializable
data object Progress

// Detail destination pushed on top of the Quests tab.
@Serializable
data class ActiveQuest(val questId: String)

// Log-workout flow pushed on top of the Workout tab.
@Serializable
data object LogWorkout

// Read-only Build Analysis (Build Characteristics + Class Affinity), pushed from the Status tab.
@Serializable
data object BuildAnalysis

// Account: Community identity + sign-in (AuthScreen). Community-only; not required for the core loop.
// Registered only when BuildConfig.COMMUNITY_ENABLED is true (off for the beta).
@Serializable
data object Account

// Community social surfaces, reached from Account when COMMUNITY_ENABLED is true. No deep links.
@Serializable
data object Friends

@Serializable
data class FriendProfile(val userId: String)

@Serializable
data object ShareSettings

/** The five primary destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    STATUS(Status, R.string.dest_status, Icons.Filled.Dashboard),
    QUESTS(Quests, R.string.dest_quests, Icons.Filled.CheckCircle),
    WORKOUT(Workout, R.string.dest_workout, Icons.Filled.FitnessCenter),
    CALENDAR(Calendar, R.string.dest_calendar, Icons.Filled.CalendarMonth),
    PROGRESS(Progress, R.string.dest_progress, Icons.Filled.TrendingUp),
}
