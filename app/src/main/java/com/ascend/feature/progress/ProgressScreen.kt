package com.ascend.feature.progress

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ascend.R
import com.ascend.core.designsystem.component.CenteredPlaceholder

@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    CenteredPlaceholder(
        title = stringResource(R.string.dest_progress),
        message = stringResource(R.string.progress_placeholder),
        icon = Icons.Filled.TrendingUp,
        modifier = modifier,
    )
}
