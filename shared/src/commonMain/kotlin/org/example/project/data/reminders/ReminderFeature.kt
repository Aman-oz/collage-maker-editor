package org.example.project.data.reminders

import org.example.project.navigation.Destination
import org.example.project.navigation.GalleryTarget
import org.example.project.ui.home.CollageMaxSelection
import org.example.project.ui.home.FreestyleMaxSelection

/**
 * The features a usage reminder can be about. The names are stored (in [UsageTracker]'s state and in
 * a scheduled notification's payload), so renaming one drops the reminders already scheduled for it.
 */
enum class ReminderFeature {
    Collage,
    Freestyle,
    Templates,
    Frames,
    Pip,
    BgRemover,
    PhotoEditor,
    Filters,
    Layouts,
    ;

    /** Where tapping this feature's reminder takes the user from Home. */
    fun destination(): Destination = when (this) {
        Collage -> Destination.Gallery(CollageMaxSelection, GalleryTarget.Collage)
        Freestyle -> Destination.Gallery(FreestyleMaxSelection, GalleryTarget.Freestyle)
        Templates -> Destination.Templates()
        Frames -> Destination.Frames()
        Pip -> Destination.Pip
        BgRemover -> Destination.Gallery(1, GalleryTarget.BackgroundRemover)
        PhotoEditor -> Destination.Gallery(1, GalleryTarget.Editor)
        Filters -> Destination.Gallery(1, GalleryTarget.EditorFilter)
        Layouts -> Destination.Layouts
    }

    companion object {
        fun fromName(name: String?): ReminderFeature? = entries.firstOrNull { it.name == name }

        /**
         * The feature a screen starts, or null for every other screen. A feature starts when its own
         * editor opens: after the photo pick for most, straight away for Templates, Frames and Layouts.
         * The photo editor without a photo of its own is the last step of another feature, not a start.
         */
        fun startedBy(destination: Any?): ReminderFeature? = when (destination) {
            is Destination.CollageEditor -> Collage
            is Destination.FreestyleEditor -> Freestyle
            is Destination.TemplatesEditor -> Templates
            is Destination.FramesEditor -> Frames
            is Destination.PipEditor -> Pip
            is Destination.BackgroundRemoverCrop -> BgRemover
            is Destination.LayoutEditor -> Layouts
            is Destination.Editor -> when {
                destination.imagePath == null -> null
                destination.openFilter -> Filters
                else -> PhotoEditor
            }
            else -> null
        }
    }
}

