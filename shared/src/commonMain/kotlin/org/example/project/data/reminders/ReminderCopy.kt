package org.example.project.data.reminders

import org.example.project.i18n.tr

/** A reminder's notification text, already in the app's language. */
data class ReminderText(val title: String, val body: String)

/**
 * The words of every reminder. Each case has a couple of wordings and [round] picks one, so the same
 * user doesn't get the same sentence every day. All of it is English source text for [tr] (and listed
 * in AppStrings), translated when the reminder is scheduled, which happens while the app is open in
 * the user's language.
 */
object ReminderCopy {

    fun textFor(kind: ReminderKind, feature: ReminderFeature?, round: Int): ReminderText {
        val copy = feature?.let(::copyOf)
        val text = when {
            kind == ReminderKind.AppOpen || copy == null -> pick(OpenTitles, OpenBodies, round)
            kind == ReminderKind.Feature -> pick(copy.titles, copy.bodies, round)
            kind == ReminderKind.UnfinishedSoon -> copy.unfinishedTitle to SoonBodies.at(round)
            else -> NextDayTitles.at(round) to copy.nextDayBody
        }
        return ReminderText(tr(text.first), tr(text.second))
    }

    /** The Android notification channel's name. */
    const val ChannelName = "Reminders"

    private fun pick(titles: List<String>, bodies: List<String>, round: Int) = titles.at(round) to bodies.at(round)

    private fun List<String>.at(round: Int) = this[((round % size) + size) % size]

    private class FeatureCopy(
        val titles: List<String>,
        val bodies: List<String>,
        val unfinishedTitle: String,
        val nextDayBody: String,
    )

    private fun copyOf(feature: ReminderFeature): FeatureCopy = when (feature) {
        ReminderFeature.Collage -> Collage
        ReminderFeature.Freestyle -> Freestyle
        ReminderFeature.Templates -> Templates
        ReminderFeature.Frames -> Frames
        ReminderFeature.Pip -> Pip
        ReminderFeature.BgRemover -> BgRemover
        ReminderFeature.PhotoEditor, ReminderFeature.Filters -> Edit
        ReminderFeature.Layouts -> Layouts
    }

    private val OpenTitles = listOf("Your photos miss you 📸", "Got a minute to create? ✨")
    private val OpenBodies = listOf(
        "Turn today's moments into a beautiful collage in seconds.",
        "Your next favourite collage is just a few taps away.",
    )

    private val SoonBodies = listOf(
        "You were so close! Come back and save your masterpiece.",
        "Pick up where you left off and save it in one tap.",
    )
    private val NextDayTitles = listOf("Finish what you started 💜", "Still thinking about it? 💭")

    private val Collage = FeatureCopy(
        titles = listOf("Time for a new collage ✨", "Make a collage today 📸"),
        bodies = listOf(
            "Pick your favourite photos and we'll lay them out beautifully.",
            "Hundreds of layouts are ready for your latest shots.",
        ),
        unfinishedTitle = "Your collage isn't finished yet",
        nextDayBody = "Your collage is still waiting. Finish it and save it today.",
    )
    private val Freestyle = FeatureCopy(
        titles = listOf("Get creative 🎨", "Freestyle time ✂️"),
        bodies = listOf(
            "Drag, rotate and stack your photos any way you like.",
            "No rules, no grids: arrange your photos your way.",
        ),
        unfinishedTitle = "Your freestyle collage isn't finished yet",
        nextDayBody = "Your freestyle collage is still waiting. Finish it and save it today.",
    )
    private val Templates = FeatureCopy(
        titles = listOf("Fresh templates are waiting", "Try a new template 🌟"),
        bodies = listOf(
            "Drop your photos into a ready-made design.",
            "Beautiful designs that only need your photos.",
        ),
        unfinishedTitle = "Your template isn't finished yet",
        nextDayBody = "Your template is still waiting. Finish it and save it today.",
    )
    private val Frames = FeatureCopy(
        titles = listOf("Frame a memory 🖼️", "New frames to try"),
        bodies = listOf(
            "Give your best shot the frame it deserves.",
            "Turn a favourite photo into a framed keepsake.",
        ),
        unfinishedTitle = "Your framed photo isn't finished yet",
        nextDayBody = "Your framed photo is still waiting. Finish it and save it today.",
    )
    private val Pip = FeatureCopy(
        titles = listOf("Try a picture-in-picture", "Make it pop with PIP ✨"),
        bodies = listOf(
            "Put your photo inside a stunning PIP effect.",
            "One photo, two layers, a striking new look.",
        ),
        unfinishedTitle = "Your PIP photo isn't finished yet",
        nextDayBody = "Your PIP photo is still waiting. Finish it and save it today.",
    )
    private val BgRemover = FeatureCopy(
        titles = listOf("Make your photo pop", "Swap your background 🪄"),
        bodies = listOf(
            "Remove any background in one tap and pick a new one.",
            "Cut yourself out and drop in a whole new scene.",
        ),
        unfinishedTitle = "Your cutout isn't finished yet",
        nextDayBody = "Your cutout is still waiting. Finish it and save it today.",
    )
    private val Edit = FeatureCopy(
        titles = listOf("Your next edit awaits", "Give a photo a glow-up ✨"),
        bodies = listOf(
            "Filters, stickers and text to make your photo shine.",
            "One filter can change everything. Try a few today.",
        ),
        unfinishedTitle = "Your photo edit isn't finished yet",
        nextDayBody = "Your photo edit is still waiting. Finish it and save it today.",
    )
    private val Layouts = FeatureCopy(
        titles = listOf("New layouts to try", "Find your perfect layout"),
        bodies = listOf(
            "Pick a layout and fill it with your favourite shots.",
            "Grids, strips and shapes, ready for your photos.",
        ),
        unfinishedTitle = "Your layout isn't finished yet",
        nextDayBody = "Your layout is still waiting. Finish it and save it today.",
    )
}
