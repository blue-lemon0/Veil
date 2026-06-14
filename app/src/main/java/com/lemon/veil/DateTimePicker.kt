package com.lemon.veil

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

@Stable
class DateTimePickerState {
    var target: Long? by mutableStateOf(null)
    var pendingDate: Long? by mutableStateOf(null)
    var showDate: Boolean by mutableStateOf(false)
    var showTime: Boolean by mutableStateOf(false)

    fun pick(stepId: Long?) {
        target = stepId
        showDate = true
    }

    fun cancel() {
        showDate = false
        showTime = false
        target = null
        pendingDate = null
    }
}

@Composable
fun DateTimePickerDialog(
    state: DateTimePickerState,
    initialTime: Long?,
    onMainTimePicked: (Long) -> Unit,
    onStepTimePicked: (Long, Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    if (state.showDate) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (state.target == null) (initialTime
                ?: System.currentTimeMillis()) else System.currentTimeMillis()
        }
        DatePickerDialog(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT, { _, year, month, dayOfMonth ->
            val tempCal = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
            tempCal.set(year, month, dayOfMonth)
            state.pendingDate = tempCal.timeInMillis
            state.showDate = false
            state.showTime = true
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            setOnDismissListener { if (!state.showTime) state.cancel() }
            show()
        }
    }

    if (state.showTime) {
        val cal = Calendar.getInstance()
            .apply { timeInMillis = state.pendingDate ?: System.currentTimeMillis() }
        TimePickerDialog(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT, { _, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            val fullTime = cal.timeInMillis
            if (state.target == null) {
                onMainTimePicked(fullTime)
            } else {
                state.target?.let { sid -> onStepTimePicked(sid, fullTime) }
            }
            state.cancel()
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).apply {
            setOnDismissListener { state.cancel() }
            show()
        }
    }
}
