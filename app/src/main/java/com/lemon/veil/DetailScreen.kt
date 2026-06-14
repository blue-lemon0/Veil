package com.lemon.veil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lemon.veil.data.NoteEntity

data class StepCallbacks(
    val onToggleStep: (NoteEntity) -> Unit,
    val onReorderStep: (Int, Int) -> Unit,
    val onAddStep: (action: String, time: Long?, location: String) -> Unit,
    val onStepUpdateAction: (Long, String) -> Unit,
    val onStepUpdateTime: (Long, Long?) -> Unit,
    val onStepUpdateLocation: (Long, String) -> Unit,
    val onDeleteStep: (Long) -> Unit,
    val onPromoteStep: (step: NoteEntity, keepCopy: Boolean) -> Unit = { _, _ -> },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    note: NoteEntity,
    onBack: () -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onSaveMain: (action: String, time: Long?, location: String) -> Unit,
    onSaveHabitDesign: (cue: String, craving: String, responsePlan: String, reward: String,
                        badCue: String, badCraving: String, badResponsePlan: String, badReward: String) -> Unit,
    onSyncToggle: (Boolean) -> Unit,
    onAlarmToggle: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    steps: List<NoteEntity>,
    stepCallbacks: StepCallbacks,
) {
    var isEditingMain by remember { mutableStateOf(false) }
    var isEditingSteps by remember { mutableStateOf(false) }
    var isEditingDesign by remember { mutableStateOf(false) }
    val isEditing = isEditingMain || isEditingSteps || isEditingDesign
    val onExitEdit by rememberUpdatedState { isEditingMain = false; isEditingSteps = false; isEditingDesign = false }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) {
                            isEditingMain = false
                            isEditingSteps = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                title = { Text(stringResource(R.string.title_task_detail), style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            DetailContent(
                note = note,
                steps = steps,
                isEditingMain = isEditingMain,
                isEditingSteps = isEditingSteps,
                isEditingDesign = isEditingDesign,
                onEditMainClick = { isEditingMain = !isEditingMain },
                onEditStepsClick = { isEditingSteps = !isEditingSteps },
                onEditDesignClick = { isEditingDesign = !isEditingDesign },
                onSaveMain = onSaveMain,
                onSaveHabitDesign = onSaveHabitDesign,
                onSyncToggle = onSyncToggle,
                onAlarmToggle = onAlarmToggle,
                onImportClick = onImportClick,
                stepCallbacks = stepCallbacks,
            )
        }
    }

    BackHandler(isEditing) { onExitEdit() }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = stringResource(R.string.text_confirm_delete_title),
            text = stringResource(R.string.text_confirm_delete_body),
            onConfirm = { showDeleteConfirm = false; onDelete(note) },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
