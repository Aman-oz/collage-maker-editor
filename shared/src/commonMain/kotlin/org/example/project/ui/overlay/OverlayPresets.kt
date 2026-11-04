package org.example.project.ui.overlay

import org.example.project.i18n.tr
import org.example.project.ui.filter.BlendKind
import org.example.project.ui.filter.FilterStep
import org.example.project.ui.filter.TextureBlendStep

/**
 * The Overlay tool's tabs: the LAS app's overlay groups (`EffectCodeAsset`), each one blend [mode]
 * over its own set of [count] textures, `files/<folder>/scr<n>.jpg`. Effect is the LAS app's
 * "Overlay" (screen over the light-leak textures); Burn is its "Burn", which is CGE's colour blend.
 *
 * Every overlay of an [isPremium] tab is premium, and the Shop's Overlay tab offers that tab as a
 * pack to unlock.
 */
internal enum class OverlayCategory(
    val englishLabel: String,
    val mode: BlendKind,
    val folder: String,
    val count: Int,
    val isPremium: Boolean = false,
) {
    Effect("Effect", BlendKind.Screen, "overlay", 23, isPremium = true),
    Dodge("Dodge", BlendKind.ColorDodge, "effect", 36, isPremium = true),
    Hardmix("Hardmix", BlendKind.HardMix, "effect", 36),
    Burn("Burn", BlendKind.Color, "effect", 36, isPremium = true),
    Divide("Divide", BlendKind.Divide, "effect", 36),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/**
 * An overlay: [category]'s texture [number] blended over the whole photo (scaled, keeping its shape,
 * to cover it), the way CGE's `krblend` does, by [steps].
 *
 * An [isPremium] overlay can be tried on the photo by anyone, but only a subscriber can apply it:
 * for everyone else Done opens the paywall instead. It is set from its category's
 * [OverlayCategory.isPremium] and, in a free category, from [PremiumOverlayPositions].
 */
internal data class OverlayPreset(
    val category: OverlayCategory,
    val number: Int,
    val isPremium: Boolean = false,
) {
    /** Unique across categories (numbers alone are not), and what the tool saves its selection as. */
    val key: String get() = "${category.name}/$number"

    val texturePath: String get() = "${category.folder}/scr$number.jpg"

    val steps: List<FilterStep> get() = listOf(TextureBlendStep(category.mode, texturePath, alpha = 1f))
}

/**
 * Which overlays of each free tab are premium, by their place among that tab's overlays (the "None" chip
 * is not counted): 1 is the first, and a negative number counts from the end, so -2 is the second
 * last.
 */
private val PremiumOverlayPositions: Map<OverlayCategory, List<Int>> = mapOf(
    OverlayCategory.Hardmix to listOf(2),
)

internal val OverlayPresets: List<OverlayPreset> = OverlayCategory.entries.flatMap { category ->
    val premium = PremiumOverlayPositions[category].orEmpty()
        .map { if (it > 0) it else category.count + it + 1 }
        .toSet()
    (1..category.count).map { number -> OverlayPreset(category, number, isPremium = category.isPremium || number in premium) }
}
