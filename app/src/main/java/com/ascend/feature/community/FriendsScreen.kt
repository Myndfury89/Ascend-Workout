package com.ascend.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.core.domain.community.RemoteUserId

// Friends list, incoming/outgoing requests, and add-by-handle. Self-contained styling.
private val BG = Color(0xFF04050B)
private val INK = Color(0xFFEAF0FF)
private val MUTED = Color(0xFF8A93B5)
private val ACCENT = Color(0xFF7FB2E6)

@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FriendsContent(
        state = state,
        onBack = onBack,
        onSearch = viewModel::search,
        onSendRequest = { viewModel.sendRequest(RemoteUserId(it)) },
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        onCancel = viewModel::cancel,
        onRemove = viewModel::remove,
        onOpenProfile = onOpenProfile,
    )
}

@Composable
fun FriendsContent(
    state: FriendsUiState,
    onBack: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onSendRequest: (String) -> Unit = {},
    onAccept: (com.ascend.core.domain.community.FriendshipId) -> Unit = {},
    onDecline: (com.ascend.core.domain.community.FriendshipId) -> Unit = {},
    onCancel: (com.ascend.core.domain.community.FriendshipId) -> Unit = {},
    onRemove: (com.ascend.core.domain.community.FriendshipId) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().background(BG).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        TextButton(onClick = onBack) { Text("‹ Back", color = MUTED, fontSize = 14.sp) }
        Spacer(Modifier.height(8.dp))
        Text("FRIENDS", color = MUTED, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(16.dp))

        AddByHandle(state, onSearch, onSendRequest)

        state.message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = ACCENT, fontSize = 13.sp)
        }

        Section("Requests") {
            if (state.incoming.isEmpty() && state.outgoing.isEmpty()) {
                Empty("No pending requests.")
            } else {
                state.incoming.forEach { item ->
                    Row2(item.label()) {
                        Button(onClick = { onAccept(item.friendshipId) }) { Text("Accept") }
                        OutlinedButton(onClick = { onDecline(item.friendshipId) }) { Text("Decline", color = INK) }
                    }
                }
                state.outgoing.forEach { item ->
                    Row2("${item.label()} · pending") {
                        OutlinedButton(onClick = { onCancel(item.friendshipId) }) { Text("Cancel", color = INK) }
                    }
                }
            }
        }

        Section("Your friends") {
            if (state.friends.isEmpty()) {
                Empty("No friends yet.")
            } else {
                state.friends.forEach { item ->
                    Row2(item.label()) {
                        TextButton(onClick = { onOpenProfile(item.user.value) }) { Text("View", color = ACCENT) }
                        OutlinedButton(onClick = { onRemove(item.friendshipId) }) { Text("Remove", color = INK) }
                    }
                }
            }
        }
    }
}

private fun FriendListItem.label(): String = displayName ?: handle ?: "Player"

@Composable
private fun AddByHandle(
    state: FriendsUiState,
    onSearch: (String) -> Unit,
    onSendRequest: (String) -> Unit,
) {
    var handle by remember { mutableStateOf("") }
    OutlinedTextField(
        value = handle,
        onValueChange = { handle = it },
        label = { Text("Add by handle") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { onSearch(handle) }, modifier = Modifier.fillMaxWidth()) { Text("Search", color = INK) }
    state.lookup?.let { card ->
        Spacer(Modifier.height(10.dp))
        Row2(card.displayName ?: card.handle ?: "Player") {
            Button(onClick = { onSendRequest(card.userId.value) }) { Text("Send request") }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(22.dp))
    Text(title.uppercase(), color = MUTED, fontSize = 11.sp, letterSpacing = 2.sp)
    Spacer(Modifier.height(8.dp))
    content()
}

@Composable
private fun Empty(text: String) {
    Text(text, color = MUTED, fontSize = 13.sp)
}

@Composable
private fun Row2(
    label: String,
    actions: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = INK, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
    }
}
