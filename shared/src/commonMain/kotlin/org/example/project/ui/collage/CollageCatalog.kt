package org.example.project.ui.collage

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.AppLog
import org.example.project.data.centraldata.CentralDataApi
import org.example.project.data.centraldata.CentralDataConfig
import org.example.project.data.collage.toRemoteCollages
import org.example.project.ui.collage.frames.FrameImageUtils
import org.example.project.ui.collage.geom.TemplateItem
import photocollagemaker.shared.generated.resources.Res

/** One entry in the bundled `collages.json` (the offline fallback): the preview image URLs and premium flag for a named layout. */
@Serializable
private data class CollageEntryDto(
    val id: String = "",
    val name: String = "",
    val image_dark: String = "",
    val image_light: String = "",
    val isPremium: Boolean = false,
)

/**
 * Loads the collage layouts. The list comes from the central-data server ([CentralDataApi]): each
 * layout's name is looked up in the hand-coded slot geometry via [FrameImageUtils.createTemplateItems]
 * and paired with its preview URLs and premium flag. With no API key configured, or when the request
 * fails (offline), it falls back to the bundled `collages.json`, so the collage editor always has
 * layouts. Layouts whose name has no matching generator are skipped; the rest are sorted by photo count.
 */
class CollageCatalog(private val api: CentralDataApi) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var cached: List<TemplateItem>? = null

    suspend fun loadAll(): List<TemplateItem> {
        cached?.let { return it }
        val remote = if (api.isConfigured) {
            runCatching { loadRemote() }
                .onFailure { AppLog.w(TAG, "Collage layouts request failed, using bundled list: ${it.message}") }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
        // Only a server answer is cached: after a bundled fallback the next open tries the server again.
        return remote?.also { cached = it } ?: loadBundled()
    }

    private suspend fun loadRemote(): List<TemplateItem> =
        api.assetsInCategory(CentralDataConfig.COLLAGES_CATEGORY_ID)
            .toRemoteCollages(api::absoluteUrl)
            .sortedBy { it.sequence }
            .mapNotNull { entry ->
                templateItem(entry.id, entry.name, entry.previewLight, entry.previewDark, entry.isPremium)
            }
            .sortedBy { it.photoItemList.size }

    private suspend fun loadBundled(): List<TemplateItem> {
        val bytes = Res.readBytes("files/collages.json")
        val entries = json.decodeFromString<List<CollageEntryDto>>(bytes.decodeToString())
        return entries.mapNotNull { entry ->
            templateItem(entry.id, entry.name, entry.image_light, entry.image_dark, entry.isPremium)
        }.sortedBy { it.photoItemList.size }
    }

    private fun templateItem(id: String, name: String, light: String, dark: String, isPremium: Boolean): TemplateItem? {
        val item = FrameImageUtils.createTemplateItems("$name.png") ?: return null
        item.id = id
        item.title = name
        item.preview = light
        item.previewDark = dark
        item.isPremium = isPremium
        return item
    }

    private companion object {
        const val TAG = "CollageCatalog"
    }
}
