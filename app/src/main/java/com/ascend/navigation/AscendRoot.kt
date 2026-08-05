package com.ascend.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.domain.repository.OnboardingRepository
import com.ascend.feature.onboarding.OnboardingScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** First-launch routing decision. */
enum class RootRoute { LOADING, ONBOARDING, MAIN }

/**
 * Decides the app's entry destination from onboarding completion: an incomplete/absent profile shows
 * onboarding; a completed profile shows the main shell. Completing onboarding flips
 * `onboardingCompleted`, which this flow observes to route straight to the production Status shell.
 * Debug prototype launchers are separate activities and are unaffected.
 */
@HiltViewModel
class RootGateViewModel
    @Inject
    constructor(
        onboardingRepository: OnboardingRepository,
    ) : ViewModel() {
        val route: StateFlow<RootRoute> =
            onboardingRepository
                .observeCompleted(LOCAL_USER_ID)
                .map { if (it) RootRoute.MAIN else RootRoute.ONBOARDING }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootRoute.LOADING)
    }

@Composable
fun AscendRoot(viewModel: RootGateViewModel = hiltViewModel()) {
    val route by viewModel.route.collectAsStateWithLifecycle()
    when (route) {
        RootRoute.LOADING ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        RootRoute.ONBOARDING -> OnboardingScreen()
        RootRoute.MAIN -> AscendApp()
    }
}
