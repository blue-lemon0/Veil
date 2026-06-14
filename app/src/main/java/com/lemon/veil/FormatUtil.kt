package com.lemon.veil

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lemon.veil.data.NoteEntity
import com.lemon.veil.ui.theme.WarningYellow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimeStatus { NORMAL, EXPIRED, OUT_OF_ORDER }

fun timeStatus(time: Long?, previousTime: Long?): TimeStatus {
    if (time == null) return TimeStatus.NORMAL
    if (time < System.currentTimeMillis()) return TimeStatus.EXPIRED
    if (previousTime != null && time < previousTime) return TimeStatus.OUT_OF_ORDER
    return TimeStatus.NORMAL
}

fun noteTimeStatus(note: NoteEntity, previousNote: NoteEntity?): TimeStatus =
    timeStatus(note.time, previousNote?.time)

fun formatTime(timestamp: Long?): String {
    if (timestamp == null) return ""
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()

    if (isSameDay(cal, now)) return "今天 $timeStr"

    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    if (isSameDay(cal, tomorrow)) return "明天 $timeStr"

    return if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR))
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    else
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
    a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

@Composable
fun timeStatusColor(status: TimeStatus, normalColor: Color = MaterialTheme.colorScheme.onSurface): Color = when (status) {
    TimeStatus.EXPIRED -> MaterialTheme.colorScheme.error
    TimeStatus.OUT_OF_ORDER -> WarningYellow
    TimeStatus.NORMAL -> normalColor
}

fun buildCommitmentText(note: NoteEntity): String =
    buildCommitmentText(note.time, note.location, note.finalAction ?: note.suggestion)

fun buildCommitmentText(time: Long?, location: String, action: String): String {
    val parts = mutableListOf<String>()
    time?.let { parts.add(formatTime(it)) }
    if (location.isNotEmpty()) parts.add("我会在$location")
    parts.add(action)
    return parts.joinToString(", ")
}
