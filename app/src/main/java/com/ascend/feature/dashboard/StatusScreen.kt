package com.ascend.feature.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ascend.R
import com.ascend.core.designsystem.component.CenteredPlaceholder

@Composable
fun StatusScreen(modifier: Modifier = Modifier) {
    CenteredPlaceholder(
        title = stringResource(R.string.dest_status),
        message = stringResource(R.string.status_placeholder),
        icon = Icons.Filled.Dashboard,
        modifier = modifier,
    )
}
