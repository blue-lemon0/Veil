package com.lemon.veil

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lemon.veil.data.NoteEntity

@Composable
fun StepEditCard(
    step: NoteEntity,
    dragInfo: ReorderDragInfo,
    onUpdateAction: (String) -> Unit,
    onUpdateLocation: (String) -> Unit,
    onSetTime: () -> Unit,
    onDelete: () -> Unit
) {
    var initialAction by remember(step.id) { mutableStateOf(step.finalAction ?: step.suggestion) }
    var actionText by remember(step.finalAction, step.suggestion) {
        mutableStateOf(
            step.finalAction ?: step.suggestion
        )
    }
    var locationText by remember(step.location) { mutableStateOf(step.location) }
    var stepTime by remember(step.id, step.time) { mutableStateOf(step.time) }

    var itemHeight by remember { mutableStateOf(0f) }
    var cumulativeOffset by remember { mutableStateOf(0f) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .zIndex(if (dragInfo.isDragged) 1f else 0f)
            .onSizeChanged { itemHeight = it.height.toFloat() }
            .graphicsLayer {
                if (dragInfo.isDragged || dragInfo.visualOffset != 0f) {
                    translationY = dragInfo.visualOffset
                    if (dragInfo.isDragged) {
                        scaleX = 1.02f
                        scaleY = 1.02f
                        shadowElevation = 6f
                    }
                }
            },
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
            ) {
                LabelRow(stringResource(R.string.label_action)) {
                    CompactTextField(
                        value = actionText,
                        onValueChange = { actionText = it }
                    )
                }
                TimeRow(stepTime, onClick = { onSetTime() })
                LabelRow(stringResource(R.string.label_location)) {
                    CompactTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        placeholder = stringResource(R.string.hint_add_location)
                    )
                }
                DisposableEffect(step.id) {
                    onDispose {
                        if (actionText != initialAction) {
                            onUpdateAction(actionText)
                        }
                        if (locationText != step.location) {
                            onUpdateLocation(locationText)
                        }
                    }
                }
            }

            Column(Modifier.fillMaxHeight()) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    cumulativeOffset = 0f
                                    dragInfo.onDragStart(itemHeight)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    cumulativeOffset += dragAmount.y
                                    dragInfo.onDrag(cumulativeOffset)
                                },
                                onDragEnd = {
                                    cumulativeOffset = 0f
                                    dragInfo.onDragEnd()
                                },
                                onDragCancel = {
                                    cumulativeOffset = 0f
                                    dragInfo.onDragEnd()
                                }
                            )
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StepsPreview(
    steps: List<NoteEntity>,
    onToggleStep: (NoteEntity) -> Unit,
    onPromoteStep: (step: NoteEntity, keepCopy: Boolean) -> Unit = { _, _ -> },
) {
    steps.forEach { step ->
        key(step.id) {
            var showMenu by remember { mutableStateOf(false) }
            val stepStatus = noteTimeStatus(step, null)
            val stepColor = timeStatusColor(stepStatus)
            Box {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showMenu = true }
                        ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = step.finalAction ?: step.suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = stepColor
                            )
                            Row {
                                Text(
                                    text = if (step.time != null) formatTime(step.time) else stringResource(R.string.hint_not_set),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(" · ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (step.location.isNotEmpty()) step.location else stringResource(R.string.hint_not_set),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Checkbox(checked = step.isCompleted, onCheckedChange = { onToggleStep(step) })
                    }
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("复制为新任务") },
                        onClick = {
                            showMenu = false
                            onPromoteStep(step, true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("转为独立任务") },
                        onClick = {
                            showMenu = false
                            onPromoteStep(step, false)
                        }
                    )
                }
            }
        }
    }
}
