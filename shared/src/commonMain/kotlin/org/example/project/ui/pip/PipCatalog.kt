package org.example.project.ui.pip

/** Folder under `composeResources/files` holding every PIP template's frame, masks and preview. */
private const val PipFolder = "files/pip"

/**
 * One photo window of a PIP template: the top-left corner ([x], [y]) where its mask is placed, in
 * the frame image's pixel space. The window's size is its mask image's own size, so it is only
 * known once the mask is decoded (see [slotRect]).
 */
data class PipSlot(val x: Int, val y: Int)

/**
 * A picture-in-picture template, ported from the LAS app's `TemplateImageUtils`: a transparent
 * frame ([framePath], e.g. a glass bottle) drawn over a blurred copy of the first photo, with each
 * photo showing through its slot's alpha mask ([maskPath]).
 *
 * [name] is `<group>_<number>`, where the group is the slot count minus one. It is also the key
 * [org.example.project.navigation.Destination.PipEditor] carries, since it names every asset.
 */
data class PipTemplate(val name: String, val slots: List<PipSlot>) {
    val previewPath: String get() = "$PipFolder/${name}_preview.webp"
    val framePath: String get() = "$PipFolder/${name}_fg.webp"
    fun maskPath(slotIndex: Int): String = "$PipFolder/${name}_$slotIndex.png"
}

private fun pip(name: String, vararg slots: Pair<Int, Int>) =
    PipTemplate(name, slots.map { (x, y) -> PipSlot(x, y) })

/** Every bundled PIP template, in the order the picker shows them (single-photo ones first). */
internal val PipTemplates: List<PipTemplate> = listOf(
    pip("0_0", 344 to 120),
    pip("0_1", 60 to 238),
    pip("0_2", 75 to 103),
    pip("0_3", 28 to 62),
    pip("0_4", 63 to 68),
    pip("0_5", 78 to 444),
    pip("0_6", 110 to 425),
    pip("0_7", 84 to 74),
    pip("0_8", 232 to 215),
    pip("0_9", 83 to 183),
    pip("0_10", 61 to 216),
    pip("0_11", 138 to 62),
    pip("0_12", 169 to 161),
    pip("0_13", 21 to 179),
    pip("0_14", 150 to 319),
    pip("0_15", 30 to 146),
    pip("0_16", 59 to 247),
    pip("0_17", 43 to 87),
    pip("0_18", 75 to 141),
    pip("0_19", 29 to 129),
    pip("0_20", 140 to 371),
    pip("1_0", 35 to 340, 350 to 340),
    pip("1_1", 88 to 197, 289 to 173),
    pip("1_2", 42 to 243, 301 to 145),
    pip("1_3", 22 to 202, 346 to 212),
    pip("1_4", 84 to 18, 96 to 330),
    pip("1_5", 61 to 151, 334 to 156),
    pip("1_6", 91 to 313, 316 to 313),
    pip("1_7", 8 to 231, 247 to 40),
    pip("1_8", 68 to 364, 316 to 425),
    pip("1_9", 87 to 378, 354 to 396),
    pip("1_10", 128 to 551, 336 to 566),
    pip("1_11", 87 to 365, 426 to 349),
    pip("1_12", 52 to 356, 336 to 15),
    pip("1_13", 98 to 333, 396 to 472),
    pip("1_14", 268 to 405, 89 to 691),
    pip("1_15", 79 to 596, 320 to 458),
    pip("1_16", 72 to 191, 285 to 315),
    pip("1_17", 37 to 177, 313 to 414),
    pip("1_18", 165 to 537, 308 to 248),
    pip("1_19", 70 to 611, 399 to 611),
    pip("2_0", 46 to 12, 173 to 249, 336 to 29),
    pip("2_1", 10 to 127, 282 to 0, 286 to 329),
    pip("2_2", 8 to 3, 306 to 143, 110 to 290),
    pip("2_3", 0 to 287, 167 to 193, 399 to 99),
    pip("2_4", 52 to 180, 215 to 360, 378 to 206),
)

/** The template called [name], or null if it isn't bundled (e.g. a stale restored back stack). */
internal fun pipTemplate(name: String): PipTemplate? = PipTemplates.firstOrNull { it.name == name }
