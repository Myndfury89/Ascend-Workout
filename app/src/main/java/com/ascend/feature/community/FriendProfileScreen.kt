package com.ascend.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.core.domain.community.ReportReason
import com.ascend.core.domain.community.SharedProfile

private val BG = Color(0xFF04050B)
private val PANEL = Color(0xFF10131F)
private val INK = Color(0xFFEAF0FF)
private val MUTED = Color(0xFF8A93B5)
private val ACCENT = Color(0xFF7FB2E6)
private val BAD = Color(0xFFE5736B)

@Composable
fun FriendProfileScreen(
    userId: String,
    onBack: () -> Unit = {},
    viewModel: FriendProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(userId) { viewModel.load(userId) }
    LaunchedEffect(state.closed) { if (state.closed) onBack() }
    FriendProfileContent(
        state = state,
        onBack = onBack,
        onBlock = { viewModel.block(userId) },
        onReport = { viewModel.report(userId, ReportReason.INAPPROPRIATE_NAME, null) },
    )
}

@Composable
fun FriendProfileContent(
    state: FriendProfileUiState,
    onBack: () -> Unit = {},
    onBlock: () -> Unit = {},
    onReport: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(BG)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp)) {
            TextButton(onClick = onBack) { Text("‹ Back", color = MUTED, fontSize = 14.sp) }
            Spacer(Modifier.height(8.dp))
            Text(state.name ?: "Player", color = INK, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            if (state.profile != null) {
                SharedProfileCard(state.profile)
            } else if (!state.loading) {
                Text("This player hasn't shared their build.", color = MUTED, fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onReport, modifier = Modifier.fillMaxWidth()) { Text("Report", color = INK) }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBlock, modifier = Modifier.fillMaxWidth()) { Text("Block", color = BAD) }

            state.message?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = ACCENT, fontSize = 13.sp)
            }
        }
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ACCENT) }
        }
    }
}

@Composable
private fun SharedProfileCard(profile: SharedProfile) {
    Column(Modifier.fillMaxWidth().background(PANEL).padding(16.dp)) {
        profile.selectedClass?.let { Line("Class", it) }
        profile.classLevel?.let { Line("Class level", it.toString()) }
        profile.rank?.let { Line("Rank", it) }
        profile.buildIdentity?.let { Line("Build identity", it) }
        if (profile.topAffinities.isNotEmpty()) {
            Line("Top affinities", profile.topAffinities.joinToString { it.classId })
        }
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
) {
    Spacer(Modifier.height(4.dp))
    Text("$label: $value", color = INK, fontSize = 14.sp)
}
