package com.lemon.veil.utils

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.ActivityCompat

data class CalendarSyncResult(
    val success: Boolean,
    val eventId: Long? = null,
    val errorMessage: String? = null
)

object CalendarManager {
    private const val TAG = "CalendarManager"

    /**
     * Add or update a calendar event.
     * Automatically detects if the linked event was deleted externally.
     */
    fun addEvent(
        context: Context, 
        eventId: Long?, 
        title: String, 
        description: String, 
        startTimeMillis: Long,
        endTimeMillis: Long? = null
    ): CalendarSyncResult {
        if (!hasPermission(context, Manifest.permission.WRITE_CALENDAR)) {
            return CalendarSyncResult(false, errorMessage = "❌ 权限被拒绝")
        }

        val calId = getWritableCalendarId(context) 
            ?: return CalendarSyncResult(false, errorMessage = "❌ 无可用日历账户")

        val end = endTimeMillis ?: (startTimeMillis + 60 * 60 * 1000)

        return try {
            // 1. 智能校验：如果关联的 ID 在系统日历中已不存在，则视为新事件
            var targetId = eventId
            if (targetId != null) {
                // 仅在有读权限时进行校验，防止因权限不足导致误判而重复创建
                if (hasPermission(context, Manifest.permission.READ_CALENDAR)) {
                    if (!checkEventExists(context, targetId)) {
                        Log.w(TAG, "⚠️ Linked event $targetId missing. Recreating.")
                        targetId = null
                    }
                }
            }

            // 2. 尝试更新（如果 ID 有效）
            val finalEventId = targetId?.let { existingId ->
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)
                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startTimeMillis)
                    put(CalendarContract.Events.DTEND, end)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DESCRIPTION, description)
                    put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
                }
                val rowsUpdated = context.contentResolver.update(updateUri, values, null, null)
                if (rowsUpdated > 0) existingId else null
            }

            // 3. 如果没有有效 ID 或更新失败，插入新事件
            val resultId = finalEventId ?: insertNewEvent(context, calId, title, description, startTimeMillis, end)

            if (resultId != null) {
                CalendarSyncResult(true, eventId = resultId)
            } else {
                CalendarSyncResult(false, errorMessage = "❌ 事件创建失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during calendar write: ${e.message}")
            CalendarSyncResult(false, errorMessage = "❌ 异常: ${e.message}")
        }
    }

    /**
     * Remove a calendar event by ID.
     */
    fun removeEvent(context: Context, eventId: Long): Boolean {
        if (!hasPermission(context, Manifest.permission.WRITE_CALENDAR)) {
            return false
        }

        return try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete event: ${e.message}")
            false
        }
    }

    /**
     * Check if an event exists and is still valid in the calendar.
     */
    fun checkEventExists(context: Context, eventId: Long): Boolean {
        if (!hasPermission(context, Manifest.permission.READ_CALENDAR)) {
            return false
        }

        val projection = arrayOf(CalendarContract.Events._ID)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                cursor.moveToFirst() && cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Event $eventId not found or inaccessible.")
            false
        }
    }

    private fun insertNewEvent(
        context: Context, calId: Long, title: String, description: String, start: Long, end: Long
    ): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calId)
            put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return uri?.lastPathSegment?.toLongOrNull()
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun getWritableCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        
        val uri = CalendarContract.Calendars.CONTENT_URI
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use {
                var firstId: Long? = null
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                val name = it.getString(1) ?: "Unnamed"
                    
                if (firstId == null) firstId = id
                    
                // Prioritize modifiable calendars, especially local/device ones
                if (name.contains("phone", ignoreCase = true) || 
                    name.contains("device", ignoreCase = true) || 
                    name.contains("local", ignoreCase = true) ||
                    name.contains("notion", ignoreCase = true) ||
                    name.contains("google", ignoreCase = true)
                ) {
                    return id
                }
                }
                // Fallback to the first available modifiable calendar
                if (firstId != null) return firstId
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission denied while querying calendars.")
        }
        
        return null
    }
}