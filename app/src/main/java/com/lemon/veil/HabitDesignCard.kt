package com.lemon.veil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@Composable
fun HabitDesignCard(
    cue: String, craving: String, responsePlan: String, reward: String,
    badCue: String, badCraving: String, badResponsePlan: String, badReward: String,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onFieldChanged: (field: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.tab_build_habit),
        stringResource(R.string.tab_break_habit),
    )

    val buildItems = listOf(
        Triple("cue", stringResource(R.string.label_cue), stringResource(R.string.hint_cue)),
        Triple("craving", stringResource(R.string.label_craving), stringResource(R.string.hint_craving)),
        Triple("responsePlan", stringResource(R.string.label_response), stringResource(R.string.hint_response)),
        Triple("reward", stringResource(R.string.label_reward), stringResource(R.string.hint_reward)),
    )
    val breakItems = listOf(
        Triple("badCue", stringResource(R.string.label_cue), stringResource(R.string.hint_bad_cue)),
        Triple("badCraving", stringResource(R.string.label_craving), stringResource(R.string.hint_bad_craving)),
        Triple("badResponsePlan", stringResource(R.string.label_response), stringResource(R.string.hint_bad_response)),
        Triple("badReward", stringResource(R.string.label_reward), stringResource(R.string.hint_bad_reward)),
    )
    val buildPlaceholders = listOf(
        stringResource(R.string.placeholder_cue),
        stringResource(R.string.placeholder_craving),
        stringResource(R.string.placeholder_response),
        stringResource(R.string.placeholder_reward),
    )
    val breakPlaceholders = listOf(
        stringResource(R.string.placeholder_bad_cue),
        stringResource(R.string.placeholder_bad_craving),
        stringResource(R.string.placeholder_bad_response),
        stringResource(R.string.placeholder_bad_reward),
    )

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.section_habit_laws),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                tabs.forEachIndexed { index, title ->
                    FilterChip(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        label = { Text(title, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = pagerState.currentPage == index,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.height(28.dp),
                    )
                    if (index < tabs.lastIndex) Spacer(Modifier.width(6.dp))
                }
                IconButton(onClick = onToggleEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) stringResource(R.string.cd_save) else stringResource(R.string.cd_edit),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(vertical = 4.dp),
            ) { page ->
                val items = if (page == 0) buildItems else breakItems
                val values = if (page == 0) listOf(cue, craving, responsePlan, reward)
                else listOf(badCue, badCraving, badResponsePlan, badReward)
                val placeholders = if (page == 0) buildPlaceholders else breakPlaceholders

                Column {
                    items.forEachIndexed { i, (field, label, hint) ->
                        HabitDesignItem(
                            label = label,
                            hint = hint,
                            placeholder = placeholders[i],
                            value = values[i],
                            isEditing = isEditing,
                            onValueChange = { onFieldChanged(field, it) },
                        )
                        if (i < items.lastIndex) {
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitDesignItem(
    label: String,
    hint: String,
    placeholder: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
) {
    var localValue by remember(value) { mutableStateOf(value) }
    var chipWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val indent = with(density) { chipWidthPx.toDp() + 8.dp }

    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.onSizeChanged { chipWidthPx = it.width },
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(4.dp))

        if (isEditing) {
            CompactTextField(
                value = localValue,
                onValueChange = { localValue = it; onValueChange(it) },
                modifier = Modifier.padding(start = indent),
                placeholder = placeholder,
            )
        } else {
            Text(
                text = value.ifEmpty { placeholder },
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = indent),
            )
        }
    }
}
