package org.example.project.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Which editor a [Destination.Gallery] pick should be routed to once images are selected. */
@Serializable
enum class GalleryTarget { Editor, Collage, Freestyle }

/**
 * Every screen the app can navigate to.
 *
 * Keys must be [Serializable] so that Navigation 3 can save and restore the back stack across
 * configuration changes and process death.
 */
@Serializable
sealed interface Destination : NavKey {

    /** First screen shown on launch. Replaced by [Home] once the app is ready. */
    @Serializable
    data object Splash : Destination

    /** Landing screen with the entry points into the different editors. */
    @Serializable
    data object Home : Destination

    /**
     * In-app photo library picker, gated behind an explicit permission request.
     *
     * @param maxSelection how many photos the user may pick.
     * @param target which editor destination the picked images are routed to.
     */
    @Serializable
    data class Gallery(val maxSelection: Int, val target: GalleryTarget) : Destination

    /**
     * Photo editor.
     *
     * @param imagePath platform path of the picked image. On Android this is a `content://` uri,
     * on iOS an absolute file path. Both round-trip through `PlatformFile(path)`.
     */
    @Serializable
    data class Editor(val imagePath: String) : Destination

    /**
     * Collage editor.
     *
     * @param imagePaths platform paths of the picked images, in selection order. Each round-trips
     * through `PlatformFile(path)` the same way [Editor.imagePath] does.
     */
    @Serializable
    data class CollageEditor(val imagePaths: List<String>) : Destination

    /**
     * Freestyle editor — every picked image starts as a freely draggable/rotatable/scalable layer
     * on one open canvas, alongside stickers and text the user adds there.
     *
     * @param imagePaths platform paths of the picked images, in selection order. Each round-trips
     * through `PlatformFile(path)` the same way [Editor.imagePath] does.
     */
    @Serializable
    data class FreestyleEditor(val imagePaths: List<String>) : Destination

    /**
     * Showcase screen for the reusable [org.example.project.ui.glassnav.GlassBottomNav] module: a
     * scrollable grid of cards behind a floating, frosted-glass bottom navigation bar.
     */
    @Serializable
    data object ProEditor : Destination

    /** Server-driven template browser (categories + templates masonry grid). */
    @Serializable
    data object Templates : Destination

    /**
     * Template editor for a chosen [frame]: its decorative image with photo slots the user fills.
     * The whole [org.example.project.ui.templates.TemplateFrame] (URL + slot coordinates) rides in
     * the nav key, so it is [Serializable].
     */
    @Serializable
    data class TemplatesEditor(val frame: org.example.project.ui.templates.TemplateFrame) : Destination

    /** Crop tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Crop : Destination

    /** Filter tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Filter : Destination

    /** Adjust tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Adjust : Destination

    /** Overlay tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Overlay : Destination

    /** Ratio tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Ratio : Destination

    /** Text tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Text : Destination

    /** Emoji/sticker tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Emoji : Destination

    /** Blur tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Blur : Destination

    /** Color Splash (brush). Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object ColorSplash : Destination

    /** s-Blur (shape reveal). Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object SelectiveBlur : Destination

    /** s-Splash (shape reveal). Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object SelectiveSplash : Destination

    /** Frame tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Frame : Destination

    /** Draw tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Draw : Destination

    /** Rotate tool. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object Rotate : Destination
}
