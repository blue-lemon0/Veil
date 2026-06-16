package com.lemon.veil

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.lemon.veil.data.IdentityEntity
import com.lemon.veil.data.NoteEntity

private sealed class DisplayItem {
    data class Note(
        val note: NoteEntity,
        val isExpanded: Boolean,
        val stepCount: Int,
        val currentStepIndex: Int,
        val previous: NoteEntity?
    ) : DisplayItem()

    data class Step(val step: NoteEntity, val parentId: Long, val previous: NoteEntity?) :
        DisplayItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: MainViewModel,
    onOpenDetail: (Long) -> Unit = {},
    onOpenIdentityManager: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val stepCounts by viewModel.stepCounts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val expandedSteps by viewModel.expandedSteps.collectAsState()
    val allIdentities by viewModel.allIdentities.collectAsState()
    val selectedIdentityId by viewModel.selectedIdentityFilter.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<NoteEntity?>(null) }

    LaunchedEffect(conversation.isLoading, conversation.isActive) {
        if (conversation.isLoading || conversation.isActive) showSheet = true
    }

    val displayItems = remember(filteredNotes, stepCounts, expandedSteps) {
        buildList {
            var prev: NoteEntity? = null
            for (note in filteredNotes) {
                val sc = stepCounts[note.id] ?: 0
                val isExp = note.id in expandedSteps
                add(
                    DisplayItem.Note(
                        note = note,
                        isExpanded = isExp,
                        stepCount = sc,
                        currentStepIndex = note.currentStepIndex,
                        previous = prev
                    )
                )
                if (isExp) {
                    val steps = expandedSteps[note.id] ?: emptyList()
                    var stepPrev: NoteEntity? = null
                    for (step in steps) {
                        add(DisplayItem.Step(step = step, parentId = note.id, previous = stepPrev))
                        stepPrev = step
                    }
                }
                prev = note
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.title_home))
                            }
                        }
                    )
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::setSearchQuery
                    )
                    IdentityFilterBar(
                        identities = allIdentities,
                        selectedId = selectedIdentityId,
                        onSelect = viewModel::setIdentityFilter,
                        onManage = onOpenIdentityManager,
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showSheet = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_manual_input)
                    )
                }
            },
        ) { padding ->
            if (displayItems.isEmpty()) {
                EmptyState(searchQuery, modifier = Modifier.padding(padding))
            } else {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(displayItems, key = {
                        when (it) {
                            is DisplayItem.Note -> "note_${it.note.id}"
                            is DisplayItem.Step -> "step_${it.step.id}"
                        }
                    }) { item ->
                        when (item) {
                            is DisplayItem.Note -> {
                                NoteCard(
                                    note = item.note,
                                    onToggle = { viewModel.toggleDone(item.note) },
                                    onClick = { onOpenDetail(item.note.id) },
                                    onLongClick = {
                                        if (item.note.parentId == null) pendingDelete = item.note
                                    },
                                    previousNote = item.previous,
                                )
                            }

                            is DisplayItem.Step -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StepCard(
                                        step = item.step,
                                        onToggle = { viewModel.toggleStep(item.step) },
                                        onClick = { onOpenDetail(item.parentId) },
                                        previousStep = item.previous,
                                    )
                                    Column {
                                        IconButton(
                                            onClick = { viewModel.stepUp(item.parentId, item.step.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp,
                                                contentDescription = stringResource(R.string.cd_move_up),
                                                modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.stepDown(item.parentId, item.step.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown,
                                                contentDescription = stringResource(R.string.cd_move_down),
                                                modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        DeleteConfirmDialog(
            title = stringResource(R.string.text_confirm_delete_title),
            text = stringResource(R.string.text_confirm_delete_body),
            onConfirm = { viewModel.deleteNote(pendingDelete!!); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }

    if (showSheet) {
        ConversationSheet(
            conversation = conversation,
            onSend = viewModel::sendMessage,
            onDismiss = { showSheet = false },
            onConfirm = { tasks -> viewModel.confirm(tasks); showSheet = false },
            onRetry = viewModel::retry,
            onNewChat = viewModel::clearChat,
            onQuickCreate = viewModel::quickCreate
        )
    }

    conversation.errorMessage?.let { msg ->
        Toast.makeText(viewModel.getApplication(), msg, Toast.LENGTH_LONG).show()
        viewModel.clearErr()
    }

    if (uiState.showAlarmRequest) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlarmRequest() },
            title = { Text(stringResource(R.string.title_alarm_permission)) },
            text = { Text(stringResource(R.string.msg_alarm_permission)) },
            confirmButton = {
                Button(onClick = { viewModel.openAlarmSettings() }) {
                    Text(stringResource(R.string.action_go_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAlarmRequest() }) {
                    Text(stringResource(R.string.action_later))
                }
            })
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(40.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.hint_search),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun IdentityFilterBar(
    identities: List<IdentityEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onManage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var cardWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val animationDuration = 400

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = animationDuration),
    )
    val selectedName = if (selectedId == null) null
        else identities.find { it.id == selectedId }?.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.section_identities),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .onSizeChanged { cardWidthPx = it.width }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { expanded = !expanded },
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedName ?: stringResource(R.string.filter_all),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedName != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (selectedName != null) {
                        IconButton(
                            onClick = { onSelect(null) },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear_filter),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
            if (expanded) {
                BackHandler { expanded = false }
            }
            Popup(
                offset = IntOffset(0, with(density) { 36.dp.roundToPx() }),
            ) {
                IdentityPopupContent(
                    expanded = expanded,
                    cardWidthPx = cardWidthPx,
                    selectedId = selectedId,
                    identities = identities,
                    animationDuration = animationDuration,
                    onSelect = { id -> onSelect(id); expanded = false },
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onManage, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.title_identity_manager),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IdentityPopupContent(
    expanded: Boolean,
    cardWidthPx: Int,
    selectedId: Long?,
    identities: List<IdentityEntity>,
    animationDuration: Int,
    onSelect: (Long?) -> Unit,
) {
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = expanded,
        modifier = Modifier
            .width(with(density) { cardWidthPx.toDp() })
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            ),
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = animationDuration, easing = LinearEasing)
        ),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = animationDuration, easing = LinearEasing)
        ),
    ) {
        Column {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.filter_all),
                        color = if (selectedId == null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                    )
                },
                onClick = { onSelect(null) },
            )
            identities.forEach { identity ->
                DropdownMenuItem(
                    text = {
                        Text(
                            identity.name,
                            color = if (identity.id == selectedId) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { onSelect(identity.id) },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(searchQuery: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (searchQuery.isBlank()) {
                Icon(
                    Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.msg_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.msg_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    stringResource(R.string.msg_no_results),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
