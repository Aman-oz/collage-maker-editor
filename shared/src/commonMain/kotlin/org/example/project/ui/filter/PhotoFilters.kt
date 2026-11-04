package org.example.project.ui.filter

import org.example.project.i18n.tr

/**
 * The Filter tool's tabs, the LAS app's filter groups. [All] lists every other tab's filters. Every
 * filter of an [isPremium] tab is premium, and the Shop's Filter tab offers that tab as a pack to
 * unlock.
 */
internal enum class FilterCategory(val englishLabel: String, val isPremium: Boolean = false) {
    All("All"),
    BlackWhite("B&W", isPremium = true),
    Vintage("Vintage"),
    Smooth("Smooth"),
    Cold("Cold", isPremium = true),
    Warm("Warm", isPremium = true),
    Legacy("Legacy"),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/**
 * One of the Filter tool's filters: [recipe] is a CGE filter string, which [parseFilterRecipe] and
 * [runFilterSteps] run. [id] (`"Cold/3"`) is what the tool saves its pick as and the layout editor
 * keeps; [number] is its place in its tab, which the chip shows after the tab's name.
 *
 * The "None" option is [None], with an empty recipe. An [isPremium] filter can be tried on the photo
 * by anyone, but only a subscriber can apply it: for everyone else Done opens the paywall instead.
 */
internal data class PhotoFilter(
    val category: FilterCategory,
    val number: Int,
    val recipe: String,
    val isPremium: Boolean = false,
) {
    val id: String get() = if (isNoneOption) NoneId else "${category.name}/$number"
    val isNoneOption: Boolean get() = recipe.isEmpty()

    /** "Cold 3", in the app's current language; read it where it is shown, never keep it. */
    val label: String get() = if (isNoneOption) tr("None") else "${category.label} $number"

    companion object {
        private const val NoneId = "None"
        val None = PhotoFilter(FilterCategory.All, 0, recipe = "")
    }
}

/**
 * Which filters of each free tab are premium (a premium tab's all are), by their place in that tab
 * (1 is the first, and a negative number counts from the end). The All tab shows the same filters,
 * crowns included.
 */
private val PremiumFilterPositions: Map<FilterCategory, List<Int>> = mapOf(
    FilterCategory.Vintage to listOf(2),
    FilterCategory.Smooth to listOf(2),
)

/**
 * The LAS app's filters (`FilterCodeAsset`), as its CGE strings, by tab. Left out: those whose
 * lookup picture the LAS app never shipped (it lists `filter_25`…`filter_87` but has only
 * `filter_1`…`filter_24`, so they showed the photo unchanged), its animated `@dynamic` ones and
 * `@special 21` (a preset CGE keeps no recipe for), and repeats within a tab.
 */
private val FilterRecipes: Map<FilterCategory, List<String>> = mapOf(
    FilterCategory.BlackWhite to listOf(
        "@adjust lut filters/filter_10.webp",
        "@adjust lut filters/filter_23.webp",
        "@adjust saturation 0 @adjust level 0 0.83921 0.8772",
        "@adjust saturation 0 @curve R(9, 13)(37, 13)(63, 23)(81, 43)(91, 58)(103, 103)(159, 239)(252, 242)G(3, 20)(29, 20)(56, 19)(77, 37)(107, 108)(126, 184)(137, 217)(150, 248)(182, 284)(255, 255)B(45, 17)(78, 51)(96, 103)(131, 202)(255, 255)",
        "@colormul mat 0.34 0.48 0.22 0.34 0.48 0.22 0.34 0.48 0.22 @curve R(0, 0)(9, 10)(47, 38)(87, 69)(114, 92)(134, 116)(175, 167)(218, 218)(255, 255)G(40, 0)(45, 14)(58, 34)(74, 55)(125, 118)(192, 205)(255, 255)B(0, 0)(15, 16)(37, 31)(71, 55)(108, 88)(159, 151)(204, 201)(255, 255)",
        "@colormul mat 0.34 0.48 0.22 0.34 0.48 0.22 0.34 0.48 0.22 @curve R(0, 29)(20, 48)(83, 103)(164, 166)(255, 239)G(0, 30)(30, 61)(66, 94)(151, 160)(255, 241)B(2, 48)(82, 93)(166, 143)(255, 199)",
        "@adjust hsv -1 -1 -1 -1 -1 -1",
        "@adjust hsv -0.7 0.5 -0.7 -0.7 -0.7 0.5 @pixblend ol 0.07059 0.60391 0.57254 1 25",
    ),
    FilterCategory.Vintage to listOf(
        "@curve R(0, 4)(39, 103)(134, 223)(242, 255)G(0, 3)(31, 85)(68, 155)(131, 255)(219, 255)B(0, 3)(42, 110)(114, 207)(255, 255)",
        "@curve R(40, 162)(108, 186)(142, 208)(193, 227)(239, 249)G(13, 7)(72, 87)(124, 150)(197, 206)(255, 255)B(8, 22)(57, 97)(112, 147)(184, 204)(255, 222)",
        "@adjust level 0.66 0.23 0.44",
        "@adjust colorbalance 0.99 0.52 -0.31",
        "@curve R(0, 0)(63, 101)(200, 84)(255, 255)G(0, 0)(86, 49)(180, 183)(255, 255)B(0, 0)(19, 17)(66, 41)(97, 92)(137, 156)(194, 211)(255, 255)RGB(0, 0)(82, 36)(160, 183)(255, 255)",
        "@vigblend overlay 255 0 0 255 100 0.12 0.54 0.5 0.5 3",
        "@adjust level 0.31 0.54 0.13",
        "@style edge 1 2 @curve RGB(0, 255)(255, 0) @adjust saturation 0 @adjust level 0.33 0.71 0.93",
        "@style emboss 1 2 2",
        "@style halftone 1.2",
        "@style haze 0.5 -0.14 1 0.8 1",
        "@style crosshatch 0.01 0.003",
        "@style sketch 0.9",
        "@adjust saturation 0 @curve R(9, 13)(37, 13)(63, 23)(81, 43)(91, 58)(103, 103)(159, 239)(252, 242)G(3, 20)(29, 20)(56, 19)(77, 37)(107, 108)(126, 184)(137, 217)(150, 248)(182, 284)(255, 255)B(45, 17)(78, 51)(96, 103)(131, 202)(255, 255)",
    ),
    FilterCategory.Smooth to listOf(
        "@adjust saturation 0 @curve R(9, 13)(37, 13)(63, 23)(81, 43)(91, 58)(103, 103)(159, 239)(252, 242)G(3, 20)(29, 20)(56, 19)(77, 37)(107, 108)(126, 184)(137, 217)(150, 248)(182, 284)(255, 255)B(45, 17)(78, 51)(96, 103)(131, 202)(255, 255)",
        "@curve R(5, 49)(85, 173)(184, 249)G(23, 35)(65, 76)(129, 145)(255, 199)B(74, 69)(158, 107)(255, 126)",
    ),
    FilterCategory.Cold to listOf(
        "@adjust lut filters/filter_1.webp",
        "@adjust lut filters/filter_2.webp",
        "@adjust lut filters/filter_4.webp",
        "@adjust lut filters/filter_7.webp",
        "@adjust lut filters/filter_16.webp",
        "@adjust lut filters/filter_17.webp",
        "@adjust lut filters/filter_24.webp",
        "@curve R(2, 2)(16, 30)(72, 112)(135, 185)(252, 255)G(2, 1)(30, 42)(55, 84)(157, 207)(238, 249)B(1, 0)(26, 17)(67, 106)(114, 165)(231, 250)",
        "@curve R(0, 0)(69, 93)(126, 160)(210, 232)(255, 255)G(0, 0)(36, 47)(135, 169)(250, 254)B(0, 0)(28, 30)(107, 137)(147, 206)(255, 255)",
        "@curve R(0, 4)(39, 103)(134, 223)(242, 255)G(0, 3)(31, 85)(68, 155)(131, 255)(219, 255)B(0, 3)(42, 110)(114, 207)(255, 255)",
        "@curve R(4, 35)(65, 82)(117, 148)(153, 208)(206, 255)G(13, 5)(74, 78)(109, 144)(156, 201)(250, 250)B(6, 37)(93, 104)(163, 184)(238, 222)(255, 237) @adjust hsv -0.2 -0.2 -0.44 -0.2 -0.2 -0.2",
        "@adjust hsv -0.4 -0.64 -1.0 -0.4 -0.88 -0.88 @curve R(0, 0)(119, 160)(255, 255)G(0, 0)(83, 65)(163, 170)(255, 255)B(0, 0)(147, 131)(255, 255)",
        "@curve R(0, 0)(71, 74)(164, 165)(255, 255) @pixblend overlay 0.357 0.863 0.882 1 40",
        "@curve G(0, 0)(101, 127)(255, 255) @pixblend colordodge 0.937 0.482 0.835 1 20",
        "@curve R(0, 0)(96, 61)(154, 177)(255, 255) @pixblend overlay 0.357 0.863 0.882 1 40",
        "@beautify bilateral 10 4 1 @style haze -0.5 -0.5 1 1 1 @curve RGB(0, 0)(94, 20)(160, 168)(255, 255) @curve R(0, 0)(129, 119)(255, 255)B(0, 0)(135, 151)(255, 255)RGB(0, 0)(146, 116)(255, 255)",
        "@beautify bilateral 100 3.5 2",
    ),
    FilterCategory.Warm to listOf(
        "@adjust lut filters/filter_5.webp",
        "@adjust lut filters/filter_6.webp",
        "@curve R(81, 3)(161, 129)(232, 253)G(91, 0)(164, 136)(255, 225)B(76, 0)(196, 162)(255, 225)",
        "@curve R(0, 0)(120, 96)(165, 255)G(90, 0)(131, 145)(172, 255)B(77, 0)(165, 167)(255, 255)",
        "@curve R(16, 0)(60, 45)(124, 124)(214, 255)G(18, 2)(91, 81)(156, 169)(213, 255)B(16, 0)(85, 74)(158, 171)(211, 255) @curve R(17, 0)(144, 150)(214, 255)G(16, 0)(61, 47)(160, 172)(215, 255)B(21, 2)(131, 135)(213, 255)",
        "@curve R(33, 0)(70, 32)(146, 143)(185, 204)(255, 255)G(22, 0)(103, 71)(189, 219)(255, 252)B(10, 0)(54, 29)(93, 66)(205, 220)(255, 255)",
        "@adjust hsv 0.3 -0.5 -0.3 0 0.35 -0.2 @curve R(0, 0)(111, 163)(255, 255)G(0, 0)(72, 56)(155, 190)(255, 255)B(0, 0)(103, 70)(212, 244)(255, 255)",
        "@curve R(5, 49)(85, 173)(184, 249)G(23, 35)(65, 76)(129, 145)(255, 199)B(74, 69)(158, 107)(255, 126)",
        "@curve R(39, 0)(93, 61)(130, 136)(162, 193)(208, 255)G(41, 0)(92, 61)(128, 133)(164, 197)(200, 250)B(0, 23)(125, 127)(255, 230)",
        "@curve R(0, 0)(69, 63)(105, 138)(151, 222)(255, 255)G(0, 0)(67, 51)(135, 191)(255, 255)B(0, 0)(86, 76)(150, 212)(255, 255)",
        "@curve R(0, 0)(117, 95)(155, 171)(179, 225)(255, 255)G(0, 0)(94, 66)(155, 176)(255, 255)B(0, 0)(48, 59)(141, 130)(255, 224)",
        "@curve B(0, 0)(68, 72)(149, 184)(255, 255) @pixblend screen 0.94118 0.29 0.29 1 20",
        "@adjust shadowhighlight -200 200",
    ),
    FilterCategory.Legacy to listOf(
        "@adjust lut filters/filter_3.webp",
        "@adjust lut filters/filter_9.webp",
        "@adjust lut filters/filter_11.webp",
        "@adjust lut filters/filter_12.webp",
        "@adjust lut filters/filter_13.webp",
        "@adjust lut filters/filter_14.webp",
        "@adjust lut filters/filter_15.webp",
        "@adjust lut filters/filter_18.webp",
        "@adjust lut filters/filter_19.webp",
        "@adjust lut filters/filter_20.webp",
        "@adjust lut filters/filter_21.webp",
        "@adjust lut filters/filter_22.webp",
        "@adjust hsl 0.02 -0.31 -0.17 @curve R(0, 28)(23, 45)(117, 148)(135, 162)G(0, 8)(131, 152)(255, 255)B(0, 17)(58, 80)(132, 131)(127, 131)(255, 225)",
        "@adjust saturation 0 @curve R(0, 49)(16, 44)(34, 56)(74, 120)(120, 185)(151, 223)(255, 255)G(0, 46)(34, 73)(85, 129)(111, 164)(138, 192)(170, 215)(255, 255)B(0, 77)(51, 101)(105, 143)(165, 182)(210, 213)(250, 229)",
        "@vigblend mix 10 10 30 255 91 0 1.0 0.5 0.5 3 @curve R(0, 31)(35, 75)(81, 139)(109, 174)(148, 207)(255, 255)G(0, 24)(59, 88)(105, 146)(130, 171)(145, 187)(180, 214)(255, 255)B(0, 96)(63, 130)(103, 157)(169, 194)(255, 255)",
        "@curve R(0, 64)(16, 13)(58, 128)(108, 109)(162, 223)(255, 255)G(0, 30)(22, 35)(42, 58)(56, 86)(70, 119)(130, 184)(189, 212)B(6, 36)(76, 157)(107, 192)(173, 229)(255, 255)",
        "@adjust saturation 0 @curve R(0, 68)(10, 72)(42, 135)(72, 177)(98, 201)(220, 255)G(0, 29)(12, 30)(57, 127)(119, 203)(212, 255)(254, 239)B(0, 36)(54, 118)(66, 141)(119, 197)(155, 215)(255, 254)",
        "@curve R(42, 2)(53, 52)(80, 102)(100, 123)(189, 196)(255, 255)G(55, 74)(75, 98)(95, 114)(177, 197)(203, 212)(221, 220)(229, 234)(240, 249)B(0, 132)(81, 188)(180, 251)",
        "@curve R(0, 0)(135, 147)(255, 255)G(0, 0)(135, 147)(255, 255)B(0, 0)(135, 147)(255, 255) @adjust saturation 0.71 @adjust brightness -0.05 @curve R(19, 0)(45, 36)(88, 90)(130, 125)(200, 170)(255, 255)G(18, 0)(39, 26)(71, 74)(147, 160)(255, 255)B(0, 0)(77, 58)(136, 132)(255, 204)",
        "@curve R(9, 0)(49, 62)(124, 155)(218, 255)G(10, 0)(30, 33)(137, 169)(223, 255)B(10, 0)(37, 45)(96, 122)(150, 182)(221, 255)",
        "@curve R(9, 0)(26, 7)(155, 108)(194, 159)(255, 253)G(9, 0)(50, 19)(218, 194)(255, 255)B(0, 0)(29, 9)(162, 116)(218, 194)(255, 255)",
        "@curve R(14, 0)(51, 42)(135, 138)(191, 202)(234, 255)G(11, 6)(78, 77)(178, 185)(242, 250)B(11, 0)(22, 10)(72, 60)(171, 162)(217, 209)(255, 255)",
        "@curve R(5, 8)(36, 51)(115, 145)(201, 220)(255, 255)G(6, 9)(67, 83)(169, 190)(255, 255)B(3, 3)(55, 60)(177, 190)(255, 255)",
        "@curve R(4, 4)(38, 38)(146, 146)(201, 202)(255, 255)G(0, 0)(80, 74)(192, 187)(255, 255)B(0, 0)(58, 58)(183, 184)(255, 255)",
        "@curve R(17, 0)(37, 18)(75, 52)(238, 255)G(16, 0)(53, 32)(113, 92)(236, 255)B(16, 0)(80, 57)(171, 164)(235, 255)",
        "@curve R(15, 0)(45, 37)(92, 103)(230, 255)G(19, 0)(34, 22)(138, 158)(228, 252)B(19, 0)(74, 63)(159, 166)(230, 255)",
        "@curve R(3, 0)(23, 29)(83, 116)(167, 206)(255, 255)G(5, 0)(56, 64)(160, 189)(255, 255)B(3, 0)(48, 49)(142, 167)(248, 255)",
        "@curve R(40, 40)(86, 148)(255, 255)G(0, 28)(67, 140)(142, 214)(255, 255)B(0, 100)(103, 176)(195, 174)(255, 255) @adjust hsv 0.32 0 -0.5 -0.2 0 -0.4",
        "@adjust hsv -0.5 -0.5 -0.5 -0.5 -0.5 -0.5 @curve R(0, 0)(129, 148)(255, 255)G(0, 0)(92, 77)(175, 189)(255, 255)B(0, 0)(163, 144)(255, 255)",
        "@adjust hsv -0.8 0 -0.8 -0.8 0.5 -0.8 @pixblend ol 0.78036 0.70978 0.09018 1 28",
        "@adjust hsv -0.7 0.5 -0.7 -0.7 0 0 @pixblend ol 0.2941 0.55292 0.06665 1 25",
        "@adjust hsv -0.7 -0.7 0.5 -0.7 -0.7 0.5 @pixblend ol 0.243 0.07059 0.59215 1 25",
        "@curve R(18, 0)(67, 63)(104, 152)(128, 255)G(23, 4)(87, 106)(132, 251)B(17, 0)(67, 63)(108, 174)(128, 251)",
        "@curve R(48, 56)(82, 129)(130, 206)(214, 255)G(7, 37)(64, 111)(140, 190)(232, 220)B(2, 97)(114, 153)(229, 172)",
        "@curve R(0, 4)(255, 244)G(0, 0)(255, 255)B(0, 84)(255, 194)",
        "@curve R(15, 0)(92, 133)(255, 234)G(0, 20)(105, 128)(255, 255)B(0, 0)(120, 132)(255, 214)",
        "@curve R(0, 0)(35, 71)(153, 197)(255, 255)G(0, 15)(16, 36)(109, 132)(255, 255)B(0, 23)(181, 194)(255, 230)",
        "@curve R(0, 0)(43, 77)(56, 104)(100, 166)(255, 255)G(0, 0)(35, 53)(255, 255)B(0, 0)(110, 123)(255, 212)",
        "@adjust saturation 0.7 @pixblend screen 1 0.243 0.69 1 30",
        "@adjust saturation 0.7 @pixblend screen 0.8112 0.243 1 1 40",
        "@curve R(0, 0)(53, 28)(172, 203)(255, 255)",
        "@curve B(0, 0)(70, 87)(140, 191)(255, 255) @pixblend pinlight 0.247 0.49 0.894 1 20",
        "@curve G(0, 0)(101, 127)(255, 255) @pixblend colordodge 0.937 0.482 0.835 1 20",
        "@curve R(0, 0)(149, 145)(255, 255)G(0, 0)(149, 145)(255, 255)B(0, 0)(149, 145)(255, 255) @pixblend colordodge 0.937 0.482 0.835 1 20",
        "@curve R(0, 0)(152, 183)(255, 255)G(0, 0)(161, 133)(255, 255) @pixblend overlay 0.357 0.863 0.882 1 40",
        "@curve R(0, 0)(96, 61)(154, 177)(255, 255) @pixblend overlay 0.357 0.863 0.882 1 40",
        "@curve B(0, 0)(68, 72)(149, 184)(255, 255) @pixblend screen 0.94118 0.29 0.29 1 20",
        "@curve G(0, 0)(144, 166)(255, 255) @pixblend screen 0.94118 0.29 0.29 1 20",
        "@curve R(0, 0)(71, 74)(164, 165)(255, 255) @pixblend screen 0.94118 0.29 0.29 1 20",
        "@curve R(0, 0)(71, 74)(164, 165)(255, 255) @pixblend overlay 0.357 0.863 0.882 1 40",
        "@adjust colorbalance 0.99 0.52 -0.31",
        "@adjust sharpen 10 1.5",
        "@adjust exposure 0.98",
        "@style halftone 1.2",
        "@style edge 1 2 @curve RGB(0, 255)(255, 0)",
    ),)

/** Every filter tab but [FilterCategory.All], in order. */
internal val PhotoFiltersByCategory: Map<FilterCategory, List<PhotoFilter>> =
    FilterRecipes.mapValues { (category, recipes) ->
        val premium = PremiumFilterPositions[category].orEmpty()
            .map { if (it > 0) it else recipes.size + it + 1 }
            .toSet()
        recipes.mapIndexed { index, recipe -> PhotoFilter(category, index + 1, recipe, isPremium = category.isPremium || index + 1 in premium) }
    }

/** A tab's filters, "None" first. */
internal fun filtersOf(category: FilterCategory): List<PhotoFilter> =
    listOf(PhotoFilter.None) + when (category) {
        FilterCategory.All -> PhotoFiltersByCategory.values.flatten()
        else -> PhotoFiltersByCategory[category].orEmpty()
    }

/** The filter saved as [id] (see [PhotoFilter.id]), or null when there is none by it any more. */
internal fun photoFilterById(id: String?): PhotoFilter? = when (id) {
    null -> null
    PhotoFilter.None.id -> PhotoFilter.None
    else -> PhotoFiltersByCategory.values.firstNotNullOfOrNull { list -> list.firstOrNull { it.id == id } }
}
