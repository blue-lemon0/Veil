package com.lemon.veil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lemon.veil.data.NoteEntity
import kotlin.math.roundToInt

data class CombinedStep(
    val note: NoteEntity,
    val isNew: Boolean,
)

enum class ImportMode { COPY, MOVE }

private fun buildCombinedList(existing: List<NoteEntity>, selected: List<NoteEntity>): List<CombinedStep> {
    val existingIds = existing.map { it.id }.toSet()
    return existing.map { CombinedStep(it, isNew = false) } +
        selected.filter { it.id !in existingIds }.map { CombinedStep(it, isNew = true) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImportScreen(
    targetTitle: String,
    existingSteps: List<NoteEntity>,
    selectedItems: List<NoteEntity>,
    savedCombinedItems: List<CombinedStep>,
    importMode: ImportMode,
    onModeChange: (ImportMode) -> Unit,
    onPickSource: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (combinedItems: List<CombinedStep>) -> Unit,
    onCombinedItemsChange: (List<CombinedStep>) -> Unit,
) {
    var combinedItems by remember {
        mutableStateOf(
            if (savedCombinedItems.isNotEmpty()) savedCombinedItems
            else emptyList()
        )
    }

    // Build combined items once existing steps are loaded (first entry only)
    LaunchedEffect(existingSteps) {
        if (existingSteps.isNotEmpty() && combinedItems.isEmpty()) {
            combinedItems = buildCombinedList(existingSteps, selectedItems)
        }
    }

    // Append new items from additional selection trips
    LaunchedEffect(selectedItems) {
        val currentIds = combinedItems.map { it.note.id }.toSet()
        val newOnes = selectedItems.filter { it.id !in currentIds }
        if (newOnes.isNotEmpty()) {
            combinedItems = combinedItems + newOnes.map { CombinedStep(it, isNew = true) }
        }
    }

    // Persist order to ViewModel (only when IDs/order change, not on field edits)
    LaunchedEffect(combinedItems.map { it.note.id }) {
        if (combinedItems.isNotEmpty()) {
            onCombinedItemsChange(combinedItems)
        }
    }

    val pickerState = remember { DateTimePickerState() }
    DateTimePickerDialog(
        state = pickerState,
        initialTime = null,
        onMainTimePicked = {},
        onStepTimePicked = { stepId, time ->
            combinedItems = combinedItems.map { item ->
                if (item.note.id == stepId) item.copy(note = item.note.copy(time = time)) else item
            }
        },
    )

    val existingCount = combinedItems.count { !it.isNew }
    val newCount = combinedItems.count { it.isNew }

    BackHandler(onBack = onDismiss)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back))
                    }
                },
                title = {
                    Text(targetTitle, style = MaterialTheme.typography.titleMedium)
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_import_mode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = importMode == ImportMode.COPY,
                                onClick = { onModeChange(ImportMode.COPY) })
                            Text(stringResource(R.string.action_copy),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = importMode == ImportMode.MOVE,
                                onClick = { onModeChange(ImportMode.MOVE) })
                            Text(stringResource(R.string.action_move),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = { onConfirm(combinedItems) },
                            enabled = combinedItems.any { it.isNew }
                        ) { Text(stringResource(R.string.action_import)) }
                    }
                }
            }
        }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onPickSource,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_pick_source))
                }
                Spacer(Modifier.height(8.dp))

                Text(
                    stringResource(R.string.msg_preview_import, combinedItems.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (existingCount > 0 || newCount > 0) {
                    Text(
                        "已有 $existingCount 项 · 新增 $newCount 项",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(8.dp))

                ImportReorderColumn(
                    items = combinedItems,
                    onReorder = { from, to ->
                        val mutable = combinedItems.toMutableList()
                        mutable.add(to, mutable.removeAt(from))
                        combinedItems = mutable
                    },
                    onUpdate = { index, note ->
                        combinedItems = combinedItems.toMutableList().apply {
                            set(index, CombinedStep(note, this[index].isNew))
                        }
                    },
                    onRemove = { index ->
                        combinedItems = combinedItems.toMutableList().apply { removeAt(index) }
                    },
                    onSetTime = { stepId -> pickerState.pick(stepId) },
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                )

                Spacer(Modifier.height(8.dp))
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImportReorderColumn(
    items: List<CombinedStep>,
    modifier: Modifier = Modifier,
    onReorder: (Int, Int) -> Unit,
    onUpdate: (index: Int, note: NoteEntity) -> Unit,
    onRemove: (index: Int) -> Unit,
    onSetTime: (stepId: Long) -> Unit,
) {
    data class DragState(val fromIndex: Int, val totalOffset: Float, val itemHeight: Float)
    var dragState by remember { mutableStateOf<DragState?>(null) }

    Column(modifier = modifier) {
        items.forEachIndexed { index, combined ->
            val visualOffset = dragState?.let { ds ->
                if (index == ds.fromIndex) ds.totalOffset
                else {
                    val shift = (ds.totalOffset / ds.itemHeight).roundToInt()
                    val targetIndex = (ds.fromIndex + shift).coerceIn(0, items.size - 1)
                    when {
                        ds.fromIndex < targetIndex && index in (ds.fromIndex + 1)..targetIndex -> -ds.itemHeight
                        ds.fromIndex > targetIndex && index in targetIndex until ds.fromIndex -> ds.itemHeight
                        else -> 0f
                    }
                }
            } ?: 0f
            val isDragged = dragState?.fromIndex == index

            ImportStepCard(
                combined = combined,
                isDragged = isDragged,
                visualOffset = visualOffset,
                onDragStart = { itemHeight -> dragState = DragState(index, 0f, itemHeight) },
                onDrag = { totalOffset ->
                    dragState?.let { if (it.fromIndex == index) dragState = it.copy(totalOffset = totalOffset) }
                },
                onDragEnd = {
                    val ds = dragState
                    if (ds != null && ds.fromIndex == index && ds.itemHeight > 0f) {
                        val shift = (ds.totalOffset / ds.itemHeight).roundToInt()
                        val targetIndex = (index + shift).coerceIn(0, items.size - 1)
                        if (targetIndex != index) onReorder(index, targetIndex)
                    }
                    dragState = null
                },
                onActionChange = { action -> onUpdate(index, combined.note.copy(finalAction = action)) },
                onLocationChange = { location -> onUpdate(index, combined.note.copy(location = location)) },
                onRemove = { onRemove(index) },
                onSetTime = { onSetTime(combined.note.id) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImportStepCard(
    combined: CombinedStep,
    isDragged: Boolean,
    visualOffset: Float,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onActionChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onRemove: () -> Unit,
    onSetTime: () -> Unit,
) {
    val step = combined.note
    var actionText by remember(step.finalAction ?: step.suggestion) {
        mutableStateOf(step.finalAction ?: step.suggestion)
    }
    var locationText by remember(step.location) { mutableStateOf(step.location) }
    var stepTime by remember(step.time) { mutableStateOf(step.time) }
    var itemHeight by remember { mutableStateOf(0f) }
    var cumulativeOffset by remember { mutableStateOf(0f) }

    val borderColor = if (combined.isNew)
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
    else
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val containerColor = if (combined.isNew)
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.10f)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .zIndex(if (isDragged) 1f else 0f)
            .onSizeChanged { itemHeight = it.height.toFloat() }
            .graphicsLayer {
                if (isDragged || visualOffset != 0f) {
                    translationY = visualOffset
                    if (isDragged) {
                        scaleX = 1.02f
                        scaleY = 1.02f
                        shadowElevation = 6f
                    }
                }
            },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (combined.isNew) {
                        Text(
                            "新增",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        if (combined.isNew) "待导入" else stringResource(R.string.label_existing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                LabelRow(stringResource(R.string.label_action)) {
                    CompactTextField(
                        value = actionText,
                        onValueChange = {
                            actionText = it
                            onActionChange(it)
                        }
                    )
                }
                TimeRow(stepTime, onClick = onSetTime)
                LabelRow(stringResource(R.string.label_location)) {
                    CompactTextField(
                        value = locationText,
                        onValueChange = {
                            locationText = it
                            onLocationChange(it)
                        },
                        placeholder = stringResource(R.string.hint_add_location)
                    )
                }
            }

            Column(Modifier.fillMaxHeight()) {
                if (combined.isNew) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                                    onDragStart(itemHeight)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    cumulativeOffset += dragAmount.y
                                    onDrag(cumulativeOffset)
                                },
                                onDragEnd = {
                                    cumulativeOffset = 0f
                                    onDragEnd()
                                },
                                onDragCancel = {
                                    cumulativeOffset = 0f
                                    onDragEnd()
                                }
                            )
                        }
                )
            }
        }
    }
}
