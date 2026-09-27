package org.example.project.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import org.example.project.ui.common.LocalNavAnimatedScope
import org.example.project.ui.common.LocalSharedTransitionScope
import org.example.project.ui.adjust.AdjustScreen
import org.example.project.ui.auto.AutoScreen
import org.example.project.ui.blur.BlurScreen
import org.example.project.ui.collage.CollageEditorScreen
import org.example.project.ui.crop.CropScreen
import org.example.project.ui.draw.DrawScreen
import org.example.project.ui.editor.EditorScreen
import org.example.project.ui.emoji.EmojiScreen
import org.example.project.ui.filter.FilterScreen
import org.example.project.ui.frame.FrameScreen
import org.example.project.ui.frames.FramesScreen
import org.example.project.ui.freestyle.FreestyleEditorScreen
import org.example.project.ui.gallery.GalleryScreen
import org.example.project.ui.home.HomeScreen
import org.example.project.ui.language.LanguageScreen
import org.example.project.ui.onboarding.OnboardingScreen
import org.example.project.ui.overlay.OverlayScreen
import org.example.project.ui.premium.PremiumScreen
import org.example.project.ui.projects.PreviewScreen
import org.example.project.ui.proeditor.ProEditorScreen
import org.example.project.ui.colorsplash.ColorSplashScreen
import org.example.project.ui.ratio.RatioScreen
import org.example.project.ui.rotate.RotateScreen
import org.example.project.ui.home.CollageMaxSelection
import org.example.project.ui.save.SaveImageScreen
import org.example.project.ui.share.ShareImageScreen
import org.example.project.ui.settings.SettingsScreen
import org.example.project.ui.shapereveal.SelectiveBlurScreen
import org.example.project.ui.shapereveal.SelectiveSplashScreen
import org.example.project.ui.splash.SplashScreen
import org.example.project.ui.templates.TemplatesEditorScreen
import org.example.project.ui.templates.TemplatesScreen
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

    // One scope around the whole NavDisplay so an element can morph between two entries (the
    // Projects grid thumbnail into the full-screen Preview, a Templates grid cell into its editor).
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                sharedTransitionScope = this,
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
                            onGetStarted = {
                                backStack.add(Destination.Language())
                                backStack.remove(Destination.Splash)
                            },
                        )
                    }

                    entry<Destination.Language> { key ->
                        LanguageScreen(
                            showBack = key.fromSettings,
                            onBack = { backStack.removeLastOrNull() },
                            onDone = {
                                if (key.fromSettings) {
                                    backStack.removeLastOrNull()
                                } else {
                                    backStack.add(Destination.Onboarding)
                                    backStack.remove(key)
                                }
                            },
                        )
                    }

                    entry<Destination.Onboarding> {
                        OnboardingScreen(
                            onFinish = {
                                backStack.add(Destination.Home)
                                backStack.remove(Destination.Onboarding)
                            },
                        )
                    }

                    entry<Destination.Home> {
                        WithNavAnimatedScope {
                            HomeScreen(
                                onOpenGallery = { maxSelection, target ->
                                    backStack.add(Destination.Gallery(maxSelection, target))
                                },
                                onOpenTemplates = { backStack.add(Destination.Templates) },
                                onOpenFrames = { backStack.add(Destination.Frames) },
                                onOpenSettings = { backStack.add(Destination.Settings) },
                                onOpenPremium = { backStack.add(Destination.Premium) },
                                onOpenProject = { imagePath -> backStack.add(Destination.Preview(imagePath)) },
                            )
                        }
                    }

                    entry<Destination.Preview> { key ->
                        WithNavAnimatedScope {
                            PreviewScreen(imagePath = key.imagePath, onBack = { backStack.removeLastOrNull() })
                        }
                    }

                    entry<Destination.Settings> {
                        SettingsScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onOpenLanguage = { backStack.add(Destination.Language(fromSettings = true)) },
                            onOpenPremium = { backStack.add(Destination.Premium) },
                        )
                    }

                    entry<Destination.Premium>(metadata = slideUpMetadata()) {
                        PremiumScreen(onClose = { backStack.removeLastOrNull() })
                    }

                    entry<Destination.ProEditor>(metadata = slideUpMetadata()) {
                        ProEditorScreen(onBack = { backStack.removeLastOrNull() })
                    }

                    entry<Destination.Templates>(metadata = slideUpMetadata()) {
                        WithNavAnimatedScope {
                            TemplatesScreen(
                                onBack = { backStack.removeLastOrNull() },
                                onOpenEditor = { frame -> backStack.add(Destination.TemplatesEditor(frame)) },
                            )
                        }
                    }

                    entry<Destination.TemplatesEditor> { key ->
                        WithNavAnimatedScope {
                            TemplatesEditorScreen(
                                frame = key.frame,
                                onBack = { backStack.removeLastOrNull() },
                                // The baked template is already in the session; the template editor stays
                                // underneath so Back from the editor returns to it for further tweaks.
                                onDone = { backStack.add(Destination.Editor()) },
                            )
                        }
                    }

                    entry<Destination.Frames>(metadata = slideUpMetadata()) {
                        WithNavAnimatedScope {
                            FramesScreen(
                                onBack = { backStack.removeLastOrNull() },
                                onOpenEditor = { frame -> backStack.add(Destination.FramesEditor(frame)) },
                            )
                        }
                    }

                    entry<Destination.FramesEditor> { key ->
                        WithNavAnimatedScope {
                            TemplatesEditorScreen(
                                frame = key.frame,
                                title = "Frames",
                                onBack = { backStack.removeLastOrNull() },
                                // Same as Templates: the baked frame is in the session and the frame editor
                                // stays underneath, so Back from the editor returns to it.
                                onDone = { backStack.add(Destination.Editor()) },
                            )
                        }
                    }

                    entry<Destination.Gallery>(metadata = slideUpMetadata()) { key ->
                        GalleryScreen(
                            maxSelection = key.maxSelection,
                            onBack = { backStack.removeLastOrNull() },
                            onImagesSelected = { imagePaths ->
                                when (key.target) {
                                    GalleryTarget.Editor -> backStack.add(Destination.Editor(imagePaths.first()))
                                    GalleryTarget.EditorFilter ->
                                        backStack.add(Destination.Editor(imagePaths.first(), openFilter = true))
                                    GalleryTarget.Collage -> backStack.add(Destination.CollageEditor(imagePaths))
                                    GalleryTarget.Freestyle -> backStack.add(Destination.FreestyleEditor(imagePaths))
                                }
                                backStack.remove(key)
                            },
                        )
                    }

                    entry<Destination.CollageEditor> { key ->
                        CollageEditorScreen(
                            imagePaths = key.imagePaths,
                            onBack = { backStack.removeLastOrNull() },
                            // The baked collage is already in the session; the collage stays underneath so
                            // Back from the editor returns to it for further layout tweaks.
                            onOpenEditor = { backStack.add(Destination.Editor()) },
                        )
                    }

                    entry<Destination.FreestyleEditor> { key ->
                        FreestyleEditorScreen(
                            imagePaths = key.imagePaths,
                            onBack = { backStack.removeLastOrNull() },
                            // Same as Collage: the baked canvas is in the session, and the freestyle stays
                            // underneath so Back from the editor returns to it for further tweaks.
                            onOpenEditor = { backStack.add(Destination.Editor()) },
                        )
                    }

                    entry<Destination.Editor> { key ->
                        EditorScreen(
                            imagePath = key.imagePath,
                            openFilterOnLoad = key.openFilter,
                            onBack = { backStack.removeLastOrNull() },
                            onDone = { backStack.add(Destination.SaveImage) },
                            onOpenAuto = { backStack.add(Destination.Auto) },
                            onOpenCrop = { backStack.add(Destination.Crop) },
                            onOpenFilter = { backStack.add(Destination.Filter) },
                            onOpenAdjust = { backStack.add(Destination.Adjust) },
                            onOpenOverlay = { backStack.add(Destination.Overlay) },
                            onOpenRatio = { backStack.add(Destination.Ratio) },
                            onOpenText = { backStack.add(Destination.Text) },
                            onOpenEmoji = { backStack.add(Destination.Emoji) },
                            onOpenBlur = { backStack.add(Destination.Blur) },
                            onOpenSplash = { backStack.add(Destination.ColorSplash) },
                            onOpenSelectiveBlur = { backStack.add(Destination.SelectiveBlur) },
                            onOpenSelectiveSplash = { backStack.add(Destination.SelectiveSplash) },
                            onOpenFrame = { backStack.add(Destination.Frame) },
                            onOpenDraw = { backStack.add(Destination.Draw) },
                            onOpenRotate = { backStack.add(Destination.Rotate) },
                        )
                    }

                    entry<Destination.Auto>(metadata = slideUpMetadata()) {
                        AutoScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onApplied = { backStack.removeLastOrNull() },
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

                    entry<Destination.ColorSplash>(metadata = slideUpMetadata()) {
                        ColorSplashScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onApplied = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Destination.SelectiveBlur>(metadata = slideUpMetadata()) {
                        SelectiveBlurScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onApplied = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Destination.SelectiveSplash>(metadata = slideUpMetadata()) {
                        SelectiveSplashScreen(
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

                    entry<Destination.SaveImage>(metadata = slideUpMetadata()) {
                        SaveImageScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onSaved = { imagePath -> backStack.add(Destination.ShareImage(imagePath)) },
                            onOpenPremium = { backStack.add(Destination.Premium) },
                        )
                    }

                    entry<Destination.ShareImage> { key ->
                        ShareImageScreen(
                            imagePath = key.imagePath,
                            onBack = { backStack.removeLastOrNull() },
                            onHome = { backStack.popToHome() },
                            onNewCollage = {
                                backStack.popToHome()
                                backStack.add(Destination.Gallery(CollageMaxSelection, GalleryTarget.Collage))
                            },
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
    }
}

/** Exposes this entry's animated scope to [org.example.project.ui.common.navSharedElement]. */
@Composable
private fun WithNavAnimatedScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedScope provides LocalNavAnimatedContentScope.current, content = content)
}

/** Drops every entry above [Destination.Home], so finishing a flow lands back on the home screen. */
private fun MutableList<NavKey>.popToHome() {
    val home = indexOf(Destination.Home)
    if (home == -1) {
        clear()
        add(Destination.Home)
    } else {
        while (size > home + 1) removeLastOrNull()
    }
}
