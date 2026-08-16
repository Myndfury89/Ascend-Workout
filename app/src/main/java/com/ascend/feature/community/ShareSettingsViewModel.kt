package com.ascend.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.ShareSettings
import com.ascend.core.domain.community.ShareSettingsGateway
import com.ascend.core.domain.community.ShareVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareSettingsUiState(
    val loading: Boolean = true,
    val settings: ShareSettings = ShareSettings.PRIVATE_DEFAULT,
    val message: String? = null,
)

/**
 * The current user's own consent settings. Every change persists immediately via the gateway; the
 * server-side RLS is the actual enforcer, so this screen just expresses intent.
 */
@HiltViewModel
class ShareSettingsViewModel
    @Inject
    constructor(
        private val gateway: ShareSettingsGateway,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ShareSettingsUiState())
        val state: StateFlow<ShareSettingsUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                val settings = (gateway.getOwn() as? RemoteResult.Success)?.value ?: ShareSettings.PRIVATE_DEFAULT
                _state.update { it.copy(loading = false, settings = settings) }
            }
        }

        fun setShareEnabled(enabled: Boolean) =
            persist(state.value.settings.copy(visibility = if (enabled) ShareVisibility.FRIENDS else ShareVisibility.PRIVATE))

        fun setShareBuildIdentity(enabled: Boolean) = persist(state.value.settings.copy(shareBuildIdentity = enabled))

        fun setShareClassProgress(enabled: Boolean) = persist(state.value.settings.copy(shareClassProgress = enabled))

        fun dismissMessage() = _state.update { it.copy(message = null) }

        private fun persist(next: ShareSettings) {
            _state.update { it.copy(settings = next) }
            viewModelScope.launch {
                when (val r = gateway.update(next)) {
                    is RemoteResult.Success -> Unit
                    RemoteResult.Offline -> _state.update { it.copy(message = "Offline — will need to be re-saved.") }
                    is RemoteResult.Failure -> _state.update { it.copy(message = r.message ?: "Couldn't save.") }
                }
            }
        }
    }
