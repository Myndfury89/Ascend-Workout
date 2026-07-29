package com.ascend.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.core.common.WeightUnit

/**
 * A two-position weight-unit selector (segmented control) — never a continuous slider. The
 * selection is conveyed by fill **and** font weight (not colour alone), and the whole control
 * carries a semantic label like "Weight unit, kilograms selected" for screen readers.
 */
@Composable
fun WeightUnitSelector(
    selected: WeightUnit,
    onSelect: (WeightUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.semantics { contentDescription = selected.accessibilityLabel },
    ) {
        Row(Modifier.padding(3.dp)) {
            WeightUnit.entries.forEach { unit ->
                val isSelected = unit == selected
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    onClick = { onSelect(unit) },
                    modifier = Modifier.padding(2.dp),
                ) {
                    Text(
                        text = unit.symbol,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
