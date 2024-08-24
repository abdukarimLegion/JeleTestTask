package ru.jeleapps.myapplication

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import ru.jeleapps.myapplication.ui.theme.JeleTestTaskTheme


class MainActivity : ComponentActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, proceed with notification
            showNotification()
        } else {
            handlePermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    showNotification()
                }
                else -> {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        }

        setContent {
            JeleTestTaskTheme {
                var token by remember { mutableStateOf("Token downloading...") }

                // Get FCM token
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        token = task.result ?: "Error retrieving token"
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Greeting(name = token)
                    }
                }
            }
        }
    }

    private fun handlePermissionDenied() {
        val deniedCount = sharedPrefs.getInt("notification_denied_count", 0)

        if (deniedCount >= 2) {
            showSettingsDialog()
        } else {
            with(sharedPrefs.edit()) {
                putInt("notification_denied_count", deniedCount + 1)
                apply()
            }
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Permission Required")
            setMessage("This app requires notification permission to function correctly. Please enable it in settings.")
            setPositiveButton("Settings") { _, _ ->
                // Navigate to app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            setCancelable(false)
        }.show()
    }

    private fun showNotification() {
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Notification")
            .setContentText("This is a test notification")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(101, notificationBuilder.build())
            } else {
                // Permission is not granted, show settings dialog
                showSettingsDialog()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hi JeleApps!\n$name",
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Color(0xFF6200EE), shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
            .wrapContentHeight(Alignment.CenterVertically)
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    JeleTestTaskTheme {
        Greeting("Android")
    }
}
