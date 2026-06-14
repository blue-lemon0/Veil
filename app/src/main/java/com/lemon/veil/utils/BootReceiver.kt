package com.lemon.veil.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lemon.veil.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("BootReceiver", "📱 Boot completed — rescheduling alarms")

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val allNotes = db.noteDao().getAllNotesOnce()
                AlarmScheduler.rescheduleAll(context, allNotes)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule alarms: ${e.message}")
            } finally {
                result.finish()
            }
        }
    }
}
