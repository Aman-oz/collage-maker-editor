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
(`onBack`, `onOpenEditor`, `onApplied`, …) down into the screen.

Adding a destination requires **three** edits, and missing the third fails only at runtime when the
back stack is restored:

1. A `@Serializable` object/class in `navigation/Destination.kt`.
2. An `entry<Destination.X>` in `AppNavDisplay` (pass `metadata = slideUpMetadata()` for a screen
   that slides up over what is underneath, like the gallery or save screen).
3. A `subclass(...)` registration in `navigation/NavigationSerialization.kt` — outside Android there
   is no reflection, so polymorphic `NavKey` serialization must be declared explicitly.

ViewModels are scoped to their nav entry via `rememberViewModelStoreNavEntryDecorator`, so a
ViewModel is cleared when its entry is popped.

### Image state — `ImageEditSession`, not navigation arguments

`data/ImageEditSession.kt` is a Koin singleton holding one `StateFlow<ImageBitmap?>`: the working
copy of the photo. Only `Destination.Editor` carries data (`imagePath`, the picked file's platform
path). The other flows that edit that photo (background remover, Set Background, save) are
`data object` destinations that read the current bitmap from the session and write the result back
with `session.set(...)`. `EditorViewModel` collects the session flow and records every new bitmap
as one undo step, so whoever writes to the session never has to know history exists.

### Editor tools — inline, not destinations

The photo editor's tools (Auto, Crop, Filter, … Frame) are **not** destinations: they open inside
`EditorScreen`. `EditorViewModel.toolSession` holds a `ToolSession` (the tool plus a snapshot of the
photo taken when it opened) — in the ViewModel so the tool is still open after the paywall has
covered the editor — and `EditorContent` an `InlineToolHostState` (`ui/common/ToolScaffold.kt`), which runs the
two-beat transition: the editor's toolbar drops off the bottom and its top bar fades, then the
tool's panel slides up in the toolbar's place while the photo moves — once — to where the tool's
stage shows it, and the stage fades in on top. Closing runs it in reverse. ✓ hands the baked bitmap
to `EditorViewModel.applyEdit` (a `session.set`), ✕ and system back just close the tool.

Every tool is laid out with `ToolScaffold { Box(Modifier.toolStage()…) { … }; ToolPanel(title,
onClose, onDone) { controls } }`. Inside the editor the scaffold reads `LocalInlineToolHost` and
lets the host drive the stage's alpha and the panel's slide; on its own (previews) it is an
ordinary opaque column. A stage that pads the photo by something other than `ToolStageInset`
(Crop, Ratio, Rotate) passes `photoInset`, so the editor's photo lands exactly under it.

The host uses coroutine-driven `Animatable`s rather than `AnimatedContent` on purpose: a tool's
first composition can be heavy (blurring, desaturating, thumbnails), and an animation started on
that frame skips ahead by however long it took. The tool is mounted hidden *between* the two beats,
while nothing is moving. Keep heavy work out of the frames where something slides.

Five filters (`PhotoFilter.isPremium`) and seven overlays (`OverlayPreset.isPremium`, picked by
position in `PremiumOverlayPositions`) are premium, with a crown on the chip: anyone can preview
them, but Done sends a non-subscriber to `Destination.Premium` instead of applying. The selection is
`rememberSaveable` so it survives that trip. Adjust is a softer offer: the first Done with
`PremiumAdjustmentCount` (3) or more adjustments off zero opens the paywall for a non-subscriber,
and the next Done applies whether or not they subscribed. Its values and undo stacks are saveable
for the same reason. Text makes the same one-time offer when the edit is a premium one
(`isPremiumTextEdit`: the Stylish font, the red colour, a size set within 30–50, or text gone back
into after it was written), and Sticker when more than `FreeEmojiLimit` (2) emojis are placed.

The editor's own Done makes that one-time offer too, when more than `FreeEditCount` (3) tool edits
are applied (`EditorViewModel.consumePremiumOffer`, counted from the undo history).

The collage editor follows the same two patterns on its own Done: a premium layout is a hard gate
(`CollageEditorViewModel.needsPremium`), and a free layout with a border width of 2–5 or the 4:5
ratio (`isPremiumCollageEdit`) gets the one-time offer (`consumePremiumOffer`). The freestyle
editor's Done makes the one-time offer too (`isPremiumFreestyle`: more than 4 photos, more than 2
stickers, or a text label in the premium red or Stylish font), and so does the PIP editor's, by
the same rule applied to its text and sticker layers. Set Background does as well
(`SetBackgroundEdit.isPremium`: that rule, or a backdrop from the Gradient tab).

The background remover's crop step still uses the full-screen `CropContent`; it shares
`CropState` / `CropStage` with the editor's `CropTool`.

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
- **Freestyle** (`ui/freestyle/`): the picked photos open spread over a grid by the pure
  `scatterPlacements` (`FreestyleLayerOps.kt`), not stacked. The canvas fill and a
  text label's fill are both a `FreestyleFill` (solid or gradient) whose `brush` the preview and
  the bake draw; labels use `BasicText`, since Material's `Text` would flatten a gradient brush.
  A label can also carry a `background` fill, drawn by `drawTextPlate` (`ui/text/TextBaking.kt`)
  in both the preview and the bake; the Text panel's words/background switch
  (`TextColorTargetSwitch`) picks which one the colour row edits. The photo editor's Text tool has
  the same switch and plate, with solid colours only.
- **Custom colours**: every colour row or grid (text, text background, Draw, Frame, and the
  collage, freestyle and Set Background backgrounds) leads with a colour-wheel swatch
  (`ColorPickerFill`) that opens `ColorPickerDialog` through `rememberColorPickerLauncher`
  (`ui/common/ColorPicker.kt`, HSV maths in `ColorPickerMath.kt`). A new colour list should too.
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
against a `viewModel { (arg: T) -> ... }` definition. The editor's tools have no ViewModels: `EditorViewModel` is the only one in
that flow.

### Feature package layout

Each tool lives in `shared/src/commonMain/kotlin/org/example/project/ui/<tool>/` and follows a
consistent split — keep new tools in this shape, since it is what makes the logic testable:

- `<Tool>Screen.kt` — an internal `<Tool>Tool(sourceImage, onClose, onApply)` entry point the
  editor calls (it bakes on Done and hands the bitmap to `onApply`), and a private
  `<Tool>Content(...)` that is pure state-in/lambdas-out, laid out with `ToolScaffold`. The
  `@Preview` at the bottom renders `Content` inside `ThemePreviews { }` (light and dark side by
  side). Screens outside the editor are a public `<Name>Screen(...)` taking nav lambdas +
  `koinViewModel()` instead.
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

### Localization — `tr("English text")`, not string resources

The app is translated into the 15 languages of the language screen, and the language is the app's
own setting (`AppSettings.languageCode` → `i18n/AppLocale.code`), not the platform locale: the
pick has to apply at once on both platforms without a restart, which neither platform's resource
locale allows. So there are no `strings.xml` / `Res.string`: every user-visible string is written
in English at its call site and wrapped in `tr(...)` (`i18n/Localization.kt`). `AppLocale.code` is
snapshot state, so whatever called `tr` during composition recomposes when the language changes.

- The English text is the key. `i18n/AppStrings.kt` lists every key and
  `i18n/translations/Translations<Xx>.kt` holds one `Map<English, translation>` per language.
  `LocalizationTest` fails if a table is missing a key, has an extra one, or drops a `{0}`
  placeholder — so a new string means: add it to `AppStrings` and to all 14 tables.
- Arguments are `{0}`, `{1}` (`tr("{0} of {1}", a, b)`), filled after translating so a language
  can reorder them. Don't build sentences by concatenating translated fragments.
- `tr` is a plain function, so click handlers and ViewModels can call it. Never call it where the
  result is *kept* — an enum constructor, a top-level `val`, a `remember`: that string would
  survive a language change. Keep the English there and translate where it is shown (enums do this
  with `val label get() = tr(englishLabel)`; data lists with `tr(item.label)` at the `Text`).
- Text that isn't a key (server category names, exception messages) passes through unchanged.
- Layout stays left-to-right in every language, including Arabic and Urdu: the editor's custom
  canvases and sliders are not mirrored-layout safe yet.

### Photo picking — two mechanisms

- **Entry pick** (from Home): the in-app `Destination.Gallery` screen, backed by the
  `gallery/` expect/actuals (`rememberGalleryAccessState`, `loadGalleryPhotos`,
  `resolveGalleryImagePath`). It *does* request photo-library permission. `Limited` access is
  treated like `Granted`. The first cell of its Photos grid is a camera (FileKit
  `rememberCameraPickerLauncher`, the system camera app, so Android needs no `CAMERA` permission;
  iOS needs `NSCameraUsageDescription`). A single-photo pick goes straight on with the shot; a
  multi-photo pick keeps the shots in `GalleryViewModel.capturedPhotos`, shown selected right after
  the camera cell, until Done. A captured photo's id is its cache-file path, so its thumbnail comes
  from `loadCapturedPhotoThumbnail`, not the library's `loadGalleryThumbnail`.
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
