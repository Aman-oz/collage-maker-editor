package org.example.project.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Which editor a [Destination.Gallery] pick should be routed to once images are selected. */
@Serializable
enum class GalleryTarget {
    Editor,

    /** The photo editor with the Filter tool opened on top as soon as the image is loaded. */
    EditorFilter,
    Collage,
    Freestyle,

    /** The background remover: crop first, then [Destination.BackgroundRemoverEditor]. */
    BackgroundRemover,

    /** The Pip editor for [Destination.Gallery.pipTemplate]; one photo per window of the template. */
    Pip,
}

/**
 * Every screen the app can navigate to.
 *
 * Keys must be [Serializable] so that Navigation 3 can save and restore the back stack across
 * configuration changes and process death.
 */
@Serializable
sealed interface Destination : NavKey {

    /**
     * First screen shown on launch: a short loading screen that replaces itself with [Language] on
     * the first run (until onboarding is finished), and afterwards with [Premium] (or [Home] for a
     * premium user).
     */
    @Serializable
    data object Splash : Destination

    /**
     * Language picker.
     *
     * @param fromSettings `true` when opened from [Settings]: Done (or back) returns there. `false`
     * for the first-run flow after [Splash], where Done continues to [Onboarding].
     */
    @Serializable
    data class Language(val fromSettings: Boolean = false) : Destination

    /** Feature walkthrough pager shown after [Language]; Get Started on the last page (or close) goes to [Premium]. */
    @Serializable
    data object Onboarding : Destination

    /** Landing screen with the entry points into the different editors. */
    @Serializable
    data object Home : Destination

    /** App settings: language, theme, rate/share and the about/legal links. Opened from [Home]. */
    @Serializable
    data object Settings : Destination

    /**
     * Premium paywall: feature list and plan picker. Opened from [Home], [Settings], [SaveImage]
     * and the [Editor] (its own Done, and Done in its Filter, Overlay, Adjust, Text and Sticker tools).
     *
     * @param fromSplash `true` in the launch flow (after [Onboarding] on the first run, straight from
     * [Splash] on later ones), where nothing is under it on the back stack: close (or back) replaces
     * it with [Home] instead of popping.
     */
    @Serializable
    data class Premium(val fromSplash: Boolean = false) : Destination

    /**
     * In-app photo library picker, gated behind an explicit permission request.
     *
     * @param maxSelection how many photos the user may pick.
     * @param target which editor destination the picked images are routed to.
     * @param pipTemplate the [org.example.project.ui.pip.PipTemplate.name] the photos are for, when
     * [target] is [GalleryTarget.Pip].
     */
    @Serializable
    data class Gallery(
        val maxSelection: Int,
        val target: GalleryTarget,
        val pipTemplate: String? = null,
    ) : Destination

    /**
     * Photo editor.
     *
     * @param imagePath platform path of the picked image. On Android this is a `content://` uri,
     * on iOS an absolute file path. Both round-trip through `PlatformFile(path)`. `null` means
     * "edit whatever is already in [org.example.project.data.ImageEditSession]" — used when a
     * collage is baked and handed straight to the editor, since it has no file on disk.
     * @param openFilter open [Filter] over the editor once the image is loaded (Home's Filters
     * shortcut). Back/Done from the filter then lands in the editor as usual.
     */
    @Serializable
    data class Editor(val imagePath: String? = null, val openFilter: Boolean = false) : Destination

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

    /** Pip picker: the bundled picture-in-picture templates. Opened from [Home]. */
    @Serializable
    data object Pip : Destination

    /**
     * Pip editor: [templateName]'s frame with the picked photos in its windows, plus text and stickers.
     *
     * @param templateName the [org.example.project.ui.pip.PipTemplate.name]; it names every asset,
     * so the key needn't carry the template itself.
     * @param imagePaths platform paths of the picked images, in slot order, as in [CollageEditor].
     */
    @Serializable
    data class PipEditor(val templateName: String, val imagePaths: List<String>) : Destination

    /** Frames catalog browser; the same UI as [Templates], fed by the server frames endpoints. */
    @Serializable
    data object Frames : Destination

    /** Editor for a frame picked in [Frames]. Carries the whole frame, like [TemplatesEditor]. */
    @Serializable
    data class FramesEditor(val frame: org.example.project.ui.templates.TemplateFrame) : Destination

    /** Save screen: previews the finished image and exports it to the gallery, optionally watermarked. */
    @Serializable
    data object SaveImage : Destination

    /**
     * Post-save screen: shows the exported image and offers sharing plus New Collage / Home.
     *
     * @param imagePath absolute path of the exported JPEG in the app cache — the exact file that
     * went to the gallery (watermarked or not), so sharing sends what was saved.
     */
    @Serializable
    data class ShareImage(val imagePath: String) : Destination

    /**
     * Full-screen view of a saved project, opened from Home's Projects tab with a shared-element
     * transition from its grid thumbnail.
     *
     * @param imagePath absolute path of the project's JPEG in the app's private files directory.
     */
    @Serializable
    data class Preview(val imagePath: String) : Destination

    /**
     * Background remover step 1: crop (with rotate/flip) the picked photo. Done puts the cropped
     * photo in [org.example.project.data.ImageEditSession] and opens [BackgroundRemoverEditor].
     *
     * @param imagePath platform path of the picked image, as in [Editor.imagePath].
     */
    @Serializable
    data class BackgroundRemoverCrop(val imagePath: String) : Destination

    /** Background remover step 2: erase the background. Reads/writes the working image via [org.example.project.data.ImageEditSession]. */
    @Serializable
    data object BackgroundRemoverEditor : Destination

    /**
     * Background remover step 3: pick what goes behind the transparent cut-out and add text and
     * stickers over it. Reads/writes the working image via [org.example.project.data.ImageEditSession].
     */
    @Serializable
    data object SetBackground : Destination

}
