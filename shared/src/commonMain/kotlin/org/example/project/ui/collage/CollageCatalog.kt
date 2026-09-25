package org.example.project.ui.collage

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.ui.collage.frames.FrameImageUtils
import org.example.project.ui.collage.geom.TemplateItem
import photocollagemaker.shared.generated.resources.Res

/** One entry in `collages.json`: the preview image URLs and premium flag for a named layout. */
@Serializable
private data class CollageEntryDto(
    val id: String = "",
    val name: String = "",
    val image_dark: String = "",
    val image_light: String = "",
    val isPremium: Boolean = false,
)

/**
 * Loads the collage layouts exactly as the LAS `CollageActivity` does: it reads the bundled
 * `collages.json`, and for each entry looks up the hand-coded slot geometry by name via
 * [FrameImageUtils.createTemplateItems], pairing it with the entry's remote preview URL and premium
 * flag. Layouts whose name has no matching generator are skipped; the rest are sorted by photo count.
 */
class CollageCatalog {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var cached: List<TemplateItem>? = null

    suspend fun loadAll(): List<TemplateItem> {
        cached?.let { return it }
        val bytes = Res.readBytes("files/collages.json")
        val entries = json.decodeFromString<List<CollageEntryDto>>(bytes.decodeToString())
        val items = entries.mapNotNull { entry ->
            val item = FrameImageUtils.createTemplateItems("${entry.name}.png") ?: return@mapNotNull null
            item.id = entry.id
            item.title = entry.name
            item.preview = entry.image_light
            item.previewDark = entry.image_dark
            item.isPremium = entry.isPremium
            item
        }.sortedBy { it.photoItemList.size }
        cached = items
        return items
    }
}
