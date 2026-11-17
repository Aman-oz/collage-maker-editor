package org.example.project.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import org.example.project.R

/**
 * Firebase Cloud Messaging set-up, run once from [org.example.project.CollageApplication].
 *
 * Every install subscribes to the [Topic] topic (the same one the old LAS app used), so a campaign
 * sent from the Firebase console to that topic reaches everyone. Single devices can be targeted by
 * the FCM token, which is logged under the `Push` tag.
 *
 * The Android 13+ notification permission is asked for on Home (`RequestPushPermission` in shared).
 */
object PushNotifications {
    const val Topic = "news"

    /** The channel every push goes to, also set as FCM's default in the manifest. */
    const val ChannelId = "general"

    internal const val Tag = "Push"

    fun init(context: Context) {
        createChannel(context)
        val messaging = FirebaseMessaging.getInstance()
        messaging.isAutoInitEnabled = true
        messaging.subscribeToTopic(Topic).addOnCompleteListener { task ->
            if (task.isSuccessful) Log.i(Tag, "Subscribed to topic '$Topic'")
            else Log.w(Tag, "Couldn't subscribe to topic '$Topic'", task.exception)
        }
        messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) Log.i(Tag, "FCM token: ${task.result}")
            else Log.w(Tag, "Couldn't get the FCM token", task.exception)
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                ChannelId,
                context.getString(R.string.push_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }
}
