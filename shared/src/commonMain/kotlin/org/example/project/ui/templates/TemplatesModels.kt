package org.example.project.ui.templates

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Reads a number the template server sends as a quoted string (slot coordinates arrive like
 * `"x": "184"`, `"rotation": "0"`), while `elevation` is a bare number. Decoding via
 * [Decoder.decodeString] (the client's JSON is lenient, so it also accepts unquoted tokens) then
 * parsing tolerates both; anything unparseable degrades to `0f`.
 */
internal object LenientFloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientFloat", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Float = decoder.decodeString().trim().toFloatOrNull() ?: 0f
    override fun serialize(encoder: Encoder, value: Float) = encoder.encodeString(value.toString())
}

// --- Wire models (mirror the LAS FrameSubCategory / FrameItem responses) ---

/** `{ "data": [ ... ] }` envelope used by both template endpoints. */
@Serializable
internal data class TemplateCategoryResponseDto(val data: List<TemplateCategoryDto> = emptyList())

/** One template category from `getTempcategories` (the LAS `FrameSubCategory`). */
@Serializable
internal data class TemplateCategoryDto(
    val _id: String = "",
    val name: String = "",
    val isEnable: Boolean = true,
    val sequence: String = "",
    val isPremium: Boolean = false,
)

@Serializable
internal data class TemplateFrameResponseDto(val data: List<TemplateFrameDto> = emptyList())

/** One template/frame from `gettemplates` (the LAS `FrameItem`). */
@Serializable
internal data class TemplateFrameDto(
    val _id: String = "",
    val name: String = "",
    val image: String = "",
    val thumbnail: String = "",
    val categoryId: String = "",
    val tempCategoryId: String = "",
    val layout: String = "portrait",
    val isPortrait: Boolean = true,
    val sequence: String = "",
    val isEnable: Boolean = true,
    val isPremium: Boolean = false,
    val coordinates: List<TemplateCoordinateDto> = emptyList(),
)

/**
 * One photo slot on a template, an axis-aligned rectangle in the frame [TemplateFrameDto.image]'s
 * pixel space. [x]/[y] are the slot CENTER (confirmed from the LAS `NewFrameEditor` applier math),
 * [width]/[height] its size, [rotation] degrees clockwise, [elevation] the paint order.
 */
@Serializable
internal data class TemplateCoordinateDto(
    val name: String = "",
    @Serializable(with = LenientFloatSerializer::class) val x: Float = 0f,
    @Serializable(with = LenientFloatSerializer::class) val y: Float = 0f,
    @Serializable(with = LenientFloatSerializer::class) val width: Float = 0f,
    @Serializable(with = LenientFloatSerializer::class) val height: Float = 0f,
    @Serializable(with = LenientFloatSerializer::class) val rotation: Float = 0f,
    @Serializable(with = LenientFloatSerializer::class) val elevation: Float = 0f,
)

// --- Domain models used by the UI (Serializable so the selected frame can be a nav argument) ---

/** The aspect family of a template thumbnail; drives its cell shape in the masonry grid. */
@Serializable
enum class TemplateLayout(val aspectRatio: Float) {
    // width / height, matching the LAS dimensionRatios (0.67:1, 1.34:1, 1:1).
    Portrait(0.67f),
    Landscape(1.34f),
    Square(1f),
}

/** A selectable category shown as a pill in the category row (rvCategory). */
data class TemplateCategory(val id: String, val name: String)

/** One photo slot on a template, in frame-image pixel space; [x]/[y] are the slot center. */
@Serializable
data class TemplateSlot(
    val index: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val elevation: Float,
)

/** A template shown in the masonry grid (rvFrames) and edited in the Templates editor. */
@Serializable
data class TemplateFrame(
    val id: String,
    val thumbnailUrl: String,
    val imageUrl: String,
    val layout: TemplateLayout,
    val isPremium: Boolean,
    val slots: List<TemplateSlot> = emptyList(),
) {
    val slotCount: Int get() = slots.size
}
