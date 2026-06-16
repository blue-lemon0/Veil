package com.lemon.veil

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lemon.veil.data.NoteEntity

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "list") {
        composable("list") {
            ListScreen(
                viewModel = viewModel,
                onOpenDetail = { noteId -> navController.navigate("detail/$noteId") },
                onOpenIdentityManager = { navController.navigate("identity_manager") },
            )
        }
        composable(
            route = "detail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { entry ->
            val noteId = entry.arguments?.getLong("noteId") ?: return@composable
            DetailRoute(
                noteId = noteId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenImport = { targetId -> navController.navigate("import_preview/$targetId") },
                onOpenIdentityManager = { navController.navigate("identity_manager") },
            )
        }
        composable("identity_manager") {
            IdentityManagerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "import_preview/{targetId}",
            arguments = listOf(navArgument("targetId") { type = NavType.LongType })
        ) { entry ->
            val targetId = entry.arguments?.getLong("targetId") ?: return@composable
            ImportRoute(
                targetId = targetId,
                viewModel = viewModel,
                onPickSource = { navController.navigate("import_select") },
                onDismiss = { navController.popBackStack() },
                onImported = { navController.popBackStack() },
            )
        }
        composable("import_select") {
            val state = viewModel.importState.value
            val targetId = state.targetId ?: return@composable
            ImportSelectScreen(
                viewModel = viewModel,
                targetId = targetId,
                onConfirm = { items ->
                    viewModel.setImportSelection(items)
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                },
            )
        }
    }
}

@Composable
private fun DetailRoute(
    noteId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenImport: (Long) -> Unit,
    onOpenIdentityManager: () -> Unit,
) {
    val notes by viewModel.notes.collectAsState()
    val stepsState by viewModel.stepsState.collectAsState()
    val allIdentities by viewModel.allIdentities.collectAsState()
    val noteIdentitiesMap by viewModel.noteIdentities.collectAsState()

    val note = remember(noteId, notes) { notes.find { it.id == noteId } }
    val noteIdentities = remember(noteId, noteIdentitiesMap) { noteIdentitiesMap[noteId] ?: emptyList() }

    LaunchedEffect(noteId) {
        viewModel.onSelectNote(note)
        viewModel.loadNoteIdentities(noteId)
    }
    DisposableEffect(noteId) {
        onDispose {
            viewModel.onSelectNote(null)
            viewModel.unloadNoteIdentities(noteId)
        }
    }

    if (note != null) {
        DetailScreen(
            note = note,
        onBack = onBack,
        onDelete = { viewModel.deleteNote(it); onBack() },
        onSaveMain = { action, time, location ->
            viewModel.updateAction(note.id, action)
            viewModel.updateTime(note.id, time)
            viewModel.updateLocation(note.id, location)
        },
        onSaveHabitStack = { currentHabit, newHabit ->
            viewModel.saveHabitStack(note.id, currentHabit, newHabit)
        },
        onSaveHabitDesign = { cue, craving, responsePlan, reward,
            badCue, badCraving, badResponsePlan, badReward ->
            viewModel.saveHabitDesign(note.id, cue, craving, responsePlan, reward, badCue, badCraving, badResponsePlan, badReward)
        },
        noteIdentities = noteIdentities,
        allIdentities = allIdentities,
        onSetNoteIdentities = { identityIds -> viewModel.setNoteIdentities(note.id, identityIds) },
        onOpenIdentityManager = onOpenIdentityManager,
        onNewIdentity = { name, description -> viewModel.insertIdentity(name, description) },
        onSyncToggle = { if (it) viewModel.syncCal(note) else viewModel.removeCal(note) },
        onAlarmToggle = { viewModel.toggleAlarm(note) },
        onImportClick = { onOpenImport(note.id) },
        steps = stepsState.steps,
        stepCallbacks = StepCallbacks(
            onToggleStep = viewModel::toggleStep,
            onReorderStep = { fromIndex, toIndex ->
                viewModel.reorderStep(note.id, fromIndex, toIndex)
            },
            onAddStep = { action, time, location ->
                viewModel.addStep(note.id, action, time, location)
            },
            onStepUpdateAction = viewModel::updateAction,
            onStepUpdateTime = viewModel::updateTime,
            onStepUpdateLocation = viewModel::updateLocation,
            onDeleteStep = viewModel::deleteStep,
            onPromoteStep = viewModel::promoteStep,
        )
    )
    }
}

@Composable
private fun ImportRoute(
    targetId: Long,
    viewModel: MainViewModel,
    onPickSource: () -> Unit,
    onDismiss: () -> Unit,
    onImported: () -> Unit,
) {
    val notes by viewModel.notes.collectAsState()
    val importState by viewModel.importState.collectAsState()

    val targetNote = remember(targetId, notes) { notes.find { it.id == targetId } }
    var existingSteps by remember { mutableStateOf(emptyList<NoteEntity>()) }
    var pendingCombinedItems by remember { mutableStateOf<List<CombinedStep>>(emptyList()) }
    var showMoveConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(targetId) {
        existingSteps = viewModel.loadSteps(targetId)
        viewModel.setImportTargetId(targetId)
    }

    ImportScreen(
        targetTitle = targetNote?.suggestion ?: "",
        existingSteps = existingSteps,
        selectedItems = importState.selectedItems,
        savedCombinedItems = importState.combinedItems,
        importMode = importState.mode,
        onModeChange = viewModel::setImportMode,
        onCombinedItemsChange = viewModel::setCombinedItems,
        onPickSource = {
            val excluded = existingSteps.map { it.id }.toSet() + importState.selectedItems.map { it.id }.toSet()
            viewModel.setImportExcludedIds(excluded)
            onPickSource()
        },
        onDismiss = {
            viewModel.clearImport()
            onDismiss()
        },
        onConfirm = { combinedItems ->
            if (importState.mode == ImportMode.MOVE && combinedItems.any { it.isNew }) {
                pendingCombinedItems = combinedItems
                viewModel.checkMoveConflicts(combinedItems) { hasConflict ->
                    if (hasConflict) {
                        showMoveConfirmDialog = true
                    } else {
                        doImport(targetId, combinedItems, importState.mode, viewModel, onImported)
                    }
                }
            } else {
                doImport(targetId, combinedItems, importState.mode, viewModel, onImported)
            }
        },
    )

    if (showMoveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showMoveConfirmDialog = false },
            title = { Text("移动确认") },
            text = { Text("部分子步骤未被选择，移动后它们将从原任务中分离且无法恢复，确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    showMoveConfirmDialog = false
                    doImport(targetId, pendingCombinedItems, importState.mode, viewModel, onImported)
                }) { Text("确定移动") }
            },
            dismissButton = {
                TextButton(onClick = { showMoveConfirmDialog = false }) { Text("取消") }
            },
        )
    }
}

private fun doImport(
    targetId: Long,
    combinedItems: List<CombinedStep>,
    mode: ImportMode,
    viewModel: MainViewModel,
    onImported: () -> Unit,
) {
    viewModel.applyImportCombined(targetId, combinedItems, mode)
    viewModel.clearImport()
    onImported()
}
