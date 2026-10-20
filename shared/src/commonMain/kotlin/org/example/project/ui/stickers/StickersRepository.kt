package org.example.project.ui.stickers

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.CentralDataApi
import org.example.project.data.centraldata.CentralDataConfig

/** A sticker category pill (Baby, Animal, …), under the stickers super-category. */
internal data class StickerCategory(val id: String, val name: String)

/** A sub-category tile of a [StickerCategory] (Newborn, Baby Boy, …). */
internal data class StickerSubcategory(val id: String, val categoryId: String, val name: String)

/**
 * One sticker of the library. [imageUrl] is its SVG, [thumbnailUrl] the picture for the grid;
 * [aspectRatio] is width / height, known before the SVG is downloaded so it can be placed at once.
 */
internal data class StickerAsset(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val imageUrl: String,
    val aspectRatio: Float,
    val isPremium: Boolean,
)

/**
 * The sticker library on the central-data server, three levels deep:
 *
 * - `categories?superCategoryId=<stickers>` → the category pills.
 * - `subcategories?categoryId=<id>` → that category's tiles.
 * - `assets?categoryId=<id>&subCategoryId=<id>` → the tile's stickers (the server wants both).
 *
 * Each answer is kept for the process, since the Stickers tool is opened and closed often and the
 * tiles each ask for their sub-category's stickers to show the first one. Failures are not kept.
 */
class StickersRepository(private val api: CentralDataApi) {

    private val mutex = Mutex()
    private var categories: List<StickerCategory>? = null
    private val subcategories = mutableMapOf<String, List<StickerSubcategory>>()
    private val stickers = mutableMapOf<String, List<StickerAsset>>()

    internal suspend fun getCategories(): List<StickerCategory> =
        mutex.withLock { categories } ?: api.categories(CentralDataConfig.STICKERS_SUPER_CATEGORY_ID)
            .filter { it.isEnabled && it.id.isNotBlank() }
            .sortedBy { it.sequence }
            .map { StickerCategory(id = it.id, name = it.name) }
            .also { mutex.withLock { categories = it } }

    internal suspend fun getSubcategories(categoryId: String): List<StickerSubcategory> =
        mutex.withLock { subcategories[categoryId] } ?: api.subcategories(categoryId)
            .filter { it.isEnabled && it.id.isNotBlank() }
            .sortedBy { it.sequence }
            .map { StickerSubcategory(id = it.id, categoryId = categoryId, name = it.name) }
            .also { mutex.withLock { subcategories[categoryId] = it } }

    internal suspend fun getStickers(subcategory: StickerSubcategory): List<StickerAsset> =
        mutex.withLock { stickers[subcategory.id] } ?: api.assets(subcategory.categoryId, subcategory.id)
            .toStickers(api::absoluteUrl)
            .also { mutex.withLock { stickers[subcategory.id] = it } }
}

/** Enabled stickers with a picture, in server order. [absoluteUrl] resolves the server's relative paths. */
internal fun List<CentralAssetDto>.toStickers(absoluteUrl: (String) -> String): List<StickerAsset> =
    filter { it.isEnabled }
        .sortedBy { it.sequence }
        .mapNotNull { asset ->
            val file = asset.moreFields.svg.firstOrNull { it.url.isNotBlank() }
                ?: asset.moreFields.image.firstOrNull { it.url.isNotBlank() }
                ?: return@mapNotNull null
            StickerAsset(
                id = asset.id,
                name = asset.name,
                thumbnailUrl = absoluteUrl(asset.thumbnailUrl?.takeIf { it.isNotBlank() } ?: file.url),
                imageUrl = absoluteUrl(file.url),
                aspectRatio = if (file.width > 0f && file.height > 0f) file.width / file.height else 1f,
                isPremium = asset.isPremium,
            )
        }
