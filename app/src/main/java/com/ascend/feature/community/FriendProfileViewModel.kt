package com.ascend.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.domain.community.FriendProfileGateway
import com.ascend.core.domain.community.ModerationGateway
import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.RemoteUserId
import com.ascend.core.domain.community.ReportReason
import com.ascend.core.domain.community.SharedProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendProfileUiState(
    val loading: Boolean = true,
    val name: String? = null,
    val profile: SharedProfile? = null,
    val message: String? = null,
    /** Set after a block so the screen can pop back — a blocked user is no longer viewable. */
    val closed: Boolean = false,
)

/**
 * Views a friend's consented [SharedProfile] (class/build only if they shared it) and hosts the
 * moderation actions (block, report). Read-only w.r.t. the local game. The name comes from
 * friend_cards; the class/build fields come from the RLS-gated snapshot (which already reflects consent).
 */
@HiltViewModel
class FriendProfileViewModel
    @Inject
    constructor(
        private val profiles: FriendProfileGateway,
        private val moderation: ModerationGateway,
    ) : ViewModel() {
        private val _state = MutableStateFlow(FriendProfileUiState())
        val state: StateFlow<FriendProfileUiState> = _state.asStateFlow()

        fun load(userId: String) {
            val user = RemoteUserId(userId)
            viewModelScope.launch {
                _state.update { it.copy(loading = true) }
                val name =
                    (profiles.friendCards() as? RemoteResult.Success)?.value
                        ?.firstOrNull { it.userId == user }
                        ?.let { it.displayName ?: it.handle }
                val profile = (profiles.fetchSharedProfile(user) as? RemoteResult.Success)?.value
                _state.update { it.copy(loading = false, name = name, profile = profile) }
            }
        }

        fun block(userId: String) {
            viewModelScope.launch {
                when (val r = moderation.block(RemoteUserId(userId))) {
                    is RemoteResult.Success -> _state.update { it.copy(closed = true) }
                    RemoteResult.Offline -> _state.update { it.copy(message = "You're offline.") }
                    is RemoteResult.Failure -> _state.update { it.copy(message = r.message ?: "Block failed.") }
                }
            }
        }

        fun report(
            userId: String,
            reason: ReportReason,
            note: String?,
        ) {
            viewModelScope.launch {
                when (val r = moderation.report(RemoteUserId(userId), reason, note)) {
                    is RemoteResult.Success -> _state.update { it.copy(message = "Report submitted for review.") }
                    RemoteResult.Offline -> _state.update { it.copy(message = "You're offline.") }
                    is RemoteResult.Failure -> _state.update { it.copy(message = r.message ?: "Report failed.") }
                }
            }
        }

        fun dismissMessage() = _state.update { it.copy(message = null) }
    }
