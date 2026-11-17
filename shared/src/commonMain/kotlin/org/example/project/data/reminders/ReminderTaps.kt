package org.example.project.data.reminders

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The feature of the reminder the user last tapped, until the app has acted on it. Set by the
 * platforms (Android's MainActivity from the notification's intent, iOS's AppDelegate as
 * `ReminderTaps.shared.onTapped(featureName:)`), read by AppNavDisplay.
 */
object ReminderTaps {
    private val _tapped = MutableStateFlow<ReminderFeature?>(null)
    val tapped: StateFlow<ReminderFeature?> = _tapped.asStateFlow()

    fun onTapped(featureName: String?) {
        ReminderFeature.fromName(featureName)?.let { _tapped.value = it }
    }

    fun consume() {
        _tapped.value = null
    }
}
