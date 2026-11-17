package org.example.project.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.example.project.MainActivity
import org.example.project.R
import java.net.URL

/**
 * Receives FCM messages.
 *
 * While the app is in the background, a message with a `notification` part is shown by FCM itself,
 * with the manifest's default icon, colour and channel, and this class isn't called. Everything else
 * comes here: any message while the app is open, and data-only messages at any time. Those are shown
 * as a notification from `title` / `body` / `image`, taken from the notification part or, failing
 * that, from the data part. Tapping it opens the app.
 */
class PushMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.i(PushNotifications.Tag, "New FCM token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        val data = message.data
        val title = notification?.title ?: data["title"] ?: getString(R.string.app_name)
        val body = notification?.body ?: data["body"] ?: data["message"]
        if (body.isNullOrBlank() && notification?.title == null && data["title"] == null) return
        val imageUrl = notification?.imageUrl?.toString() ?: data["image"]
        show(title, body, imageUrl?.let(::downloadImage))
    }

    private fun show(title: String, body: String?, image: Bitmap?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            // SINGLE_TOP keeps a running app as it was instead of restarting MainActivity.
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, PushNotifications.ChannelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(this, R.color.push_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp)
        if (image != null) {
            builder.setLargeIcon(image)
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(image).bigLargeIcon(null as Bitmap?))
        } else if (body != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }
        // A new id each time, so a second push doesn't replace the first.
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    /** Runs on FCM's worker thread, which allows blocking for a few seconds. */
    private fun downloadImage(url: String): Bitmap? = runCatching {
        URL(url).openConnection().run {
            connectTimeout = 5_000
            readTimeout = 5_000
            getInputStream().use(BitmapFactory::decodeStream)
        }
    }.onFailure { Log.w(PushNotifications.Tag, "Couldn't load the push image $url", it) }.getOrNull()
}
