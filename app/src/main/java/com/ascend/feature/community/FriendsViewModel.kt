package com.ascend.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.domain.community.FriendEdge
import com.ascend.core.domain.community.FriendGateway
import com.ascend.core.domain.community.FriendProfileGateway
import com.ascend.core.domain.community.FriendshipId
import com.ascend.core.domain.community.ProfileCard
import com.ascend.core.domain.community.RemoteResult
import com.ascend.core.domain.community.RemoteUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A friend or request row with a resolved name (via friend_cards); avatar/name may be null offline. */
data class FriendListItem(
    val friendshipId: FriendshipId,
    val user: RemoteUserId,
    val handle: String?,
    val displayName: String?,
    val incoming: Boolean,
)

data class FriendsUiState(
    val loading: Boolean = true,
    val friends: List<FriendListItem> = emptyList(),
    val incoming: List<FriendListItem> = emptyList(),
    val outgoing: List<FriendListItem> = emptyList(),
    val lookup: ProfileCard? = null,
    val message: String? = null,
)

/**
 * Friends list, incoming/outgoing requests, and add-by-handle. Reads only remote community data; never
 * touches the local game. Names are resolved via [FriendProfileGateway.friendCards] and merged onto the
 * friendship edges, so pending requesters render with a handle without exposing their class/build.
 */
@HiltViewModel
class FriendsViewModel
    @Inject
    constructor(
        private val friends: FriendGateway,
        private val profiles: FriendProfileGateway,
    ) : ViewModel() {
        private val _state = MutableStateFlow(FriendsUiState())
        val state: StateFlow<FriendsUiState> = _state.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _state.update { it.copy(loading = true) }
                val cards = (profiles.friendCards() as? RemoteResult.Success)?.value.orEmpty().associateBy { it.userId.value }
                val friendEdges = (friends.listFriends() as? RemoteResult.Success)?.value.orEmpty()
                val pending = (friends.listPendingRequests() as? RemoteResult.Success)?.value.orEmpty()
                _state.update {
                    it.copy(
                        loading = false,
                        friends = friendEdges.map { e -> e.toItem(cards) },
                        incoming = pending.filter { e -> e.incoming }.map { e -> e.toItem(cards) },
                        outgoing = pending.filterNot { e -> e.incoming }.map { e -> e.toItem(cards) },
                    )
                }
            }
        }

        fun search(handle: String) {
            viewModelScope.launch {
                when (val r = profiles.lookupByHandle(handle.trim())) {
                    is RemoteResult.Success ->
                        _state.update {
                            it.copy(lookup = r.value, message = if (r.value == null) "No player found with that handle." else null)
                        }
                    RemoteResult.Offline -> _state.update { it.copy(message = "You're offline.") }
                    is RemoteResult.Failure -> _state.update { it.copy(message = r.message ?: "Lookup failed.") }
                }
            }
        }

        fun sendRequest(user: RemoteUserId) = act({ friends.sendRequest(user) }) { it.copy(lookup = null, message = "Request sent.") }

        fun accept(id: FriendshipId) = act(block = { friends.acceptRequest(id) })

        fun decline(id: FriendshipId) = act(block = { friends.declineRequest(id) })

        fun cancel(id: FriendshipId) = act(block = { friends.cancelRequest(id) })

        fun remove(id: FriendshipId) = act(block = { friends.removeFriend(id) })

        fun dismissMessage() = _state.update { it.copy(message = null) }

        private fun act(
            block: suspend () -> RemoteResult<Unit>,
            onSuccess: (FriendsUiState) -> FriendsUiState = { it },
        ) {
            viewModelScope.launch {
                when (val r = block()) {
                    is RemoteResult.Success -> {
                        _state.update(onSuccess)
                        refresh()
                    }
                    RemoteResult.Offline -> _state.update { it.copy(message = "You're offline.") }
                    is RemoteResult.Failure -> _state.update { it.copy(message = r.message ?: "Something went wrong.") }
                }
            }
        }

        private fun FriendEdge.toItem(cards: Map<String, ProfileCard>): FriendListItem {
            val card = cards[other.value]
            return FriendListItem(id, other, card?.handle, card?.displayName, incoming)
        }
    }
