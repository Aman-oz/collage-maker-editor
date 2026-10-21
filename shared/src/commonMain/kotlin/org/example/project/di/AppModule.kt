package org.example.project.di

import org.example.project.Platform
import org.example.project.data.AppSettings
import org.example.project.data.ImageEditSession
import org.example.project.data.SavedImageHandoff
import org.example.project.data.ProjectsRepository
import org.example.project.data.bgremover.BackgroundRemoverApi
import org.example.project.data.billing.AppBillingWrapper
import org.example.project.data.billing.billingPlatformModule
import org.example.project.data.centraldata.CentralDataApi
import org.example.project.data.createKeyValueStore
import org.example.project.data.fonts.FontLibrary
import org.example.project.data.fonts.FontLoader
import org.example.project.data.cache.AssetCache
import org.example.project.data.network.NetworkImageLoader
import org.example.project.data.network.createHttpClient
import org.example.project.getPlatform
import org.example.project.ui.collage.CollageCatalog
import org.example.project.ui.bgremover.BackgroundRemoverCropViewModel
import org.example.project.ui.bgremover.BackgroundRemoverEditorViewModel
import org.example.project.ui.setbackground.SetBackgroundViewModel
import org.example.project.ui.collage.CollageEditorViewModel
import org.example.project.ui.editor.EditorViewModel
import org.example.project.ui.frames.FramesRepository
import org.example.project.ui.frames.FramesViewModel
import org.example.project.ui.home.HomeFeedRepository
import org.example.project.ui.home.HomeFeedViewModel
import org.example.project.ui.layouts.LayoutsRepository
import org.example.project.ui.layouts.LayoutsViewModel
import org.example.project.ui.layouteditor.LayoutEditorViewModel
import org.example.project.ui.freestyle.FreestyleEditorViewModel
import org.example.project.ui.gallery.GalleryViewModel
import org.example.project.ui.language.LanguageViewModel
import org.example.project.ui.pip.PipAssetLoader
import org.example.project.ui.pip.PipEditorViewModel
import org.example.project.ui.pip.PipViewModel
import org.example.project.ui.premium.PremiumViewModel
import org.example.project.ui.projects.PreviewViewModel
import org.example.project.ui.projects.ProjectsViewModel
import org.example.project.ui.save.SaveImageViewModel
import org.example.project.ui.settings.SettingsViewModel
import org.example.project.ui.share.ShareImageViewModel
import org.example.project.ui.stickers.StickersRepository
import org.example.project.ui.templates.TemplateFrame
import org.example.project.ui.templates.TemplatesEditorViewModel
import org.example.project.ui.templates.TemplatesRepository
import org.example.project.ui.templates.TemplatesViewModel
import org.example.project.ui.onboarding.OnboardingViewModel
import org.example.project.ui.splash.SplashViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Platform independent singletons. */
val coreModule: Module = module {
    single<Platform> { getPlatform() }
    // Shared working copy of the photo being edited, so the editor and the screens around it (background
    // remover, save, ...) stay in sync without passing image data through navigation arguments.
    single { ImageEditSession() }
    single { SavedImageHandoff() }
    // Persisted user preferences (theme, language); App observes the theme from here.
    single { AppSettings(createKeyValueStore()) }
    // Ktor client + loader for the remote collage layout thumbnails (collage preview URLs).
    single { createHttpClient() }
    // The disk cache of the fonts and stickers downloaded on demand.
    single { AssetCache(get()) }
    single { NetworkImageLoader(get(), get()) }
    single { FontLoader(get(), get()) }
    // The AI background remover's upload/download client (endpoint from local.properties).
    single { BackgroundRemoverApi(get()) }
    // The central-data server (key from local.properties) behind the collage, template and frame
    // catalogs. Collages fall back to the bundled collages.json offline.
    single { CentralDataApi(get()) }
    single { CollageCatalog(get()) }
    // Fetch the template and frame catalogs (sub-categories + assets) from the central-data server.
    single { TemplatesRepository(get()) }
    single { FramesRepository(get()) }
    single { HomeFeedRepository(get()) }
    // The Layouts category (Home's Layouts row and its browser), joined with the collage catalog.
    single { LayoutsRepository(get(), get()) }
    // The editor's sticker library (categories, sub-categories, stickers), kept for the process.
    single { StickersRepository(get()) }
    // The user's saved creations, listed on Home's Projects tab.
    single { ProjectsRepository() }

    single { PipAssetLoader() }
    // The Text tool's fonts from the central-data server, with the user's favourite and recent fonts.
    single { FontLibrary(get(), createKeyValueStore()) }
    // Subscriptions. Created at startup so the entitlement check runs on every launch and, on
    // Android, so its activity tracker is registered before the first activity resumes.
    single(createdAtStart = true) { AppBillingWrapper(get(), get()) }
}

/** ViewModels, scoped to their Navigation 3 entry. */
val viewModelModule: Module = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::LanguageViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::GalleryViewModel)
    // The image path comes from the navigation key, so it is passed in as a runtime parameter.
    viewModel { (imagePath: String?) -> EditorViewModel(imagePath, get(), get(), get()) }
    // The image paths (and the layout to open on, from Home's Layouts) come from the navigation key.
    viewModel { params -> CollageEditorViewModel(params.get(), params.getOrNull<String>(), get(), get(), get()) }
    // The layout to open on comes from the navigation key.
    viewModel { (layoutName: String) -> LayoutEditorViewModel(layoutName, get(), get(), get(), get()) }
    viewModel { (imagePaths: List<String>) -> FreestyleEditorViewModel(imagePaths, get(), get()) }
    viewModel { (imagePath: String) -> BackgroundRemoverCropViewModel(imagePath, get()) }
    viewModel { BackgroundRemoverEditorViewModel(get(), get(), get()) }
    viewModelOf(::SetBackgroundViewModel)
    viewModelOf(::SaveImageViewModel)
    viewModelOf(::ProjectsViewModel)
    // The project file path comes from the navigation key, passed in as a runtime parameter.
    viewModel { (imagePath: String) -> PreviewViewModel(imagePath, get()) }
    // The exported file path comes from the navigation key, passed in as a runtime parameter.
    viewModel { (imagePath: String) -> ShareImageViewModel(imagePath, get()) }
    // Shared by the Splash / s-Blur / s-Splash reveal tools (one scoped instance per destination).
    // The category to open on comes from the navigation key (Home's View All), or is null.
    viewModel { params -> TemplatesViewModel(get(), params.getOrNull()) }
    viewModel { params -> FramesViewModel(get(), params.getOrNull()) }
    viewModelOf(::HomeFeedViewModel)
    viewModelOf(::LayoutsViewModel)
    viewModelOf(::PremiumViewModel)
    // The selected template comes from the navigation key, passed in as a runtime parameter.
    viewModel { (frame: TemplateFrame) -> TemplatesEditorViewModel(frame, get(), get(), get()) }
    viewModelOf(::PipViewModel)
    viewModel { (templateName: String, imagePaths: List<String>) -> PipEditorViewModel(templateName, imagePaths, get(), get(), get()) }
}

val appModules: List<Module> = listOf(coreModule, viewModelModule, billingPlatformModule())
