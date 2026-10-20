package org.example.project.ui.layouts

import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.CentralDataApi
import org.example.project.data.centraldata.CentralDataConfig
import org.example.project.ui.collage.CollageCatalog
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.toCategories

/**
 * A collage layout offered on Home and in the Layouts browser. Picking one opens the gallery for
 * [photoCount] photos and then the collage editor on this layout ([name], e.g. `collage_3_5`).
 */
data class CollageLayout(
    val id: String,
    val name: String,
    val photoCount: Int,
    /** The layout's lines on a transparent ground, drawn tinted to the theme. */
    val previewUrl: String,
    /** The server's own flag for this layout; the collage catalog's premium layouts don't count here. */
    val isPremium: Boolean,
)

/**
 * Reads the central-data server's Layouts category: `subcategories?categoryId=` (one per photo
 * count, the browser's tabs) and `assets?categoryId=` / `?subCategoryId=` (the layouts).
 *
 * A layout asset only names its picture (`collage_3_5.png`). Each is joined by that name with
 * the [CollageCatalog], which supplies the photo count and whose slot geometry the collage editor
 * needs: a layout the catalog can't draw is left out, so every one shown opens.
 */
class LayoutsRepository(
    private val api: CentralDataApi,
    private val catalog: CollageCatalog,
) {

    val isConfigured: Boolean get() = api.isConfigured

    /** Every layout in the category, for Home's rows. */
    suspend fun getAll(): List<CollageLayout> =
        api.assetsInCategory(CentralDataConfig.LAYOUTS_CATEGORY_ID).toLayouts()

    /** The browser's tabs: one sub-category per photo count, in server order. */
    suspend fun getCategories(): List<TemplateCategory> =
        api.subcategories(CentralDataConfig.LAYOUTS_CATEGORY_ID).toCategories()

    /** The names of the layouts the server marks premium, for the layout editor's gate. */
    suspend fun premiumLayoutNames(): Set<String> =
        getAll().filter { it.isPremium }.mapTo(HashSet()) { it.name }

    suspend fun getLayouts(subCategoryId: String): List<CollageLayout> =
        api.assetsInSubcategory(subCategoryId).toLayouts()

    private suspend fun List<CentralAssetDto>.toLayouts(): List<CollageLayout> {
        val drawable = catalog.loadAll().associateBy { it.title }
        return toCollageLayouts(api::absoluteUrl) { name -> drawable[name] }
    }
}

/**
 * The enabled assets as [CollageLayout]s in server order, each joined with [templateFor] its
 * layout name (the picture's file name without its extension). Assets with no template are dropped.
 */
internal fun List<CentralAssetDto>.toCollageLayouts(
    absoluteUrl: (String) -> String,
    templateFor: (String) -> TemplateItem?,
): List<CollageLayout> =
    filter { it.isEnabled }
        .sortedBy { it.sequence }
        .mapNotNull { asset ->
            val image = asset.moreFields.image.firstOrNull()
            val path = image?.url ?: asset.thumbnailUrl.orEmpty()
            val name = (image?.filename ?: path.substringAfterLast('/')).substringBeforeLast('.')
            val template = templateFor(name) ?: return@mapNotNull null
            CollageLayout(
                id = asset.id,
                name = name,
                photoCount = template.imageCount,
                previewUrl = absoluteUrl(path),
                isPremium = asset.isPremium,
            )
        }
        .distinctBy { it.name }
