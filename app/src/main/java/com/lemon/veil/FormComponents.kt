package com.lemon.veil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun LabelRow(
    label: String,
    modifier: Modifier = Modifier,
    labelWidth: Int = 40,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidth.dp)
        )
        content()
    }
}

@Composable
fun TimeRow(time: Long?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LabelRow(stringResource(R.string.label_time), modifier) {
        val timeText = if (time != null) formatTime(time) else stringResource(R.string.hint_set_time)
        Box(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable { onClick() },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    placeholder: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val borderWidth = if (isFocused) 1.5.dp else 0.5.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = singleLine,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            placeholder, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun NoteEditCard(
    commitment: String,
    isEditing: Boolean,
    onSave: () -> Unit,
    onStartEdit: () -> Unit,
    action: String,
    onActionChange: (String) -> Unit,
    showActionError: Boolean,
    time: Long?,
    timeStatus: TimeStatus,
    onTimeClick: () -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = commitment.ifEmpty {
                        if (isEditing) stringResource(R.string.hint_enter_action)
                        else stringResource(R.string.hint_no_commitment)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { if (isEditing) onSave() else onStartEdit() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) stringResource(R.string.cd_save)
                        else stringResource(R.string.cd_edit),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    LabelRow(stringResource(R.string.label_action)) {
                        if (isEditing) {
                            CompactTextField(
                                value = action,
                                onValueChange = {
                                    onActionChange(it)
                                }
                            )
                        } else {
                            Text(
                                text = action,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (showActionError) {
                        Text(
                            stringResource(R.string.error_action_empty),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 44.dp, top = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (isEditing) {
                TimeRow(time, onClick = onTimeClick)
            } else {
                val timeColor = timeStatusColor(timeStatus, MaterialTheme.colorScheme.primary)
                LabelRow(stringResource(R.string.label_time)) {
                    if (time != null) {
                        Text(
                            text = formatTime(time),
                            style = MaterialTheme.typography.bodyMedium,
                            color = timeColor
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.hint_not_set),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            LabelRow(stringResource(R.string.label_location)) {
                if (isEditing) {
                    CompactTextField(value = location, onValueChange = onLocationChange)
                } else {
                    if (location.isNotEmpty()) {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.hint_not_set),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmRow(hasAlarm: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.label_alarm), style = MaterialTheme.typography.bodySmall)
        Switch(checked = hasAlarm, onCheckedChange = onToggle, enabled = enabled, modifier = Modifier.scale(0.75f))
    }
}

@Composable
fun CalendarSyncRow(isSynced: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.label_sync_calendar), style = MaterialTheme.typography.bodySmall)
        Switch(checked = isSynced, onCheckedChange = onToggle, enabled = enabled, modifier = Modifier.scale(0.75f))
    }
}
