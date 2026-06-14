package com.lemon.veil.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lemon.veil.MainActivity
import com.lemon.veil.R
import java.text.SimpleDateFormat
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val wakeLock: PowerManager.WakeLock? = try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmWakeLock").apply {
                acquire(10_000L) // Keep CPU awake for max 10s
            }
        } catch (e: SecurityException) {
            null
        }

        try {
            Log.d("AlarmReceiver", "🔔 Alarm triggered! Action: ${intent.action}")
            val title = intent.getStringExtra("title") ?: context.getString(R.string.notification_title)
            val content = intent.getStringExtra("content") ?: "您有一个待办事项需要处理"
            val noteId = intent.getLongExtra("noteId", 0L)

            // Ensure channel exists
            AlarmScheduler.createNotificationChannel(context)

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, noteId.toInt(), mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Full-screen intent for alarm-like behavior
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context, (noteId + 10000).toInt(), mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, "veil_reminder")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    NotificationManagerCompat.from(context).notify(noteId.toInt(), notification)
                    Log.d("AlarmReceiver", "✅ Notification posted.")
                } catch (e: SecurityException) {
                    Log.e("AlarmReceiver", "❌ Notification failed: ${e.message}")
                }
            } else {
                Log.e("AlarmReceiver", "❌ POST_NOTIFICATIONS permission missing.")
            }
        } finally {
            wakeLock?.release()
        }
    }
}

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    private const val CHANNEL_ID = "veil_reminder"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                importance
            ).apply {
                description = "提醒用户执行从笔记生成的任务或日程"
                setShowBadge(true)
                enableVibration(true)
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setBypassDnd(true)
                }
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleAlarm(context: Context, triggerTimeMillis: Long, noteId: Long, title: String, content: String): Long? {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        Log.d(TAG, "⏰ Scheduling alarm for: ${dateFormat.format(triggerTimeMillis)} (Now: ${dateFormat.format(now)})")

        if (triggerTimeMillis <= now) {
            Log.w(TAG, "❌ Time is in the past! Not scheduling.")
            return null
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.lemon.veil.ALARM_$noteId" // Unique action to prevent overwriting
            putExtra("title", title)
            putExtra("content", content)
            putExtra("noteId", noteId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, noteId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.d(TAG, "✅ Exact Alarm scheduled via setExactAndAllowWhileIdle.")
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                } else {
                    Log.w(TAG, "⚠️ Exact Alarm denied, falling back to inexact set().")
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                }
            } else {
                Log.d(TAG, "✅ Alarm scheduled (Legacy).")
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }
            triggerTimeMillis
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security Exception: ${e.message}")
            null
        }
    }

    fun cancelAlarm(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.lemon.veil.ALARM_$noteId"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, noteId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /** Reschedule all future alarms after reboot */
    fun rescheduleAll(context: Context, notes: List<com.lemon.veil.data.NoteEntity>) {
        val now = System.currentTimeMillis()
        for (note in notes) {
            if (note.time != null && note.time > now && !note.isCompleted) {
                scheduleAlarm(context, note.time, note.id, "任务提醒", note.suggestion)
            }
        }
    }
}
