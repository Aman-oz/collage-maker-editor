package org.example.project.data.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max
import org.example.project.AppLog

/*
 * Android: one WorkManager job per reminder, tagged [WorkTag], which survives restarts and reboots and
 * needs no exact-alarm permission (it may run a few minutes late). The job shows the notification.
 */

private const val WorkTag = "reminder"
private const val ChannelId = "reminders"
private const val KeyId = "id"
private const val KeyTitle = "title"
private const val KeyBody = "body"
private const val KeyFeature = "feature"

/** The extra on the intent that opens the app from a reminder; MainActivity passes it to [ReminderTaps]. */
const val ReminderFeatureExtra = "reminder_feature"

/** androidApp's MainActivity, named here since shared can't see the app module's classes. */
private const val MainActivityClass = "org.example.project.MainActivity"

/** The app's purple, matching the push notifications. */
private const val AccentColor = 0xFF8B5CF6.toInt()

actual object LocalReminders {
    actual fun replaceAll(reminders: List<ScheduledReminder>, channelName: String) {
        val context = FileKit.context
        createChannel(context, channelName)
        val work = WorkManager.getInstance(context)
        work.cancelAllWorkByTag(WorkTag)
        val now = System.currentTimeMillis()
        for (reminder in reminders) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .addTag(WorkTag)
                .setInitialDelay(max(0L, reminder.fireAt - now), TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        KeyId to reminder.id,
                        KeyTitle to reminder.title,
                        KeyBody to reminder.body,
                        KeyFeature to reminder.feature?.name,
                    ),
                )
                .build()
            work.enqueue(request)
        }
    }

    actual fun cancelAll() {
        WorkManager.getInstance(FileKit.context).cancelAllWorkByTag(WorkTag)
    }

    private fun createChannel(context: Context, name: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(NotificationChannel(ChannelId, name, NotificationManager.IMPORTANCE_DEFAULT))
    }
}

/** Shows one reminder. Runs at (or a little after) the reminder's time. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        val id = inputData.getString(KeyId) ?: return Result.success()
        val title = inputData.getString(KeyTitle) ?: return Result.success()
        val body = inputData.getString(KeyBody)
        val open = Intent()
            .setComponent(ComponentName(context.packageName, MainActivityClass))
            // Brings the running app forward as it was (onNewIntent), or starts it.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ReminderFeatureExtra, inputData.getString(KeyFeature))
        val pending = PendingIntent.getActivity(
            context,
            id.hashCode(),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // The app's white status-bar icon lives in androidApp's resources, so it is looked up by name.
        @Suppress("DiscouragedApi")
        val smallIcon = context.resources.getIdentifier("ic_stat_notification", "drawable", context.packageName)
            .takeIf { it != 0 } ?: context.applicationInfo.icon
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(smallIcon)
            .setColor(AccentColor)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id.hashCode(), notification) }
            .onFailure { AppLog.w("Reminders", "Couldn't show reminder $id", it) }
        return Result.success()
    }
}

actual fun platformDayClock(): DayClock = object : DayClock {
    override fun minuteOfDay(epochMs: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = epochMs }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    override fun atMinuteOfDay(dayOf: Long, minuteOfDay: Int): Long = Calendar.getInstance().apply {
        timeInMillis = dayOf
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
