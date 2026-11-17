package org.example.project.data.reminders

import org.example.project.AppLog
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/*
 * iOS: one UNNotificationRequest per reminder with a calendar trigger at its local date and time.
 * The tap comes back through AppDelegate, which hands `userInfo["feature"]` to ReminderTaps.
 */

private const val FeatureKey = "feature"

/** Every id the planner can produce, so cancelling never touches anything else. */
private val AllIds: List<String> = listOf("${ReminderPlanner.IdPrefix}open", "${ReminderPlanner.IdPrefix}soon") +
    (0 until ReminderPlanner.MaxDailyReminders).map { "${ReminderPlanner.IdPrefix}day_$it" }

// Down to the second: without it a reminder under a minute away would match the current minute and never fire.
private val CalendarUnits = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
    NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond

actual object LocalReminders {
    actual fun replaceAll(reminders: List<ScheduledReminder>, channelName: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        cancelAll()
        for (reminder in reminders) {
            val content = UNMutableNotificationContent().apply {
                setTitle(reminder.title)
                setBody(reminder.body)
                setSound(UNNotificationSound.defaultSound)
                reminder.feature?.let { setUserInfo(mapOf<Any?, Any?>(FeatureKey to it.name)) }
            }
            val date = NSDate.dateWithTimeIntervalSince1970(reminder.fireAt / 1000.0)
            val components = NSCalendar.currentCalendar.components(CalendarUnits, fromDate = date)
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
            val request = UNNotificationRequest.requestWithIdentifier(reminder.id, content, trigger)
            center.addNotificationRequest(request) { error ->
                if (error != null) AppLog.w("Reminders", "Couldn't schedule ${reminder.id}: ${error.localizedDescription}")
            }
        }
    }

    actual fun cancelAll() {
        UNUserNotificationCenter.currentNotificationCenter().removePendingNotificationRequestsWithIdentifiers(AllIds)
    }
}

actual fun platformDayClock(): DayClock = object : DayClock {
    private val calendar get() = NSCalendar.currentCalendar

    override fun minuteOfDay(epochMs: Long): Int {
        val components = calendar.components(CalendarUnits, fromDate = NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0))
        return (components.hour * 60 + components.minute).toInt()
    }

    override fun atMinuteOfDay(dayOf: Long, minuteOfDay: Int): Long {
        val day = NSDate.dateWithTimeIntervalSince1970(dayOf / 1000.0)
        val date = calendar.dateBySettingHour(
            (minuteOfDay / 60).toLong(),
            minute = (minuteOfDay % 60).toLong(),
            second = 0,
            ofDate = day,
            options = 0u,
        ) ?: return dayOf
        return (date.timeIntervalSince1970 * 1000).toLong()
    }
}
