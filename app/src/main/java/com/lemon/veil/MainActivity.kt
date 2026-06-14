package com.lemon.veil

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.lemon.veil.ui.theme.VeilTheme
import com.lemon.veil.utils.AlarmScheduler
import com.lemon.veil.utils.PermissionHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PermissionHelper.setup(this)
        enableEdgeToEdge()
        AlarmScheduler.createNotificationChannel(this)

        requestPermissions()

        setContent {
            VeilTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(viewModel)
                }
            }
        }

        handleIntent(intent)
    }

    private fun requestPermissions() {
        val allPerms =
            PermissionHelper.CALENDAR_PERMISSIONS + PermissionHelper.NOTIFICATION_PERMISSIONS

        lifecycleScope.launch {
            val results = PermissionHelper.request(this@MainActivity, *allPerms)

            if (results[android.Manifest.permission.WRITE_CALENDAR] == true) {
                Toast.makeText(this@MainActivity, "✅ 已获取日历权限", Toast.LENGTH_SHORT).show()
            }
            if (results[android.Manifest.permission.POST_NOTIFICATIONS] == true) {
                Toast.makeText(this@MainActivity, "✅ 已获取通知权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                viewModel.sendMessage(text)
            }
        }
    }
}






