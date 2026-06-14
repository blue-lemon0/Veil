package com.lemon.veil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lemon.veil.data.NoteEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class SheetMode { AI, QUICK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationSheet(
    conversation: ConversationState,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (List<NoteEntity>) -> Unit,
    onRetry: () -> Unit,
    onNewChat: () -> Unit,
    onQuickCreate: (action: String, time: Long?, location: String) -> Unit = { _, _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(SheetMode.AI) }

    val stopPropagation = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }

    var inputText by remember { mutableStateOf("") }
    val selectedIndices = remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(conversation.isActive) {
        if (conversation.isActive) mode = SheetMode.AI
    }
    LaunchedEffect(conversation.pendingTasks) {
        selectedIndices.value = emptySet()
    }

    ModalBottomSheet(
        onDismissRequest = { if (!conversation.isLoading) onDismiss() },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .nestedScroll(stopPropagation)
        ) {
            // Mode toggle chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == SheetMode.AI,
                    onClick = { mode = SheetMode.AI },
                    label = { Text("AI 对话") }
                )
                FilterChip(
                    selected = mode == SheetMode.QUICK,
                    onClick = { mode = SheetMode.QUICK },
                    label = { Text("快速笔记") }
                )
            }

            when {
                mode == SheetMode.QUICK -> {
                    QuickCreateView(
                        onQuickCreate = { action, time, location ->
                            onQuickCreate(action, time, location)
                            onDismiss()
                        },
                        onDismiss = onDismiss
                    )
                }
                conversation.messages.isEmpty() && !conversation.isLoading -> {
                    InputView(
                        value = inputText,
                        onValueChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSend(inputText.trim())
                                inputText = ""
                            }
                        },
                        onDismiss = onDismiss
                    )
                }
                else -> {
                    Box(modifier = Modifier.weight(1f)) {
                        ConversationContent(
                            conversation = conversation,
                            selectedIndices = selectedIndices.value,
                            onToggleSelection = { index ->
                                selectedIndices.value = if (selectedIndices.value.contains(index)) {
                                    selectedIndices.value - index
                                } else {
                                    selectedIndices.value + index
                                }
                            },
                            onOptionClick = { onSend(it) }
                        )
                    }

                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("继续对话...") },
                            singleLine = true,
                            enabled = !conversation.isLoading
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSend(inputText.trim())
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !conversation.isLoading
                        ) { Text("发送") }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, enabled = !conversation.isLoading) {
                            Text("关闭")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onNewChat, enabled = !conversation.isLoading) {
                                Text("新对话")
                            }
                            if (conversation.pendingTasks.isNotEmpty()) {
                                TextButton(onClick = onRetry, enabled = !conversation.isLoading) {
                                    Text("重试")
                                }
                                Button(
                                    onClick = {
                                        val selected = selectedIndices.value.sorted()
                                            .mapNotNull { conversation.pendingTasks.getOrNull(it) }
                                        if (selected.isNotEmpty()) onConfirm(selected)
                                    },
                                    enabled = selectedIndices.value.isNotEmpty() && !conversation.isLoading
                                ) { Text("采纳选中 (${selectedIndices.value.size})") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCreateView(
    onQuickCreate: (action: String, time: Long?, location: String) -> Unit,
    onDismiss: () -> Unit
) {
    var actionText by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    if (showDatePicker) {
        val cal = Calendar.getInstance().apply {
            selectedTime?.let { timeInMillis = it }
        }
        android.app.DatePickerDialog(
            context,
            android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT,
            { _, year, month, dayOfMonth ->
                val temp = Calendar.getInstance().apply {
                    selectedTime?.let { timeInMillis = it }
                }
                temp.set(year, month, dayOfMonth)
                selectedTime = temp.timeInMillis
                showDatePicker = false
                showTimePicker = true
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply {
            selectedTime?.let { timeInMillis = it }
        }
        android.app.TimePickerDialog(
            context,
            android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT,
            { _, hour, minute ->
                val temp = Calendar.getInstance().apply {
                    selectedTime?.let { timeInMillis = it }
                }
                temp.set(Calendar.HOUR_OF_DAY, hour)
                temp.set(Calendar.MINUTE, minute)
                selectedTime = temp.timeInMillis
                showTimePicker = false
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).apply {
            setOnDismissListener { showTimePicker = false }
            show()
        }
    }

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = actionText,
                    onValueChange = { actionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("行动") },
                    placeholder = { Text("输入要做的事...") },
                    singleLine = false,
                    minLines = 2
                )

                Spacer(Modifier.height(12.dp))

                LabelRow("地点") {
                    CompactTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        placeholder = "添加地点"
                    )
                }

                Spacer(Modifier.height(12.dp))

                LabelRow("时间") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedTime != null) selectedTime = null
                                else showDatePicker = true
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (selectedTime != null) {
                                "⏰ ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(selectedTime)}"
                            } else "未设定",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedTime != null) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("取消") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onQuickCreate(actionText.trim(), selectedTime, locationText.trim()) },
                enabled = actionText.isNotBlank()
            ) {
                Text("保存")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InputView(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text("粘贴或输入笔记内容", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("粘贴或输入笔记内容...") },
            singleLine = false,
            minLines = 3
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("取消") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSend, enabled = value.isNotBlank()) {
                Text("开始对话")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ConversationContent(
    conversation: ConversationState,
    selectedIndices: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    onOptionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))

        conversation.messages.forEach { msg ->
            when (msg.role) {
                "user" -> MessageBubble(text = msg.content, isUser = true)
                "assistant" -> MessageBubble(text = msg.content, isUser = false)
            }
        }

        if (conversation.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("AI 思考中...", style = MaterialTheme.typography.bodySmall)
            }
        }

        conversation.reply?.let { reply ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = reply,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            conversation.exploreOptions?.let { options ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        FilterChip(
                            selected = false,
                            onClick = { onOptionClick(option) },
                            label = { Text(option) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            val tasks = conversation.pendingTasks
            if (tasks.isNotEmpty()) {
                Text(
                    text = "行动建议 (${tasks.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                TaskSelectionList(
                    tasks = tasks,
                    selectedIndices = selectedIndices,
                    onToggleSelection = onToggleSelection
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MessageBubble(text: String, isUser: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TaskSelectionList(
    tasks: List<NoteEntity>,
    selectedIndices: Set<Int>,
    onToggleSelection: (Int) -> Unit
) {
    tasks.forEachIndexed { index, note ->
        val isSelected = selectedIndices.contains(index)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection(index) }
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        text = note.suggestion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (note.time != null) {
                        Text(
                            text = "  ${formatTime(note.time)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (note.steps.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        note.steps.forEach { step ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "\u2514 ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = step.suggestion,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (step.time != null) {
                                        Text(
                                            text = formatTime(step.time),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
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
