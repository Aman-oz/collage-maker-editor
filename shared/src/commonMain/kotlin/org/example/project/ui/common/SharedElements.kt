package org.example.project.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** The app-wide [SharedTransitionScope] wrapping the `NavDisplay`; `null` in previews. */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * The animated scope of the nav entry being composed. Navigation 3's own
 * `LocalNavAnimatedContentScope` throws when read outside a `NavDisplay`, which would break
 * `@Preview`s, so entries that take part in shared transitions re-provide it here as nullable.
 */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Marks this element as shared under [key]: when navigating between two entries that both show a
 * [navSharedElement] with the same key, it morphs from one's bounds to the other's. A no-op when the
 * scopes are absent (previews).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.navSharedElement(key: Any): Modifier {
    val transitionScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedScope.current ?: return this
    return with(transitionScope) {
        this@navSharedElement.sharedElement(rememberSharedContentState(key), animatedScope)
    }
}

/** Shared-element key for a saved project's image, used by the Projects grid and the preview. */
fun projectImageKey(path: String): String = "project-image:$path"

/** Shared-element key for a template's frame, used by the Templates grid cell and the Templates editor canvas. */
fun templateFrameKey(id: String): String = "template-frame:$id"
