package com.ascend.feature.calendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ascend.R
import com.ascend.core.designsystem.component.CenteredPlaceholder

@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    CenteredPlaceholder(
        title = stringResource(R.string.dest_calendar),
        message = stringResource(R.string.calendar_placeholder),
        icon = Icons.Filled.CalendarMonth,
        modifier = modifier,
    )
}
