package com.lemon.veil

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lemon.veil.ai.AiRepository
import com.lemon.veil.ai.AiRepositoryImpl
import com.lemon.veil.ai.ChatMessage
import com.lemon.veil.data.NoteEntity
import com.lemon.veil.data.NoteRepository
import com.lemon.veil.data.NoteRepositoryImpl
import com.lemon.veil.utils.AlarmScheduler
import com.lemon.veil.utils.CalendarManager

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository: NoteRepository = NoteRepositoryImpl(application)
    private val aiRepository: AiRepository = AiRepositoryImpl.getInstance()

    val notes: StateFlow<List<NoteEntity>> = noteRepository.getRootNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _stepCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val stepCounts: StateFlow<Map<Long, Int>> = _stepCounts.asStateFlow()

    init {
        viewModelScope.launch {
            notes.collect { topLevel ->
                val counts = mutableMapOf<Long, Int>()
                for (note in topLevel) {
                    counts[note.id] = noteRepository.getStepCount(note.id)
                }
                _stepCounts.value = counts
            }
        }
    }

    // === Expand / collapse ===
    private val _expandedParents = MutableStateFlow<Set<Long>>(emptySet())
    val expandedParents: StateFlow<Set<Long>> = _expandedParents.asStateFlow()

    private val _expandedSteps = MutableStateFlow<Map<Long, List<NoteEntity>>>(emptyMap())
    val expandedSteps: StateFlow<Map<Long, List<NoteEntity>>> = _expandedSteps.asStateFlow()

    // === Search ===
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredNotes = combine(notes, _searchQuery, noteRepository.getAllNotes()) { rootNotes, query, allNotes ->
        if (query.isBlank()) return@combine rootNotes
        val q = query.lowercase()
        val matchingParentIds = allNotes
            .filter { it.parentId != null }
            .filter { step ->
                step.suggestion.lowercase().contains(q) ||
                step.originalText.lowercase().contains(q) ||
                (step.finalAction?.lowercase()?.contains(q) == true) ||
                step.location.lowercase().contains(q)
            }
            .mapNotNullTo(HashSet()) { it.parentId }
        rootNotes.filter { note ->
            note.suggestion.lowercase().contains(q) ||
            note.originalText.lowercase().contains(q) ||
            (note.finalAction?.lowercase()?.contains(q) == true) ||
            note.location.lowercase().contains(q) ||
            note.id in matchingParentIds
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    // === Detail steps ===
    data class StepsState(val noteId: Long? = null, val steps: List<NoteEntity> = emptyList(), val loaded: Boolean = false)

    private val _stepsState = MutableStateFlow(StepsState())
    val stepsState: StateFlow<StepsState> = _stepsState.asStateFlow()

    private var detailStepsJob: Job? = null

    fun onSelectNote(note: NoteEntity?) {
        detailStepsJob?.cancel()
        if (note == null) {
            _stepsState.value = StepsState()
            return
        }
        _stepsState.value = StepsState(noteId = note.id)
        viewModelScope.launch {
            val steps = noteRepository.getStepsOnce(note.id)
            _stepsState.value = StepsState(noteId = note.id, steps = steps, loaded = true)
            detailStepsJob = viewModelScope.launch {
                noteRepository.getStepsByParentId(note.id).collect { updated ->
                    _stepsState.value = StepsState(noteId = note.id, steps = updated, loaded = true)
                }
            }
        }
    }

    // === Conversation ===
    private val _conversation = MutableStateFlow(ConversationState())
    val conversation: StateFlow<ConversationState> = _conversation.asStateFlow()

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val userMsg = ChatMessage("user", text)
            _conversation.update {
                it.copy(messages = it.messages + userMsg, isLoading = true, errorMessage = null,
                    reply = null, exploreOptions = null, pendingTasks = emptyList())
            }

            val result = aiRepository.getSuggestion(_conversation.value.messages, getApplication())
            if (result.isFailure) {
                _conversation.update { it.copy(isLoading = false, errorMessage = "网络请求失败: ${result.exceptionOrNull()?.message}") }
                return@launch
            }

            val responseObj = result.getOrThrow()
            val aiReply = responseObj.message ?: "你有什么想法？"
            val aiMsg = ChatMessage("assistant", aiReply)

            if (responseObj.mode == "chat") {
                _conversation.update {
                    it.copy(messages = it.messages + aiMsg, isLoading = false, reply = aiReply, exploreOptions = responseObj.options)
                }
                return@launch
            }

            val firstUserMsg = _conversation.value.messages.firstOrNull { it.role == "user" }?.content ?: text
            val tasks = responseObj.tasks?.mapNotNull { task ->
                if (task.suggestion.isBlank()) return@mapNotNull null
                val stepEntities = task.steps?.mapIndexed { index, step ->
                    NoteEntity(originalText = firstUserMsg, suggestion = step.suggestion, type = task.type,
                        location = step.location ?: "", time = parseSuggestedTime(step.suggested_time), stepOrder = index)
                } ?: emptyList()
                val parentTime = if (stepEntities.isNotEmpty()) {
                    stepEntities.minOfOrNull { it.time ?: Long.MAX_VALUE }
                        ?.takeIf { it != Long.MAX_VALUE }
                } else parseSuggestedTime(task.suggested_time)
                NoteEntity(originalText = firstUserMsg, suggestion = task.suggestion, type = task.type,
                    location = task.location ?: "", time = parentTime).also { it.steps = stepEntities }
            } ?: emptyList()

            _conversation.update {
                it.copy(messages = it.messages + aiMsg, isLoading = false, reply = aiReply,
                    pendingTasks = tasks, errorMessage = if (tasks.isEmpty()) "AI 未提取到有效行动" else null)
            }
        }
    }

    fun clearChat() { _conversation.update { ConversationState() } }
    fun retry() {
        val lastUserMsg = _conversation.value.messages.lastOrNull { it.role == "user" }?.content ?: return
        sendMessage(lastUserMsg)
    }
    fun clearErr() { _conversation.update { it.copy(errorMessage = null) } }

    // === Settings ===
    fun openAlarmSettings() {
        _uiState.update { it.copy(showAlarmRequest = false) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            getApplication<Application>().startActivity(intent)
        }
    }

    fun dismissAlarmRequest() {
        _uiState.update { it.copy(showAlarmRequest = false) }
        Toast.makeText(getApplication(), "⚠️ 闹钟可能会有延迟", Toast.LENGTH_SHORT).show()
    }

    // === Confirm tasks from AI ===
    fun confirm(notes: List<NoteEntity>) {
        viewModelScope.launch {
            var hasAlarmError = false
            var hasCalendarError = false

            for (parentNote in notes) {
                val savedSteps = parentNote.steps
                val parentId = noteRepository.insertTask(
                    parentNote.copy(finalAction = parentNote.suggestion).also { it.steps = savedSteps }
                )
                val insertedSteps = noteRepository.getStepsOnce(parentId)
                for (step in insertedSteps) {
                    if (step.time == null || step.time <= System.currentTimeMillis()) continue
                    if (AlarmScheduler.scheduleAlarm(getApplication(), step.time, step.id, "步骤提醒", step.suggestion) != null) {
                        noteRepository.updateAlarmStatus(step.id, true)
                    } else {
                        hasAlarmError = true
                    }
                }
                if (parentNote.time == null) continue
                val time = parentNote.time
                if (AlarmScheduler.scheduleAlarm(getApplication(), time, parentId, "任务提醒", parentNote.suggestion) != null) {
                    noteRepository.updateAlarmStatus(parentId, true)
                } else {
                    hasAlarmError = true
                }
                val result = CalendarManager.addEvent(getApplication(), eventId = null, title = parentNote.suggestion, description = parentNote.originalText, startTimeMillis = time)
                if (result.success && result.eventId != null) {
                    noteRepository.updateSyncStatus(parentId, result.eventId, true)
                } else {
                    hasCalendarError = true
                    Log.e("MainViewModel", "Calendar failed: ${result.errorMessage}")
                }
            }

            if (hasAlarmError) Toast.makeText(getApplication(), "⚠️ 部分提醒未能设置（请检查闹钟权限）", Toast.LENGTH_LONG).show()
            if (hasCalendarError) Toast.makeText(getApplication(), "⚠️ 部分日程未能添加到日历", Toast.LENGTH_LONG).show()
            _conversation.update { ConversationState() }
        }
    }

    // === Calendar ===
    fun syncCal(note: NoteEntity) {
        viewModelScope.launch {
            if (note.time == null) {
                Toast.makeText(getApplication(), "⚠️ 未设置时间，无法同步到日历", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val result = CalendarManager.addEvent(getApplication(), note.calendarEventId, note.suggestion, note.originalText, note.time)
            if (result.success && result.eventId != null) {
                noteRepository.updateSyncStatus(note.id, result.eventId, true)
                Toast.makeText(getApplication(), "📅 已同步到系统日历", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), result.errorMessage ?: "同步失败", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun removeCal(note: NoteEntity) {
        viewModelScope.launch {
            if (note.calendarEventId == null) return@launch
            val removed = CalendarManager.removeEvent(getApplication(), note.calendarEventId)
            noteRepository.updateSyncStatus(note.id, null, false)
            Toast.makeText(getApplication(), if (removed) "🗑️ 已从日历移除" else "⚠️ 日程已丢失，同步状态已重置", Toast.LENGTH_SHORT).show()
        }
    }

    // === Alarm ===
    fun toggleAlarm(note: NoteEntity) {
        viewModelScope.launch {
            if (note.hasAlarm) {
                AlarmScheduler.cancelAlarm(getApplication(), note.id)
                noteRepository.updateAlarmStatus(note.id, false)
                Toast.makeText(getApplication(), "🔕 闹钟已关闭", Toast.LENGTH_SHORT).show()
            } else {
                if (note.time == null || note.time <= System.currentTimeMillis()) {
                    Toast.makeText(getApplication(), "⚠️ 请先设置一个未来时间", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (AlarmScheduler.scheduleAlarm(getApplication(), note.time, note.id, "日程提醒", note.suggestion) != null) {
                    noteRepository.updateAlarmStatus(note.id, true)
                    Toast.makeText(getApplication(), "🔔 闹钟已开启", Toast.LENGTH_SHORT).show()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            _uiState.update { it.copy(showAlarmRequest = true) }
                        }
                    }
                }
            }
        }
    }

    // === Note editing ===
    fun updateTime(noteId: Long, newTime: Long?) {
        viewModelScope.launch {
            val oldNote = noteRepository.getNoteById(noteId)
            noteRepository.updateSuggestedTime(noteId, newTime)
            AlarmScheduler.cancelAlarm(getApplication(), noteId)
            if (oldNote?.hasAlarm != true || newTime == null || newTime <= System.currentTimeMillis()) {
                if (newTime == null && oldNote?.hasAlarm == true) {
                    Toast.makeText(getApplication(), "🔕 提醒已取消", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            if (AlarmScheduler.scheduleAlarm(getApplication(), newTime, noteId, "日程提醒", oldNote.suggestion) != null) {
                Toast.makeText(getApplication(), "⏰ 闹钟已更新", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    _uiState.update { it.copy(showAlarmRequest = true) }
                }
            }
        }
    }

    fun updateLocation(noteId: Long, newLocation: String) {
        viewModelScope.launch {
            noteRepository.updateLocationField(noteId, newLocation)
        }
    }

    fun updateAction(noteId: Long, newAction: String) {
        viewModelScope.launch {
            noteRepository.updateActionField(noteId, newAction)
        }
    }

    fun saveHabitDesign(
        noteId: Long,
        cue: String, craving: String, responsePlan: String, reward: String,
        badCue: String, badCraving: String, badResponsePlan: String, badReward: String
    ) {
        viewModelScope.launch {
            noteRepository.updateHabitDesign(noteId, cue, craving, responsePlan, reward, badCue, badCraving, badResponsePlan, badReward)
        }
    }

    fun quickCreate(action: String, time: Long?, location: String) {
        viewModelScope.launch {
            val note = NoteEntity(originalText = action, suggestion = action, finalAction = action, location = location, time = time)
            val id = noteRepository.insertNote(note)
            if (time == null || time <= System.currentTimeMillis()) {
                return@launch
            }
            if (AlarmScheduler.scheduleAlarm(getApplication(), time, id, "日程提醒", action) != null) {
                noteRepository.updateAlarmStatus(id, true)
                return@launch
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    _uiState.update { it.copy(showAlarmRequest = true) }
                }
            }
        }
    }

    // === Completion ===
    fun toggleDone(note: NoteEntity) {
        viewModelScope.launch {
            val newCompleted = !note.isCompleted
            noteRepository.updateCompleted(note.id, newCompleted)
            if (newCompleted) {
                AlarmScheduler.cancelAlarm(getApplication(), note.id)
                noteRepository.updateAlarmStatus(note.id, false)
            }
        }
    }

    fun toggleStep(step: NoteEntity) {
        viewModelScope.launch {
            val parentId = step.parentId ?: return@launch
            val newCompleted = !step.isCompleted
            noteRepository.updateCompleted(step.id, newCompleted)
            val steps = noteRepository.getStepsOnce(parentId)
            val completedCount = steps.count { it.isCompleted }
            noteRepository.updateStepIndex(parentId, completedCount)
            when {
                !newCompleted -> noteRepository.updateCompleted(parentId, false)
                completedCount >= steps.size -> {
                    noteRepository.updateCompleted(parentId, true)
                    noteRepository.getNoteById(parentId)?.let { parent ->
                        AlarmScheduler.cancelAlarm(getApplication(), parent.id)
                    }
                }
            }
            if (_expandedParents.value.contains(parentId)) {
                _expandedSteps.value = _expandedSteps.value + (parentId to noteRepository.getStepsOnce(parentId))
            }
        }
    }

    // === Steps ===
    fun addStep(parentId: Long, action: String, time: Long?, location: String) {
        viewModelScope.launch {
            val existing = noteRepository.getStepsOnce(parentId)
            val insertIndex = if (time != null) {
                val idx = existing.indexOfFirst { it.time != null && it.time > time }
                if (idx == -1) existing.size else idx
            } else existing.size
            for (i in insertIndex until existing.size) {
                noteRepository.updateStepOrder(existing[i].id, i + 1)
            }
            val step = NoteEntity(originalText = action, suggestion = action, finalAction = action,
                location = location, time = time, parentId = parentId, stepOrder = insertIndex)
            noteRepository.insertNote(step)
            if (_expandedParents.value.contains(parentId)) {
                _expandedSteps.value = _expandedSteps.value + (parentId to noteRepository.getStepsOnce(parentId))
            }
            if (time == null) {
                return@launch
            }
            val parent = noteRepository.getNoteById(parentId)
            if (parent?.time == null || time < parent.time!!) {
                noteRepository.updateSuggestedTime(parentId, time)
                if (AlarmScheduler.scheduleAlarm(getApplication(), time, parentId, "任务提醒", parent?.suggestion ?: "") != null) {
                    noteRepository.updateAlarmStatus(parentId, true)
                }
            }
            if (AlarmScheduler.scheduleAlarm(getApplication(), time, step.id, "步骤提醒", action) != null) {
                noteRepository.updateAlarmStatus(step.id, true)
            }
        }
    }

    fun reorderStep(parentId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val steps = noteRepository.getStepsOnce(parentId)
            if (fromIndex < 0 || fromIndex >= steps.size || toIndex < 0 || toIndex >= steps.size) return@launch
            if (fromIndex == toIndex) return@launch
            val mutableSteps = steps.toMutableList()
            val item = mutableSteps.removeAt(fromIndex)
            mutableSteps.add(toIndex, item)
            mutableSteps.forEachIndexed { index, step ->
                noteRepository.updateStepOrder(step.id, index)
            }
            if (_expandedParents.value.contains(parentId)) {
                _expandedSteps.value = _expandedSteps.value + (parentId to mutableSteps)
            }
            if (_stepsState.value.noteId == parentId) {
                _stepsState.value = StepsState(noteId = parentId, steps = mutableSteps, loaded = true)
            }
        }
    }

    fun stepUp(parentId: Long, stepId: Long) {
        viewModelScope.launch {
            val steps = noteRepository.getStepsOnce(parentId)
            val idx = steps.indexOfFirst { it.id == stepId }
            if (idx <= 0) return@launch
            noteRepository.updateStepOrder(stepId, idx - 1)
            noteRepository.updateStepOrder(steps[idx - 1].id, idx)
            if (_expandedParents.value.contains(parentId)) {
                _expandedSteps.value = _expandedSteps.value + (parentId to noteRepository.getStepsOnce(parentId))
            }
        }
    }

    fun stepDown(parentId: Long, stepId: Long) {
        viewModelScope.launch {
            val steps = noteRepository.getStepsOnce(parentId)
            val idx = steps.indexOfFirst { it.id == stepId }
            if (idx < 0 || idx >= steps.size - 1) return@launch
            noteRepository.updateStepOrder(stepId, idx + 1)
            noteRepository.updateStepOrder(steps[idx + 1].id, idx)
            if (_expandedParents.value.contains(parentId)) {
                _expandedSteps.value = _expandedSteps.value + (parentId to noteRepository.getStepsOnce(parentId))
            }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
            AlarmScheduler.cancelAlarm(getApplication(), note.id)
        }
    }

    fun deleteStep(stepId: Long) {
        viewModelScope.launch {
            noteRepository.getNoteById(stepId)?.let { noteRepository.deleteNote(it) }
            AlarmScheduler.cancelAlarm(getApplication(), stepId)
        }
    }

    // === Batch operations ===
    fun mergeNotes(noteIds: List<Long>, newTitle: String) {
        viewModelScope.launch {
            noteRepository.mergeNotes(noteIds, newTitle)
        }
    }

    fun setAsSubTasks(noteIds: List<Long>, targetId: Long) {
        viewModelScope.launch {
            noteRepository.setAsSubTasks(noteIds, targetId)
        }
    }

    fun promoteToTask(stepIds: List<Long>) {
        viewModelScope.launch {
            noteRepository.promoteToRoot(stepIds)
        }
    }

    fun promoteStep(step: NoteEntity, keepCopy: Boolean) {
        viewModelScope.launch {
            val root = NoteEntity(
                originalText = step.originalText,
                suggestion = step.suggestion,
                type = step.type,
                time = step.time,
                location = step.location,
                finalAction = step.finalAction,
            )
            noteRepository.insertNote(root)
            if (!keepCopy) {
                noteRepository.deleteNote(step)
                AlarmScheduler.cancelAlarm(getApplication(), step.id)
            }
        }
    }

    suspend fun loadSteps(parentId: Long): List<NoteEntity> = noteRepository.getStepsOnce(parentId)

    fun importSteps(targetId: Long, items: List<NoteEntity>, mode: ImportMode) {
        viewModelScope.launch {
            val count = noteRepository.getStepCount(targetId)
            if (mode == ImportMode.COPY) {
                items.forEachIndexed { index, item ->
                    noteRepository.insertNote(NoteEntity(
                        originalText = item.originalText,
                        suggestion = item.suggestion,
                        type = item.type,
                        time = item.time,
                        location = item.location,
                        finalAction = item.finalAction,
                        parentId = targetId,
                        stepOrder = count + index,
                    ))
                }
            } else {
                items.forEachIndexed { index, item ->
                    noteRepository.updateNoteParent(item.id, targetId, count + index)
                }
            }
        }
    }

    // === Import flow state ===
    private val _importState = MutableStateFlow(ImportFlowState())
    val importState: StateFlow<ImportFlowState> = _importState.asStateFlow()

    data class ImportFlowState(
        val targetId: Long? = null,
        val selectedItems: List<NoteEntity> = emptyList(),
        val mode: ImportMode = ImportMode.COPY,
        val excludedIds: Set<Long> = emptySet(),
        val combinedItems: List<CombinedStep> = emptyList(),
    )

    fun setImportTargetId(targetId: Long) {
        _importState.update { it.copy(targetId = targetId) }
    }

    fun setImportExcludedIds(ids: Set<Long>) {
        _importState.update { it.copy(excludedIds = ids) }
    }

    fun setImportMode(mode: ImportMode) {
        _importState.update { it.copy(mode = mode) }
    }

    fun setImportSelection(items: List<NoteEntity>) {
        _importState.update { state ->
            val existingIds = state.selectedItems.map { it.id }.toSet()
            state.copy(selectedItems = state.selectedItems + items.filter { it.id !in existingIds })
        }
    }

    fun removeImportItem(item: NoteEntity) {
        _importState.update { state ->
            state.copy(selectedItems = state.selectedItems.filter { it.id != item.id })
        }
    }

    fun setCombinedItems(items: List<CombinedStep>) {
        _importState.update { it.copy(combinedItems = items) }
    }

    fun clearImport() {
        _importState.value = ImportFlowState()
    }

    fun checkMoveConflicts(
        combinedItems: List<CombinedStep>,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val movedIds = combinedItems.filter { it.isNew }.map { it.note.id }.toSet()
            for (item in combinedItems.filter { it.isNew }) {
                val steps = noteRepository.getStepsOnce(item.note.id)
                if (steps.isNotEmpty() && steps.any { it.id !in movedIds }) {
                    onResult(true)
                    return@launch
                }
            }
            onResult(false)
        }
    }

    fun applyImportCombined(targetId: Long, combinedItems: List<CombinedStep>, mode: ImportMode) {
        viewModelScope.launch {
            combinedItems.forEachIndexed { index, combined ->
                if (combined.isNew && mode == ImportMode.COPY) {
                    noteRepository.insertNote(NoteEntity(
                        originalText = combined.note.originalText,
                        suggestion = combined.note.suggestion,
                        type = combined.note.type,
                        time = combined.note.time,
                        location = combined.note.location,
                        finalAction = combined.note.finalAction,
                        parentId = targetId,
                        stepOrder = index,
                    ))
                } else {
                    val noteId = combined.note.id
                    if (combined.isNew) {
                        noteRepository.updateNoteParent(noteId, targetId, index)
                    } else {
                        noteRepository.updateStepOrder(noteId, index)
                    }
                    combined.note.finalAction?.let { noteRepository.updateActionField(noteId, it) }
                    combined.note.time?.let { noteRepository.updateSuggestedTime(noteId, it) }
                    noteRepository.updateLocationField(noteId, combined.note.location)
                }
            }
        }
    }

    // === Helpers ===
    private fun parseSuggestedTime(timeStr: String?): Long? {
        if (timeStr.isNullOrBlank() || timeStr.equals("null", true)) return null
        val formats = listOf("yyyy-MM-dd HH:mm", "yyyy-MM-dd", "MM-dd HH:mm")
        for (f in formats) {
            try {
                val time = SimpleDateFormat(f, Locale.getDefault()).parse(timeStr)?.time
                Log.d("MainViewModel", "Parsed '$timeStr' as $time using format $f")
                return time
            } catch (_: Exception) {}
        }
        Log.w("MainViewModel", "Failed to parse time: $timeStr")
        return null
    }
}
