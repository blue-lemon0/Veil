package com.lemon.veil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lemon.veil.data.IdentityEntity
import com.lemon.veil.data.NoteEntity

@Composable
fun SourceSection(title: String, content: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (expanded) "\u25BE" else "\u25B8", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (expanded) {
        Text(
            text = content,
            modifier = Modifier.padding(start = 24.dp, top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SourceSections(originalText: String, suggestion: String) {
    var showOriginal by remember { mutableStateOf(false) }
    var showSuggestion by remember { mutableStateOf(false) }
    SourceSection(stringResource(R.string.section_original_note), originalText, showOriginal) { showOriginal = !showOriginal }
    Spacer(Modifier.height(8.dp))
    SourceSection(stringResource(R.string.section_ai_suggestion), suggestion, showSuggestion) { showSuggestion = !showSuggestion }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailContent(
    note: NoteEntity,
    steps: List<NoteEntity>,
    isEditingMain: Boolean,
    isEditingSteps: Boolean,
    isEditingDesign: Boolean,
    isEditingStack: Boolean,
    onEditMainClick: () -> Unit,
    onEditStepsClick: () -> Unit,
    onEditDesignClick: () -> Unit,
    onEditStackClick: () -> Unit,
    onSaveMain: (action: String, time: Long?, location: String) -> Unit,
    onSaveHabitDesign: (cue: String, craving: String, responsePlan: String, reward: String,
                        badCue: String, badCraving: String, badResponsePlan: String, badReward: String) -> Unit,
    onSaveHabitStack: (currentHabit: String, newHabit: String) -> Unit,
    noteIdentities: List<IdentityEntity> = emptyList(),
    allIdentities: List<IdentityEntity> = emptyList(),
    onSetNoteIdentities: (List<Long>) -> Unit = {},
    onOpenIdentityManager: () -> Unit = {},
    onNewIdentity: (name: String, description: String) -> Unit = { _, _ -> },
    onSyncToggle: (Boolean) -> Unit,
    onAlarmToggle: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    stepCallbacks: StepCallbacks,
) {
    var actionText by remember(note.id) { mutableStateOf(note.finalAction ?: note.suggestion) }
    var locationText by remember(note.id) { mutableStateOf(note.location) }
    var selectedTime by remember(note.id) { mutableStateOf(note.time) }
    var showActionError by remember { mutableStateOf(false) }

    var cue by remember(note.id) { mutableStateOf(note.cue) }
    var craving by remember(note.id) { mutableStateOf(note.craving) }
    var responsePlan by remember(note.id) { mutableStateOf(note.responsePlan) }
    var reward by remember(note.id) { mutableStateOf(note.reward) }
    var badCue by remember(note.id) { mutableStateOf(note.badCue) }
    var badCraving by remember(note.id) { mutableStateOf(note.badCraving) }
    var badResponsePlan by remember(note.id) { mutableStateOf(note.badResponsePlan) }
    var badReward by remember(note.id) { mutableStateOf(note.badReward) }

    var currentHabit by remember(note.id) { mutableStateOf(note.currentHabit) }
    var newHabit by remember(note.id) { mutableStateOf(note.newHabit) }

    var showIdentityPicker by remember { mutableStateOf(false) }
    var showIdentityForm by remember { mutableStateOf(false) }

    val status = noteTimeStatus(note, null)

    var localSteps by remember { mutableStateOf(steps) }
    LaunchedEffect(steps) {
        if (localSteps.size != steps.size) {
            localSteps = steps
        }
    }

    val pickerState = remember { DateTimePickerState() }
    DateTimePickerDialog(
        state = pickerState,
        initialTime = selectedTime,
        onMainTimePicked = { selectedTime = it },
        onStepTimePicked = stepCallbacks.onStepUpdateTime,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        val commitment = buildCommitmentText(selectedTime, locationText, actionText)
        NoteEditCard(
            commitment = commitment,
            isEditing = isEditingMain,
            onSave = {
                if (actionText.isNotEmpty()) {
                    onSaveMain(actionText, selectedTime, locationText)
                    onEditMainClick()
                } else {
                    showActionError = true
                }
            },
            onStartEdit = {
                actionText = note.finalAction ?: note.suggestion
                locationText = note.location
                selectedTime = note.time
                showActionError = false
                onEditMainClick()
            },
            action = actionText,
            onActionChange = {
                actionText = it
                if (showActionError && it.isNotEmpty()) showActionError = false
            },
            showActionError = showActionError,
            time = selectedTime,
            timeStatus = status,
            onTimeClick = { pickerState.pick(null) },
            location = locationText,
            onLocationChange = { locationText = it },
            identityChips = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    noteIdentities.forEach { identity ->
                        AssistChip(
                            onClick = { },
                            label = { Text(identity.name, style = MaterialTheme.typography.labelMedium) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            border = null,
                        )
                    }
                    AssistChip(
                        onClick = { showIdentityPicker = true },
                        label = { Text("+", style = MaterialTheme.typography.labelMedium) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    )
                }
            },
        )

        if (showIdentityPicker) {
            IdentityPickerDialog(
                allIdentities = allIdentities,
                selectedIds = noteIdentities.map { it.id }.toSet(),
                onConfirm = { selectedIds ->
                    onSetNoteIdentities(selectedIds.toList())
                    showIdentityPicker = false
                },
                onDismiss = { showIdentityPicker = false },
                onNewIdentity = { showIdentityForm = true; showIdentityPicker = false },
            )
        }

        if (showIdentityForm) {
            IdentityFormDialog(
                onConfirm = { name, description ->
                    onNewIdentity(name, description)
                    showIdentityForm = false
                    showIdentityPicker = true
                },
                onDismiss = { showIdentityForm = false; showIdentityPicker = true },
            )
        }

        Spacer(Modifier.height(12.dp))

        HabitStackCard(
            currentHabit = currentHabit,
            newHabit = newHabit,
            isEditing = isEditingStack,
            onToggleEdit = {
                if (isEditingStack) {
                    onSaveHabitStack(currentHabit, newHabit)
                }
                onEditStackClick()
            },
            onFieldChanged = { field, value ->
                when (field) {
                    "currentHabit" -> currentHabit = value
                    "newHabit" -> newHabit = value
                }
            },
        )

        Spacer(Modifier.height(12.dp))

        HabitDesignCard(
            cue = cue,
            craving = craving,
            responsePlan = responsePlan,
            reward = reward,
            badCue = badCue,
            badCraving = badCraving,
            badResponsePlan = badResponsePlan,
            badReward = badReward,
            isEditing = isEditingDesign,
            onToggleEdit = {
                if (isEditingDesign) {
                    onSaveHabitDesign(cue, craving, responsePlan, reward, badCue, badCraving, badResponsePlan, badReward)
                }
                onEditDesignClick()
            },
            onFieldChanged = { field, value ->
                when (field) {
                    "cue" -> cue = value
                    "craving" -> craving = value
                    "responsePlan" -> responsePlan = value
                    "reward" -> reward = value
                    "badCue" -> badCue = value
                    "badCraving" -> badCraving = value
                    "badResponsePlan" -> badResponsePlan = value
                    "badReward" -> badReward = value
                }
            },
        )

        Spacer(Modifier.height(12.dp))

        val completedCount = steps.count { it.isCompleted }
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.title_steps_progress, completedCount, steps.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    if (!isEditingSteps) {
                        IconButton(onClick = onImportClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "导入步骤",
                                modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = onEditStepsClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isEditingSteps) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditingSteps) stringResource(R.string.cd_save) else stringResource(R.string.cd_edit),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (steps.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    if (isEditingSteps) {
                        ReorderColumn(
                            items = localSteps,
                            onReorder = { fromIndex, toIndex ->
                                val mutable = localSteps.toMutableList()
                                mutable.add(toIndex, mutable.removeAt(fromIndex))
                                localSteps = mutable
                                stepCallbacks.onReorderStep(fromIndex, toIndex)
                            }
                        ) { step, _index, dragInfo ->
                            key(step.id) {
                                StepEditCard(
                                    step = step,
                                    dragInfo = dragInfo,
                                    onUpdateAction = { stepCallbacks.onStepUpdateAction(step.id, it) },
                                    onUpdateLocation = { stepCallbacks.onStepUpdateLocation(step.id, it) },
                                    onSetTime = { pickerState.pick(step.id) },
                                    onDelete = {
                                        localSteps = localSteps.filter { it.id != step.id }
                                        stepCallbacks.onDeleteStep(step.id)
                                    }
                                )
                            }
                        }
                    } else {
                        StepsPreview(steps, stepCallbacks.onToggleStep, onPromoteStep = stepCallbacks.onPromoteStep)
                    }
                }
                if (isEditingSteps) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { stepCallbacks.onAddStep("", null, "") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_add_step))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                AlarmRow(note.hasAlarm, note.time != null && note.time > System.currentTimeMillis(), onAlarmToggle)
                Spacer(Modifier.height(4.dp))
                CalendarSyncRow(note.isCalendarSynced, note.time != null, onSyncToggle)
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                SourceSections(note.originalText, note.suggestion)
            }
        }
    }
}
