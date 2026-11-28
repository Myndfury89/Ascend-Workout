package com.ascend.core.common

import com.ascend.BuildConfig

/**
 * Build-time feature flags. Community is officially excluded from the initial Ascend beta (Play
 * internal testing), so [isEnabled] is false for debug and release unless a build overrides
 * [BuildConfig.COMMUNITY_ENABLED].
 *
 * The Community implementation stays in the tree and is re-enabled by flipping that field on the
 * desired build type in `app/build.gradle.kts`, or by passing `-Pcommunity.enabled=true`. Do not
 * delete Community code to hide it.
 *
 * Account sign-in (`AuthScreen`) is Community-only: it links a remote identity for friends, share
 * settings, and discovery, and does not gate workouts, quests, XP, class progression, or Status.
 * Those features use the local profile ([LOCAL_USER_ID]) and keep working with Community off.
 */
object CommunityFeatures {
    val isEnabled: Boolean
        get() = BuildConfig.COMMUNITY_ENABLED
}

/**
 * Shared predicates so UI, navigation, and Hilt wiring cannot drift, and so unit tests can exercise
 * both flag states without rebuilding [BuildConfig].
 */
object CommunityAccess {
    fun accountEntryVisible(communityEnabled: Boolean): Boolean = communityEnabled

    fun registerCommunityRoutes(communityEnabled: Boolean): Boolean = communityEnabled

    /**
     * The live Supabase client is created only when Community is on *and* backend config is present.
     * A disabled flag wins even if `SUPABASE_URL` / `SUPABASE_ANON_KEY` are baked into the build,
     * so internal beta APKs never talk to the Community backend.
     */
    fun shouldCreateBackendClient(
        communityEnabled: Boolean,
        supabaseUrl: String,
        supabaseKey: String,
    ): Boolean = communityEnabled && supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()

    fun shouldStartAuthLinkCoordinator(communityEnabled: Boolean): Boolean = communityEnabled
}
