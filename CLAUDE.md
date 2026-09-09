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

### DI — Koin

`di/AppModule.kt` declares `coreModule` (Platform, `ImageEditSession`) and `viewModelModule`.
`di/Koin.kt`'s `initKoin` is idempotent and called from `CollageApplication` on Android (with
`androidContext`) and from `MainViewController` on iOS. Screens obtain ViewModels via
`koinViewModel()`; `EditorViewModel` needs the nav argument, so it uses
`koinViewModel { parametersOf(imagePath) }` against a `viewModel { (imagePath: String) -> ... }`
definition.

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

### Gallery picking

FileKit (`rememberFilePickerLauncher(type = FileKitType.Image)`) — Android photo picker /
iOS `PHPickerViewController`, neither requiring a runtime permission. The resulting `file.path`
(a `content://` uri on Android, an absolute path on iOS) round-trips through `PlatformFile(path)`
and is what `Destination.Editor` carries.

## Conventions

- Package root is `org.example.project` across all source sets.
- Dependencies go through the `gradle/libs.versions.toml` version catalog only.
- Kotlin/Compose Multiplatform artifacts are the `org.jetbrains.*` variants, not the AndroidX ones.
- Configuration cache and build cache are on; avoid Gradle config that breaks them.
- KDoc explains *why* on non-obvious code (transform math, platform workarounds). Match that.
