package org.example.project.data.reminders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderPlannerTest {

    /** UTC days, so the expected times are plain arithmetic. */
    private val clock = object : DayClock {
        override fun minuteOfDay(epochMs: Long): Int = ((epochMs % Day) / Minute).toInt()
        override fun atMinuteOfDay(dayOf: Long, minuteOfDay: Int): Long = dayOf - dayOf % Day + minuteOfDay * Minute
    }

    private fun at(day: Int, hour: Int, minute: Int = 0): Long = day * Day + hour * Hour + minute * Minute

    private fun open(state: UsageState, time: Long) = ReminderPlanner.onForeground(state, time, clock)

    @Test
    fun openOnly_remindsNextDayAtTheOpenTime() {
        val state = open(UsageState(), at(1, 20, 15))
        val plan = ReminderPlanner.plan(state, at(1, 20, 30), clock)
        assertEquals(listOf(ReminderKind.AppOpen), plan.map { it.kind })
        assertEquals(at(2, 20, 15), plan.single().fireAt)
    }

    @Test
    fun openingAtAboutTheSameTime_keepsTheTime() {
        var state = open(UsageState(), at(1, 20, 15)).copy(lastBackgroundAt = at(1, 20, 30))
        state = open(state, at(2, 20, 55)) // 40 min later in the day
        assertEquals(20 * 60 + 15, state.anchorMinute)
        assertEquals(at(3, 20, 15), ReminderPlanner.plan(state, at(2, 21, 0), clock).single().fireAt)
    }

    @Test
    fun openingAtAnotherTime_movesTheTime() {
        var state = open(UsageState(), at(1, 20, 15)).copy(lastBackgroundAt = at(1, 20, 30))
        state = open(state, at(2, 22, 15)) // 2 h later in the day
        assertEquals(22 * 60 + 15, state.anchorMinute)
    }

    @Test
    fun aQuickReturn_continuesTheSession() {
        var state = open(UsageState(), at(1, 20, 0))
        state = ReminderPlanner.onFeatureStarted(state, ReminderFeature.Collage, at(1, 20, 5))
        state = state.copy(lastBackgroundAt = at(1, 20, 10))
        state = open(state, at(1, 20, 20)) // back after an ad or a share sheet
        assertEquals(1, state.slots.size)
    }

    @Test
    fun featuresUsed_replaceTheOpenReminder_oneAtEachStartTime() {
        var state = open(UsageState(), at(1, 8, 0))
        state = ReminderPlanner.onFeatureStarted(state, ReminderFeature.Collage, at(1, 8, 5))
        state = ReminderPlanner.onImageSaved(state)
        state = ReminderPlanner.onFeatureStarted(state, ReminderFeature.BgRemover, at(1, 9, 0))
        state = ReminderPlanner.onImageSaved(state)
        val plan = ReminderPlanner.plan(state, at(1, 9, 30), clock)
        assertEquals(listOf(ReminderKind.Feature, ReminderKind.Feature), plan.map { it.kind })
        assertEquals(listOf(at(2, 8, 5), at(2, 9, 0)), plan.map { it.fireAt })
        assertEquals(listOf(ReminderFeature.Collage, ReminderFeature.BgRemover), plan.map { it.feature })
    }

    @Test
    fun startsCloseTogether_merge_andTheLaterWins() {
        var state = open(UsageState(), at(1, 8, 0))
        state = ReminderPlanner.onFeatureStarted(state, ReminderFeature.Collage, at(1, 8, 0))
        state = ReminderPlanner.onImageSaved(state)
        state = ReminderPlanner.onFeatureStarted(state, ReminderFeature.Frames, at(1, 8, 10))
        state = ReminderPlanner.onImageSaved(state)
        val plan = ReminderPlanner.plan(state, at(1, 8, 30), clock)
        assertEquals(listOf(ReminderFeature.Frames), plan.map { it.feature })
        assertEquals(at(2, 8, 10), plan.single().fireAt)
    }

    @Test
    fun atMostThreeADay_theMostRecentKept() {
        var state = open(UsageState(), at(1, 8, 0))
        val features = listOf(
            ReminderFeature.Collage, ReminderFeature.Freestyle, ReminderFeature.Templates,
            ReminderFeature.Frames, ReminderFeature.Pip,
        )
        features.forEachIndexed { index, feature ->
            state = ReminderPlanner.onFeatureStarted(state, feature, at(1, 8 + index))
            state = ReminderPlanner.onImageSaved(state)
        }
        val plan = ReminderPlanner.plan(state, at(1, 13, 0), clock)
        assertEquals(listOf(ReminderFeature.Templates, ReminderFeature.Frames, ReminderFeature.Pip), plan.map { it.feature })
    }

    @Test
    fun unfinished_remindsIn2Hours_andNextDayToFinish() {
        var state = open(UsageState(), at(1, 20, 0))
        state = ReminderPlanner.onFeatureStarted(state, ReminderFeature.Collage, at(1, 20, 5))
        val leftAt = at(1, 20, 20)
        val plan = ReminderPlanner.plan(state, leftAt, clock)
        assertEquals(listOf(ReminderKind.UnfinishedSoon, ReminderKind.UnfinishedNextDay), plan.map { it.kind })
        assertEquals(listOf(leftAt + 2 * Hour, at(2, 20, 5)), plan.map { it.fireAt })
        assertTrue(plan.all { it.feature == ReminderFeature.Collage })
    }

    @Test
    fun savedAfterStarting_hasNoUnfinishedReminder() {
        var state = open(UsageState(), at(1, 20, 0))
        state = ReminderPlanner.onFeatureStarted(state, ReminderFeature.Collage, at(1, 20, 5))
        state = ReminderPlanner.onImageSaved(state)
        val plan = ReminderPlanner.plan(state, at(1, 20, 20), clock)
        assertEquals(listOf(ReminderKind.Feature), plan.map { it.kind })
        assertNull(plan.firstOrNull { it.id.endsWith("soon") })
    }

    @Test
    fun nextDay_isNeverSoonerThan12Hours() {
        // Started at 23:50, left after midnight: "next day" is tonight at 23:50, not in 47 h.
        var state = open(UsageState(), at(1, 23, 40))
        state = ReminderPlanner.onFeatureStarted(state, ReminderFeature.Layouts, at(1, 23, 50))
        state = ReminderPlanner.onImageSaved(state)
        assertEquals(at(2, 23, 50), ReminderPlanner.plan(state, at(2, 0, 10), clock).single().fireAt)
    }

    @Test
    fun everyCase_hasText() {
        for (kind in ReminderKind.entries) {
            for (feature in ReminderFeature.entries) {
                for (round in 0..2) {
                    val text = ReminderCopy.textFor(kind, feature, round)
                    assertTrue(text.title.isNotBlank() && text.body.isNotBlank(), "$kind $feature")
                }
            }
        }
    }

    private companion object {
        const val Minute = 60_000L
        const val Hour = 60 * Minute
        const val Day = 24 * Hour
    }
}
