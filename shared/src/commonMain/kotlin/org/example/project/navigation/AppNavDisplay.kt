package org.example.project.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.example.project.ui.adjust.AdjustScreen
import org.example.project.ui.blur.BlurScreen
import org.example.project.ui.collage.CollageEditorScreen
import org.example.project.ui.crop.CropScreen
import org.example.project.ui.draw.DrawScreen
import org.example.project.ui.editor.EditorScreen
import org.example.project.ui.emoji.EmojiScreen
import org.example.project.ui.filter.FilterScreen
import org.example.project.ui.frame.FrameScreen
import org.example.project.ui.home.HomeScreen
import org.example.project.ui.overlay.OverlayScreen
import org.example.project.ui.ratio.RatioScreen
import org.example.project.ui.rotate.RotateScreen
import org.example.project.ui.splash.SplashScreen
import org.example.project.ui.text.TextScreen

/** How long a tool screen (crop, filter, ...) takes to slide up over / down off the editor. */
private const val ToolScreenTransitionMillis = 320

/** Slides a tool screen up from the bottom over whatever is underneath, and back down on pop. */
private fun slideUpMetadata(): Map<String, Any> = metadata {
    // Slide new content up, keeping the old content in place underneath.
    put(NavDisplay.TransitionKey) {
        slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(ToolScreenTransitionMillis),
        ) togetherWith ExitTransition.KeepUntilTransitionsFinished
    }
    // Slide old content down, revealing the content underneath.
    put(NavDisplay.PopTransitionKey) {
        EnterTransition.None togetherWith slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(ToolScreenTransitionMillis),
        )
    }
    put(NavDisplay.PredictivePopTransitionKey) {
        EnterTransition.None togetherWith slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(ToolScreenTransitionMillis),
        )
    }
}

/**
 * Single source of truth for navigation: a back stack of [Destination]s rendered by [NavDisplay].
 *
 * Navigating is plain list manipulation — `add` to go forward, `removeLastOrNull` to go back.
 */
@Composable
fun AppNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(navSavedStateConfiguration, Destination.Splash)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        // `rememberViewModelStoreNavEntryDecorator` scopes ViewModels to their NavEntry, so each
        // destination gets its own ViewModel that is cleared when the entry is popped.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Destination.Splash> {
                SplashScreen(
                    onFinished = {
                        backStack.add(Destination.Home)
                        backStack.remove(Destination.Splash)
                    },
                )
            }

            entry<Destination.Home> {
                HomeScreen(
                    onImagePicked = { imagePath ->
                        backStack.add(Destination.Editor(imagePath))
                    },
                    onCollageImagesPicked = { imagePaths ->
                        backStack.add(Destination.CollageEditor(imagePaths))
                    },
                )
            }

            entry<Destination.CollageEditor> { key ->
                CollageEditorScreen(
                    imagePaths = key.imagePaths,
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Editor> { key ->
                EditorScreen(
                    imagePath = key.imagePath,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenCrop = { backStack.add(Destination.Crop) },
                    onOpenFilter = { backStack.add(Destination.Filter) },
                    onOpenAdjust = { backStack.add(Destination.Adjust) },
                    onOpenOverlay = { backStack.add(Destination.Overlay) },
                    onOpenRatio = { backStack.add(Destination.Ratio) },
                    onOpenText = { backStack.add(Destination.Text) },
                    onOpenEmoji = { backStack.add(Destination.Emoji) },
                    onOpenBlur = { backStack.add(Destination.Blur) },
                    onOpenFrame = { backStack.add(Destination.Frame) },
                    onOpenDraw = { backStack.add(Destination.Draw) },
                    onOpenRotate = { backStack.add(Destination.Rotate) },
                )
            }

            entry<Destination.Crop>(metadata = slideUpMetadata()) {
                CropScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onCropped = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Filter>(metadata = slideUpMetadata()) {
                FilterScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Adjust>(metadata = slideUpMetadata()) {
                AdjustScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Overlay>(metadata = slideUpMetadata()) {
                OverlayScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Ratio>(metadata = slideUpMetadata()) {
                RatioScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Text>(metadata = slideUpMetadata()) {
                TextScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Emoji>(metadata = slideUpMetadata()) {
                EmojiScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Blur>(metadata = slideUpMetadata()) {
                BlurScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Frame>(metadata = slideUpMetadata()) {
                FrameScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Draw>(metadata = slideUpMetadata()) {
                DrawScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }

            entry<Destination.Rotate>(metadata = slideUpMetadata()) {
                RotateScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onApplied = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
