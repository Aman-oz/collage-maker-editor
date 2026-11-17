package org.example.project.data.reminders

import com.ozi.nextgen.compose.ads_kmp.OziAds
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import org.example.project.AppLog
import org.example.project.data.KeyValueStore
import org.example.project.i18n.tr

/**
 * Follows how the app is used and keeps the usage reminders ([ReminderPlanner] has the rules) up to
 * date: everything pending is cancelled when the app comes to the screen, and a new plan is scheduled
 * when it leaves. A Koin singleton, fed by App (foreground/background) and AppNavDisplay (feature
 * starts and saves).
 *
 * Remote Config `local_reminders_enabled_android` / `local_reminders_enabled_ios` (default true)
 * turns all of it off.
 */
class UsageTracker(
    private val store: KeyValueStore,
    private val clock: DayClock = platformDayClock(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private var state: UsageState = load()

    fun onAppForeground() {
        update(ReminderPlanner.onForeground(state, now(), clock))
        LocalReminders.cancelAll()
    }

    fun onFeatureStarted(feature: ReminderFeature) {
        update(ReminderPlanner.onFeatureStarted(state, feature, now()))
    }

    fun onImageSaved() {
        update(ReminderPlanner.onImageSaved(state))
    }

    fun onAppBackground() {
        val at = now()
        update(state.copy(lastBackgroundAt = at, copyRound = state.copyRound + 1))
        if (!OziAds.remoteBoolean("$EnabledKeyPrefix${OziAds.platformName}", default = true)) {
            LocalReminders.cancelAll()
            return
        }
        val reminders = ReminderPlanner.plan(state, at, clock).map { planned ->
            val text = ReminderCopy.textFor(planned.kind, planned.feature, state.copyRound)
            ScheduledReminder(planned.id, planned.fireAt, text.title, text.body, planned.feature)
        }
        LocalReminders.replaceAll(reminders, tr(ReminderCopy.ChannelName))
        AppLog.i(Tag, "Scheduled ${reminders.size}: " + reminders.joinToString { "${it.id} in ${(it.fireAt - at) / 60_000} min" })
    }

    private fun update(newState: UsageState) {
        state = newState
        store.putString(KeyState, json.encodeToString(UsageState.serializer(), newState))
    }

    private fun load(): UsageState = store.getString(KeyState)
        ?.let { runCatching { json.decodeFromString(UsageState.serializer(), it) }.getOrNull() }
        ?: UsageState()

    private companion object {
        const val KeyState = "usage_reminders_state"
        const val EnabledKeyPrefix = "local_reminders_enabled_"
        const val Tag = "Reminders"
        val json = Json { ignoreUnknownKeys = true }
    }
}
