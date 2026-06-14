package com.lemon.veil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lemon.veil.data.NoteEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImportSelectScreen(
    viewModel: MainViewModel,
    targetId: Long,
    onConfirm: (List<NoteEntity>) -> Unit,
    onCancel: () -> Unit,
) {
    val importState by viewModel.importState.collectAsState()
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val stepCounts by viewModel.stepCounts.collectAsState()

    val visibleNotes = remember(filteredNotes, importState.excludedIds, targetId) {
        filteredNotes.filter { it.id != targetId && it.id !in importState.excludedIds }
    }

    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedNotes by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var expandedStepsCache by remember { mutableStateOf<Map<Long, List<NoteEntity>>>(emptyMap()) }

    BackHandler { onCancel() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                title = { Text(stringResource(R.string.action_pick_source), style = MaterialTheme.typography.titleMedium) }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "已选 ${selectedKeys.size} 项",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rootItems = visibleNotes.filter { "note_${it.id}" in selectedKeys }
                            val stepItems = expandedNotes.flatMap { nid ->
                                expandedStepsCache[nid] ?: emptyList()
                            }.filter { "step_${it.id}" in selectedKeys }
                            onConfirm(rootItems + stepItems)
                        },
                        enabled = selectedKeys.isNotEmpty()
                    ) { Text("确认选择") }
                }
            }
        }
    ) { padding ->
        if (visibleNotes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.msg_no_selection),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(visibleNotes, key = { it.id }) { note ->
                    val key = "note_${note.id}"
                    val isExpanded = note.id in expandedNotes
                    val sc = stepCounts[note.id] ?: 0
                    val toggleNote = {
                        selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                    }

                    SelectionCard(
                        checked = key in selectedKeys,
                        onToggle = toggleNote,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = note.finalAction ?: note.suggestion,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$sc 个子步骤",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (sc > 0) {
                            IconButton(
                                onClick = {
                                    expandedNotes = if (isExpanded) expandedNotes - note.id
                                    else expandedNotes + note.id
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "收起" else "展开",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (isExpanded) {
                        val steps = expandedStepsCache[note.id] ?: emptyList()
                        val visibleSteps = steps.filter { it.id !in importState.excludedIds }
                        LaunchedEffect(note.id) {
                            if (steps.isEmpty()) {
                                expandedStepsCache = expandedStepsCache + (note.id to viewModel.loadSteps(note.id))
                            }
                        }
                        visibleSteps.forEach { step ->
                            val stepKey = "step_${step.id}"
                            val toggleStep = {
                                selectedKeys = if (stepKey in selectedKeys) selectedKeys - stepKey else selectedKeys + stepKey
                            }
                            SelectionCard(
                                checked = stepKey in selectedKeys,
                                onToggle = toggleStep,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 40.dp, end = 12.dp, top = 3.dp, bottom = 3.dp)
                            ) {
                                Text(
                                    text = step.finalAction ?: step.suggestion,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectionCard(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Card(
        modifier = modifier.combinedClickable(onClick = onToggle, onLongClick = {}),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            content()
        }
    }
}
