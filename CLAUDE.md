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

### Networking

`data/network/`: one shared Ktor `HttpClient` (`createHttpClient`, which names no engine: OkHttp
comes from `androidMain` deps and Darwin from `iosMain`, so Ktor auto-selects). `NetworkImageLoader`
is a small in-house URL→`ImageBitmap` memo cache, not an image library. It decodes bytes through the
`expect fun decodeImageBitmap` because FileKit only decodes files on disk. Compose code uses
`ui/common/NetworkImage.kt`.

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
session, on Done.

### Drawing gotcha

Use `copyBitmap` / `drawImageScaled` from `ui/common/ImageBitmapDrawing.kt` rather than
`DrawScope.drawImage`. Platform-decoded bitmaps (a photo straight off disk) do not redraw reliably
as the source of a scaling draw; copying once through an in-memory canvas fixes it.

### Theming

Two palettes coexist deliberately: `ui/theme/Theme.kt` (`AppTheme`, Material 3 light/dark) for
splash and home, and the hardcoded dark chrome in `ui/common/EditorPalette.kt`
(`EditorBackground`, `EditorAccent`, …) for the editor and all tool screens, which are always dark.
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
