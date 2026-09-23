package org.example.project.di

import org.example.project.Platform
import org.example.project.data.ImageEditSession
import org.example.project.data.network.NetworkImageLoader
import org.example.project.data.network.createHttpClient
import org.example.project.getPlatform
import org.example.project.ui.collage.CollageCatalog
import org.example.project.ui.adjust.AdjustViewModel
import org.example.project.ui.blur.BlurViewModel
import org.example.project.ui.collage.CollageEditorViewModel
import org.example.project.ui.crop.CropViewModel
import org.example.project.ui.draw.DrawViewModel
import org.example.project.ui.editor.EditorViewModel
import org.example.project.ui.emoji.EmojiViewModel
import org.example.project.ui.filter.FilterViewModel
import org.example.project.ui.frame.FrameViewModel
import org.example.project.ui.freestyle.FreestyleEditorViewModel
import org.example.project.ui.gallery.GalleryViewModel
import org.example.project.ui.home.HomeViewModel
import org.example.project.ui.overlay.OverlayViewModel
import org.example.project.ui.ratio.RatioViewModel
import org.example.project.ui.reveal.RevealEditViewModel
import org.example.project.ui.rotate.RotateViewModel
import org.example.project.ui.templates.TemplateFrame
import org.example.project.ui.templates.TemplatesEditorViewModel
import org.example.project.ui.templates.TemplatesRepository
import org.example.project.ui.templates.TemplatesViewModel
import org.example.project.ui.text.TextViewModel
import org.example.project.ui.splash.SplashViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Platform independent singletons. */
val coreModule: Module = module {
    single<Platform> { getPlatform() }
    // Shared working copy of the photo being edited, so the editor and its tool screens (crop,
    // filter, ...) stay in sync without passing image data through navigation arguments.
    single { ImageEditSession() }
    // Ktor client + loader for the remote collage layout thumbnails (collages.json preview URLs).
    single { createHttpClient() }
    single { NetworkImageLoader(get()) }
    // Loads the bundled collages.json layout catalog.
    single { CollageCatalog() }
    // Fetches the server template catalog (categories + templates) for the Templates screen.
    single { TemplatesRepository(get()) }
}

/** ViewModels, scoped to their Navigation 3 entry. */
val viewModelModule: Module = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::GalleryViewModel)
    // The image path comes from the navigation key, so it is passed in as a runtime parameter.
    viewModel { (imagePath: String) -> EditorViewModel(imagePath, get()) }
    // The image paths come from the navigation key, so they are passed in as a runtime parameter.
    viewModel { (imagePaths: List<String>) -> CollageEditorViewModel(imagePaths, get(), get()) }
    viewModel { (imagePaths: List<String>) -> FreestyleEditorViewModel(imagePaths, get()) }
    viewModelOf(::CropViewModel)
    viewModelOf(::FilterViewModel)
    viewModelOf(::AdjustViewModel)
    viewModelOf(::OverlayViewModel)
    viewModelOf(::RatioViewModel)
    viewModelOf(::TextViewModel)
    viewModelOf(::EmojiViewModel)
    viewModelOf(::BlurViewModel)
    viewModelOf(::FrameViewModel)
    viewModelOf(::DrawViewModel)
    viewModelOf(::RotateViewModel)
    // Shared by the Splash / s-Blur / s-Splash reveal tools (one scoped instance per destination).
    viewModelOf(::RevealEditViewModel)
    viewModelOf(::TemplatesViewModel)
    // The selected template comes from the navigation key, passed in as a runtime parameter.
    viewModel { (frame: TemplateFrame) -> TemplatesEditorViewModel(frame, get(), get()) }
}

val appModules: List<Module> = listOf(coreModule, viewModelModule)
