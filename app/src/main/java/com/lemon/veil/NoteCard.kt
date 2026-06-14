package com.lemon.veil

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lemon.veil.data.NoteEntity
import com.lemon.veil.ui.theme.WarningYellow

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    onToggle: (NoteEntity) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    previousNote: NoteEntity? = null,
    isSelected: Boolean = false,
) {
    val commitment = buildCommitmentText(note)
    val status = noteTimeStatus(note, previousNote)
    val titleColor = timeStatusColor(status)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commitment,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = titleColor
                )
                if (status == TimeStatus.OUT_OF_ORDER && note.time != null) {
                    Text(
                        text = "⏰ ${formatTime(note.time)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningYellow
                    )
                }
                if (note.hasAlarm && note.time != null) {
                    Text(
                        text = "🔔 ${formatTime(note.time)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Checkbox(checked = note.isCompleted, onCheckedChange = { onToggle(note) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StepCard(
    step: NoteEntity,
    onToggle: (NoteEntity) -> Unit,
    onClick: () -> Unit,
    previousStep: NoteEntity? = null,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
) {
    val status = noteTimeStatus(step, previousStep)
    val titleColor = timeStatusColor(status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 12.dp, top = 3.dp, bottom = 3.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.finalAction ?: step.suggestion,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = titleColor
                )
                step.time?.let {
                    Text(
                        text = formatTime(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status == TimeStatus.OUT_OF_ORDER) WarningYellow
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (step.hasAlarm && step.time != null) {
                    Text(
                        text = "🔔 ${formatTime(step.time)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Checkbox(checked = step.isCompleted, onCheckedChange = { onToggle(step) })
        }
    }
}
