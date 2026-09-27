package org.example.project.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Navigation 3 persists the back stack as a polymorphic list of [NavKey]s. Outside of Android there
 * is no reflection to fall back on, so every destination has to be registered explicitly.
 *
 * Remember to add new [Destination]s here, otherwise restoring the back stack fails at runtime.
 */
private val navKeySerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(Destination.Splash::class, Destination.Splash.serializer())
        subclass(Destination.Language::class, Destination.Language.serializer())
        subclass(Destination.Onboarding::class, Destination.Onboarding.serializer())
        subclass(Destination.Home::class, Destination.Home.serializer())
        subclass(Destination.Settings::class, Destination.Settings.serializer())
        subclass(Destination.Premium::class, Destination.Premium.serializer())
        subclass(Destination.Gallery::class, Destination.Gallery.serializer())
        subclass(Destination.Editor::class, Destination.Editor.serializer())
        subclass(Destination.CollageEditor::class, Destination.CollageEditor.serializer())
        subclass(Destination.FreestyleEditor::class, Destination.FreestyleEditor.serializer())
        subclass(Destination.ProEditor::class, Destination.ProEditor.serializer())
        subclass(Destination.Templates::class, Destination.Templates.serializer())
        subclass(Destination.TemplatesEditor::class, Destination.TemplatesEditor.serializer())
        subclass(Destination.Frames::class, Destination.Frames.serializer())
        subclass(Destination.FramesEditor::class, Destination.FramesEditor.serializer())
        subclass(Destination.Auto::class, Destination.Auto.serializer())
        subclass(Destination.Crop::class, Destination.Crop.serializer())
        subclass(Destination.Filter::class, Destination.Filter.serializer())
        subclass(Destination.Adjust::class, Destination.Adjust.serializer())
        subclass(Destination.Overlay::class, Destination.Overlay.serializer())
        subclass(Destination.Ratio::class, Destination.Ratio.serializer())
        subclass(Destination.Text::class, Destination.Text.serializer())
        subclass(Destination.Emoji::class, Destination.Emoji.serializer())
        subclass(Destination.Blur::class, Destination.Blur.serializer())
        subclass(Destination.ColorSplash::class, Destination.ColorSplash.serializer())
        subclass(Destination.SelectiveBlur::class, Destination.SelectiveBlur.serializer())
        subclass(Destination.SelectiveSplash::class, Destination.SelectiveSplash.serializer())
        subclass(Destination.Frame::class, Destination.Frame.serializer())
        subclass(Destination.Draw::class, Destination.Draw.serializer())
        subclass(Destination.SaveImage::class, Destination.SaveImage.serializer())
        subclass(Destination.Rotate::class, Destination.Rotate.serializer())
        subclass(Destination.ShareImage::class, Destination.ShareImage.serializer())
        subclass(Destination.Preview::class, Destination.Preview.serializer())
    }
}

internal val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = navKeySerializersModule
}
