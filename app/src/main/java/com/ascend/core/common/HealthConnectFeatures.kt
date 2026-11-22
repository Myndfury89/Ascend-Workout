package com.ascend.core.common

import com.ascend.BuildConfig

/**
 * Compile-time switch for the onboarding "Connect step history" suggestion.
 * Off hides that suggestion only. It does not enable Health Connect integration,
 * permissions, or sync.
 */
object HealthConnectFeatures {
    val isOnboardingSuggestionEnabled: Boolean
        get() = BuildConfig.HEALTH_CONNECT_ONBOARDING_SUGGESTION_ENABLED
}
