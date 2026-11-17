package org.example.project.data.reminders

import kotlin.math.abs
import kotlin.math.min
import kotlinx.serialization.Serializable

/** A feature started in the current session: when, and whether its image has been saved since. */
@Serializable
data class UsageSlot(val feature: ReminderFeature, val startedAt: Long, val saved: Boolean = false)

/** Everything [UsageTracker] remembers between launches. */
@Serializable
data class UsageState(
    /** The time of day (minutes after midnight) of the app-open reminder. */
    val anchorMinute: Int? = null,
    /** The features started in the current session, oldest first. */
    val slots: List<UsageSlot> = emptyList(),
    /** When the app last left the screen; a return within [ReminderPlanner.SessionGapMs] continues that session. */
    val lastBackgroundAt: Long? = null,
    /** Turns over on every plan, so each reminder picks a different wording than last time. */
    val copyRound: Int = 0,
)

enum class ReminderKind {
    /** No feature used: a nudge at the time the app is usually opened. */
    AppOpen,

    /** A feature that was used (and saved): another go at it, at the time it was started. */
    Feature,

    /** A feature left without saving: 2 h after leaving the app. */
    UnfinishedSoon,

    /** A feature left without saving: the next day, at the time it was started. */
    UnfinishedNextDay,
}

data class PlannedReminder(val id: String, val fireAt: Long, val kind: ReminderKind, val feature: ReminderFeature?)

/** Local time of day, which the planner needs and common code can't compute on its own. */
interface DayClock {
    /** Minutes after local midnight at [epochMs]. */
    fun minuteOfDay(epochMs: Long): Int

    /** The instant of [minuteOfDay] on the local calendar day that contains [dayOf]. */
    fun atMinuteOfDay(dayOf: Long, minuteOfDay: Int): Long
}

/**
 * The reminder rules, as pure functions over [UsageState]:
 *
 * 1. No feature used in a session: one reminder the next day at the app-open time. Opening the app
 *    within [SameTimeWindowMinutes] of that time keeps it; further away moves it to the new time.
 * 2. Features used: the app-open reminder is dropped, and each feature gets one at its start time.
 * 3. At most [MaxDailyReminders] a day: starts within [MergeWindowMs] of each other merge (the later
 *    one wins), and the most recent are kept.
 * 4. A feature started but not saved: the next-day reminder asks to finish it, and one more comes
 *    [UnfinishedSoonDelayMs] after leaving the app (for the latest such feature).
 */
object ReminderPlanner {
    const val SameTimeWindowMinutes = 60
    const val SessionGapMs = 30 * 60_000L
    const val MergeWindowMs = 30 * 60_000L
    const val MaxDailyReminders = 3
    const val UnfinishedSoonDelayMs = 2 * 60 * 60_000L

    /** A "next day" reminder is never sooner than this after leaving the app. */
    const val MinNextDayLeadMs = 12 * 60 * 60_000L

    private const val DayMs = 24 * 60 * 60_000L
    private const val MinutesPerDay = 24 * 60

    const val IdPrefix = "reminder_"

    /** The app came to the screen at [now]: a new session unless it left only moments ago. */
    fun onForeground(state: UsageState, now: Long, clock: DayClock): UsageState {
        val continues = state.lastBackgroundAt?.let { now - it in 0..SessionGapMs } ?: false
        if (continues) return state
        val minute = clock.minuteOfDay(now)
        val anchor = state.anchorMinute?.takeIf { minuteDistance(it, minute) <= SameTimeWindowMinutes } ?: minute
        return state.copy(anchorMinute = anchor, slots = emptyList())
    }

    fun onFeatureStarted(state: UsageState, feature: ReminderFeature, now: Long): UsageState =
        state.copy(slots = state.slots + UsageSlot(feature, now))

    /** An image was saved: the latest feature still open is done. */
    fun onImageSaved(state: UsageState): UsageState {
        val index = state.slots.indexOfLast { !it.saved }
        if (index < 0) return state
        return state.copy(slots = state.slots.toMutableList().also { it[index] = it[index].copy(saved = true) })
    }

    /** The reminders to schedule when the app leaves the screen at [now]. */
    fun plan(state: UsageState, now: Long, clock: DayClock): List<PlannedReminder> {
        val kept = mergeAndCap(state.slots)
        if (kept.isEmpty()) {
            val anchor = state.anchorMinute ?: clock.minuteOfDay(now)
            return listOf(PlannedReminder("${IdPrefix}open", nextDayAt(anchor, now, clock), ReminderKind.AppOpen, null))
        }
        val daily = kept.mapIndexed { index, slot ->
            PlannedReminder(
                id = "${IdPrefix}day_$index",
                fireAt = nextDayAt(clock.minuteOfDay(slot.startedAt), now, clock),
                kind = if (slot.saved) ReminderKind.Feature else ReminderKind.UnfinishedNextDay,
                feature = slot.feature,
            )
        }
        val soon = state.slots.lastOrNull { !it.saved }?.let { slot ->
            PlannedReminder("${IdPrefix}soon", now + UnfinishedSoonDelayMs, ReminderKind.UnfinishedSoon, slot.feature)
        }
        return (daily + listOfNotNull(soon)).sortedBy { it.fireAt }
    }

    internal fun mergeAndCap(slots: List<UsageSlot>): List<UsageSlot> {
        val merged = mutableListOf<UsageSlot>()
        for (slot in slots.sortedBy { it.startedAt }) {
            val last = merged.lastOrNull()
            if (last != null && slot.startedAt - last.startedAt < MergeWindowMs) merged[merged.lastIndex] = slot
            else merged += slot
        }
        return merged.takeLast(MaxDailyReminders)
    }

    /** The first time at [minuteOfDay] that is at least [MinNextDayLeadMs] after [now]. */
    internal fun nextDayAt(minuteOfDay: Int, now: Long, clock: DayClock): Long =
        (0..2).map { clock.atMinuteOfDay(now + it * DayMs, minuteOfDay) }
            .first { it >= now + MinNextDayLeadMs }

    internal fun minuteDistance(a: Int, b: Int): Int {
        val d = abs(a - b) % MinutesPerDay
        return min(d, MinutesPerDay - d)
    }
}
