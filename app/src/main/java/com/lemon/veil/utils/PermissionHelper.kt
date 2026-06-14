package com.lemon.veil.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PermissionHelper {
    private var launcher: ActivityResultLauncher<Array<String>>? = null
    private var onPermissionsResult: ((Map<String, Boolean>) -> Unit)? = null

    // 定义日历相关权限
    val CALENDAR_PERMISSIONS = arrayOf(
        android.Manifest.permission.READ_CALENDAR,
        android.Manifest.permission.WRITE_CALENDAR
    )

    // 定义通知权限 (自动处理版本兼容)
    val NOTIFICATION_PERMISSIONS: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

    fun setup(activity: ComponentActivity) {
        launcher =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                onPermissionsResult?.invoke(permissions)
            }
    }

    suspend fun request(
        activity: ComponentActivity,
        vararg permissions: String
    ): Map<String, Boolean> {
        val alreadyGranted = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
        val missing = permissions.filter { it !in alreadyGranted }

        if (missing.isEmpty()) {
            return permissions.associateWith { true }
        }

        return suspendCancellableCoroutine { continuation ->
            onPermissionsResult = { result ->
                continuation.resume(result)
                onPermissionsResult = null
            }
            launcher?.launch(missing.toTypedArray())
        }
    }

    fun Context.hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
