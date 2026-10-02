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
        subclass(Destination.Pip::class, Destination.Pip.serializer())
        subclass(Destination.PipEditor::class, Destination.PipEditor.serializer())
        subclass(Destination.SaveImage::class, Destination.SaveImage.serializer())
        subclass(Destination.ShareImage::class, Destination.ShareImage.serializer())
        subclass(Destination.Preview::class, Destination.Preview.serializer())
        subclass(Destination.BackgroundRemoverCrop::class, Destination.BackgroundRemoverCrop.serializer())
        subclass(Destination.BackgroundRemoverEditor::class, Destination.BackgroundRemoverEditor.serializer())
        subclass(Destination.SetBackground::class, Destination.SetBackground.serializer())
    }
}

internal val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = navKeySerializersModule
}
