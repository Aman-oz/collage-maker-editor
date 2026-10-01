# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Kotlin Multiplatform photo editor targeting Android and iOS, with the entire UI shared via Compose
Multiplatform. Two Gradle modules: `:shared` (all UI, state, and logic) and `:androidApp` (a thin
Android host). `iosApp/` is the Xcode project that hosts the same Compose UI.

## Commands

```bash
./gradlew :androidApp:assembleDebug          # build Android debug APK
./gradlew :androidApp:installDebug           # install on connected device/emulator
./gradlew :shared:testAndroidHostTest        # run commonTest + androidHostTest on the JVM (fast loop)
./gradlew :shared:iosSimulatorArm64Test      # run commonTest + iosTest on the iOS simulator
./gradlew :shared:allTests                   # both of the above
./gradlew build                              # compile everything + all tests
```

Run a single test class or method (works on both test tasks):

```bash
./gradlew :shared:testAndroidHostTest --tests "org.example.project.ui.rotate.RotateMathTest"
./gradlew :shared:testAndroidHostTest --tests "*.RotateMathTest.nearestRotationStop_snapsToClosestStop"
```

iOS app: open `iosApp/iosApp.xcodeproj` in Xcode and run. Its build phase shells out to
`./gradlew :shared:embedAndSignAppleFrameworkForXcode`, so the `Shared` framework is always
rebuilt from Gradle — never hand-edit framework output.

There is no lint/ktlint/detekt setup; `check` only runs tests.

## Architecture

### Navigation — Navigation 3, no NavController

`navigation/AppNavDisplay.kt` is the single `NavDisplay` for the whole app. The back stack is a
plain observable list: `backStack.add(Destination.X)` to go forward, `backStack.removeLastOrNull()`
to go back. Screens never navigate themselves — every destination entry passes lambdas
(`onBack`, `onOpenCrop`, `onApplied`, …) down into the screen.

Adding a destination requires **three** edits, and missing the third fails only at runtime when the
back stack is restored:

1. A `@Serializable` object/class in `navigation/Destination.kt`.
2. An `entry<Destination.X>` in `AppNavDisplay` (tool screens pass `metadata = slideUpMetadata()`
   for the slide-up-over-the-editor transition).
3. A `subclass(...)` registration in `navigation/NavigationSerialization.kt` — outside Android there
   is no reflection, so polymorphic `NavKey` serialization must be declared explicitly.

ViewModels are scoped to their nav entry via `rememberViewModelStoreNavEntryDecorator`, so a
ViewModel is cleared when its entry is popped.

### Image state — `ImageEditSession`, not navigation arguments

`data/ImageEditSession.kt` is a Koin singleton holding one `StateFlow<ImageBitmap?>`: the working
copy of the photo. Only `Destination.Editor` carries data (`imagePath`, the picked file's platform
path). Every tool destination (`Crop`, `Filter`, `Rotate`, …) is a `data object` — it reads the
current bitmap from the session and writes the edited bitmap back with `session.set(...)`.
`EditorViewModel` collects the session flow, so any tool's result reflects back into the editor
automatically on pop.

### Multi-image editors — Collage, Freestyle, Templates

Besides the single-photo `Editor` + tools flow, three editors carry their inputs in the nav key:
`CollageEditor(imagePaths)`, `FreestyleEditor(imagePaths)` and `TemplatesEditor(frame)` (the whole
`@Serializable` `TemplateFrame` rides in the key). `Home` routes through
`Gallery(maxSelection, target: GalleryTarget)`, which pushes the matching editor and removes itself
from the back stack.

- **Collage** (`ui/collage/`): layout geometry is hand-coded, ported from the older Android "LAS"
  app. `frames/*FrameImage.kt` + `FrameImageUtils.createTemplateItems(name)` map a
  `collage_<count>_<index>.png` name to slot polygons (`geom/` has the `PhotoItem`/`TemplateItem`
  model). `CollageCatalog` joins that with the bundled `composeResources/files/collages.json`
  (preview URLs, premium flag) and drops entries with no generator. A new layout needs both a JSON
  entry and a generator case.
- **Templates** (`ui/templates/`): server-driven. `TemplatesRepository` hits the unauthenticated
  plain-HTTP LAS collagemaker API, which is why cleartext traffic is enabled in the Android
  manifest and iOS `Info.plist` (ATS). The server sends slot coordinates as quoted strings, which
  `LenientFloatSerializer` handles.
- **Frames** (`ui/frames/`): the same screens as Templates (`TemplatesContent`, `TemplatesEditorScreen`)
  fed by `FramesRepository`, which calls the same server's `getSubCategories` and
  `getassets?subcategoryId=` (the LAS `FramesActivity`). The payload is the same `FrameItem` shape,
  so it reuses the templates DTOs and their `toCategories()` / `toFrames()` mapping.

### PIP — `ui/pip/`

Ported from the LAS app's `TemplateActivity` / `TemplateDetailActivity`. Home's PIP opens
`Destination.Pip` (the template grid) → `Gallery(slotCount, GalleryTarget.Pip, pipTemplate = name)` →
`PipEditor(templateName, imagePaths)` → `Editor()`. The 46 templates are bundled in
`composeResources/files/pip/` as `<name>_preview.webp`, `<name>_fg.webp` (the transparent frame) and
one `<name>_<slot>.png` alpha mask per slot. `PipCatalog.kt` holds each slot's top-left corner in the
frame's pixel space; a slot's size is its mask's own size, so geometry is only known once the masks
are decoded (`PipAssetLoader`). `drawPip` renders the blurred first photo, then each photo masked by
`DstIn` in its own layer, then the frame, and is shared by the preview and the bake. The editor's text
and sticker layers are the freestyle ones, as in Set Background.

### Background remover — `ui/bgremover/`

Home's BG Remove goes through `Gallery(1, GalleryTarget.BackgroundRemover)` →
`BackgroundRemoverCrop(imagePath)` → `BackgroundRemoverEditor` → `SetBackground` → `Editor()`. The
crop step reuses `CropContent` from `ui/crop/` with `showTransformTools = true` (rotate/flip baked
into the working bitmap). The eraser replays an undoable list of `EraseOp`s over the original photo
inside an offscreen layer. Auto is a colour-based magic wand, not ML: a tap flood-fills the
connected region around it (the bottom slider, tinted with the tapped colour, sets the tolerance)
and erases or recovers it. The eraser's backdrop chip (each tap cycles checkerboard → white → dark
→ grey) is preview-only: Done hands a *transparent* cut-out to `ui/setbackground/`, which puts a
colour, gradient or picked photo behind it, lets the user add text and stickers over it, and
flattens it all (export is JPEG). The text/sticker layers are the freestyle editor's:
`ui/freestyle/FreestyleLayers.kt` (canvas, selection handles, keyboard text bar, Text/Stickers
panels) and `FreestyleLayerOps.kt` (pure layer edits) are internal and shared by both screens.

Ai Magic (premium) is server-side, ported from the LAS app: `data/bgremover/BackgroundRemoverApi`
uploads the cropped photo as multipart `file` to `<base>/bg-remover/remove`, downloads the returned
`image_url` PNG, and the eraser pushes it as an undoable `EraseOp.AiCutOut` (its alpha masks the
photo, so Recover still works). The endpoint and key are read from `local.properties`
(`BG_REMOVER_BASE_URL`, `BG_REMOVER_API_KEY_HEADER`, `BG_REMOVER_API_KEY`, or the LAS app's
`BASE_URL`, `KEY`, `X_API_KEY` as fallbacks) by the
`:shared:generateBgRemoverConfig` task into a generated `BgRemoverConfig` object — the KMP stand-in
for `BuildConfig`. Without them the app builds and Ai Magic reports "not set up".

### Networking

`data/network/`: one shared Ktor `HttpClient` (`createHttpClient`, which names no engine: OkHttp
comes from `androidMain` deps and Darwin from `iosMain`, so Ktor auto-selects). `NetworkImageLoader`
is a small in-house URL→`ImageBitmap` memo cache, not an image library. It decodes bytes through the
`expect fun decodeImageBitmap` because FileKit only decodes files on disk. Compose code uses
`ui/common/NetworkImage.kt`.

### Billing — `AppBillingWrapper`

`data/billing/` holds the subscriptions. `BillingProductIds` has the three store product IDs
(`weekly_subscription`, `monthly_subscription`, `yearly_subscription`), which must match Play
Console and App Store Connect exactly. `AppBillingWrapper` (a Koin singleton with
`createdAtStart = true`) loads prices, tracks the active plan and runs purchase/restore. It is the
only thing that writes `AppSettings.isPremium`, and only from a successful store answer, so an
offline launch keeps the cached state.

The store sits behind `BillingClientAdapter`, bound per platform by `billingPlatformModule()`:
- Android: `PlayBillingClientAdapter` (Play Billing 9). It needs the foreground Activity to launch
  a purchase, which its lifecycle tracker only sees if it is created before the first activity
  resumes. That is why the wrapper is eager.
- iOS: `StoreKitBillingAdapter` over the Kotlin `StoreKitBridge` interface, which is implemented
  in Swift on StoreKit 2 (`iosApp/iosApp/StoreKitBridge.swift`) and passed into
  `MainViewController(storeKitBridge:)`. StoreKit 2 is Swift-only, so that half can't live in Kotlin.

There is no server-side receipt verification yet.

### DI — Koin

`di/AppModule.kt` declares `coreModule` (Platform, `ImageEditSession`, the HttpClient,
`NetworkImageLoader`, `CollageCatalog`, `TemplatesRepository`) and `viewModelModule`.
`di/Koin.kt`'s `initKoin` is idempotent and called from `CollageApplication` on Android (with
`androidContext`) and from `MainViewController` on iOS. Screens obtain ViewModels via
`koinViewModel()`. ViewModels that need a nav argument (`EditorViewModel`, `CollageEditorViewModel`,
`FreestyleEditorViewModel`, `TemplatesEditorViewModel`) use `koinViewModel { parametersOf(...) }`
against a `viewModel { (arg: T) -> ... }` definition. `RevealEditViewModel` is shared by the
ColorSplash / SelectiveBlur / SelectiveSplash tools; each destination gets its own nav-scoped
instance.

### Feature package layout

Each tool lives in `shared/src/commonMain/kotlin/org/example/project/ui/<tool>/` and follows a
consistent split — keep new tools in this shape, since it is what makes the logic testable:

- `<Tool>Screen.kt` — a public `<Tool>Screen(...)` that takes nav lambdas + `koinViewModel()`, and
  a private `<Tool>Content(...)` that is pure state-in/lambdas-out. The `@Preview` at the bottom
  renders `Content` inside `ThemePreviews { }` (light and dark side by side).
- `<Tool>ViewModel.kt` — thin; usually just reads `session.image.value` and writes results back.
- `<Tool>Math.kt` — pure functions (snapping, normalization, geometry). **This is what the tests in
  `shared/src/commonTest/` cover**; there are no UI tests.
- `<Tool>Baking.kt` — renders the edit into a new full-resolution `ImageBitmap` via
  `Canvas(output)`, mirroring whatever transform the live preview shows with a `graphicsLayer`.

Transient per-screen editing state (including **undo/redo**, which is a pair of
`remember { mutableStateOf(emptyList<...>()) }` stacks of an immutable `<Tool>Edit` data class)
lives in the `Content` composable, not the ViewModel. Only the final baked bitmap reaches the
session, on Done. The exception is a screen that pushes a *next step* rather than popping (the
background eraser → Set Background): a covered nav entry leaves composition and loses `remember`
state, so `BackgroundRemoverEditorViewModel` holds the erase ops and snapshots its source photo, and
`SetBackgroundViewModel` holds its backdrop/layer history, so Back resumes the same edit.

### Drawing gotcha

Use `copyBitmap` / `drawImageScaled` from `ui/common/ImageBitmapDrawing.kt` rather than
`DrawScope.drawImage`. Platform-decoded bitmaps (a photo straight off disk) do not redraw reliably
as the source of a scaling draw; copying once through an in-memory canvas fixes it.

### Theming

Screens follow `ui/theme/Theme.kt` (`AppTheme`, Material 3 light/dark): tool screens use the
theme-aware `ToolTopBar`, and the photo/collage/freestyle editors each derive a small private
`*Chrome` from `MaterialTheme`. The hardcoded dark chrome in `ui/common/EditorPalette.kt`
(`EditorBackground`, `EditorAccent`, …) is now only used by the always-dark project Preview and
ProEditor showcase, and as `CenterFillSlider`'s default colors (every tool screen overrides them).
Shared editor widgets live in `ui/common/EditorControls.kt` and `EditorSlider.kt`.

### Photo picking — two mechanisms

- **Entry pick** (from Home): the in-app `Destination.Gallery` screen, backed by the
  `gallery/` expect/actuals (`rememberGalleryAccessState`, `loadGalleryPhotos`,
  `resolveGalleryImagePath`). It *does* request photo-library permission. `Limited` access is
  treated like `Granted`.
- **In-editor pick** (replacing a collage/template slot, adding a freestyle layer): FileKit
  `rememberFilePickerLauncher(type = FileKitType.Image)`, which needs no runtime permission.

Both produce the same path shape (a `content://` URI on Android, an absolute path on iOS). It
round-trips through `PlatformFile(path)` and is what the editor nav keys carry.

Compose resources are accessed via `photocollagemaker.shared.generated.resources.Res`.

## Conventions

- Package root is `org.example.project` across all source sets.
- Dependencies go through the `gradle/libs.versions.toml` version catalog only.
- Kotlin/Compose Multiplatform artifacts are the `org.jetbrains.*` variants, not the AndroidX ones.
- Configuration cache and build cache are on; avoid Gradle config that breaks them.
- KDoc explains *why* on non-obvious code (transform math, platform workarounds). Match that.
