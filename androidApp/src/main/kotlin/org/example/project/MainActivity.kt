package org.example.project

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.data.reminders.ReminderFeatureExtra
import org.example.project.data.reminders.ReminderTaps

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Opened from a usage reminder (not again after a configuration change).
        if (savedInstanceState == null) ReminderTaps.onTapped(intent?.getStringExtra(ReminderFeatureExtra))

        setContent {
            App()
        }
    }

    /** A reminder tapped while the app is running brings it forward here, as it was. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReminderTaps.onTapped(intent.getStringExtra(ReminderFeatureExtra))
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}