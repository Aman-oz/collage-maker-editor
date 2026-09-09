package org.example.project.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

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
