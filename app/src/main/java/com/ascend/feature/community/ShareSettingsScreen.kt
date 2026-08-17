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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.core.domain.community.ShareVisibility

private val BG = Color(0xFF04050B)
private val INK = Color(0xFFEAF0FF)
private val MUTED = Color(0xFF8A93B5)

@Composable
fun ShareSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: ShareSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShareSettingsContent(
        state = state,
        onBack = onBack,
        onShareEnabled = viewModel::setShareEnabled,
        onShareBuildIdentity = viewModel::setShareBuildIdentity,
        onShareClassProgress = viewModel::setShareClassProgress,
    )
}

@Composable
fun ShareSettingsContent(
    state: ShareSettingsUiState,
    onBack: () -> Unit = {},
    onShareEnabled: (Boolean) -> Unit = {},
    onShareBuildIdentity: (Boolean) -> Unit = {},
    onShareClassProgress: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sharing = state.settings.visibility == ShareVisibility.FRIENDS
    Column(modifier.fillMaxSize().background(BG).padding(horizontal = 20.dp, vertical = 20.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back", color = MUTED, fontSize = 14.sp) }
        Spacer(Modifier.height(8.dp))
        Text("SHARING", color = MUTED, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(4.dp))
        Text("What friends can see", color = INK, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Private by default. Nothing is shared until you turn this on.", color = MUTED, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))

        ToggleRow("Share with friends", sharing, onShareEnabled)
        if (sharing) {
            ToggleRow("Class, level & rank", state.settings.shareClassProgress, onShareClassProgress)
            ToggleRow("Build identity & affinities", state.settings.shareBuildIdentity, onShareBuildIdentity)
        }

        state.message?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = MUTED, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = INK, fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
