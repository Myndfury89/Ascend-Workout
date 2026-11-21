package com.ascend.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ascend.core.common.CommunityAccess
import com.ascend.core.common.CommunityFeatures
import com.ascend.feature.community.AuthScreen
import com.ascend.feature.community.FriendProfileScreen
import com.ascend.feature.community.FriendsScreen
import com.ascend.feature.community.ShareSettingsScreen

/**
 * Type-safe Community destinations, reached from Status → Account when the feature is enabled.
 * There are no Community deep links or intent filters in the manifest.
 */
val COMMUNITY_ROUTE_CLASSES =
    listOf(
        Account::class,
        Friends::class,
        FriendProfile::class,
        ShareSettings::class,
    )

fun communityRouteKeys(communityEnabled: Boolean): List<String> =
    if (CommunityAccess.registerCommunityRoutes(communityEnabled)) {
        COMMUNITY_ROUTE_CLASSES.map { it.qualifiedName ?: it.simpleName.orEmpty() }
    } else {
        emptyList()
    }

/**
 * Registers Community composables only when [communityEnabled] is true. The implementations stay
 * in the tree so a later release can flip the flag without restoring deleted code.
 */
fun NavGraphBuilder.communityDestinations(
    navController: NavHostController,
    communityEnabled: Boolean = CommunityFeatures.isEnabled,
) {
    if (!CommunityAccess.registerCommunityRoutes(communityEnabled)) return

    composable<Account> {
        AuthScreen(
            onBack = { navController.popBackStack() },
            onOpenFriends = { navController.navigate(Friends) },
            onOpenShareSettings = { navController.navigate(ShareSettings) },
        )
    }
    composable<Friends> {
        FriendsScreen(
            onBack = { navController.popBackStack() },
            onOpenProfile = { userId -> navController.navigate(FriendProfile(userId)) },
        )
    }
    composable<FriendProfile> { entry ->
        FriendProfileScreen(
            userId = entry.toRoute<FriendProfile>().userId,
            onBack = { navController.popBackStack() },
        )
    }
    composable<ShareSettings> {
        ShareSettingsScreen(onBack = { navController.popBackStack() })
    }
}
