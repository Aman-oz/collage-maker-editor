package org.example.project.data.reminders

/** One notification to show at [fireAt] (epoch millis). [feature] comes back in [ReminderTaps] when it is tapped. */
data class ScheduledReminder(
    val id: String,
    val fireAt: Long,
    val title: String,
    val body: String,
    val feature: ReminderFeature?,
)

/**
 * On-device scheduled notifications: WorkManager on Android, UNUserNotificationCenter on iOS. Only
 * ever touches its own ids (all starting with [ReminderPlanner.IdPrefix]), never Firebase pushes.
 * Nothing shows without the notification permission, which Home asks for.
 */
expect object LocalReminders {
    /** Cancels every pending reminder and schedules [reminders] instead. */
    fun replaceAll(reminders: List<ScheduledReminder>, channelName: String)

    fun cancelAll()
}

/** The device's local time of day. */
expect fun platformDayClock(): DayClock
