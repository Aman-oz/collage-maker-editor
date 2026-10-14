package org.example.project.ui.home

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.CentralDataApi
import org.example.project.data.centraldata.CentralDataConfig
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplateFrame
import org.example.project.ui.templates.toCategories
import org.example.project.ui.templates.toFrames

/** Which catalog a Home row belongs to: its items open that catalog's editor and View All its browser. */
enum class HomeSectionKind { Templates, Frames }

/** One Home row: a category of the Home catalog and its templates or frames. */
data class HomeSection(
    val category: TemplateCategory,
    val kind: HomeSectionKind,
    val frames: List<TemplateFrame>,
)

/**
 * Fetches the rows under Home's tools card from the central-data server:
 *
 * - `subcategories?categoryId=<home category>` → one row per category.
 * - `assets?subCategoryId=<id>` → that row's templates or frames.
 *
 * The Home category has categories of its own, each holding either templates or frames. Their
 * assets have the template shape either way, so they reuse the templates mapping.
 */
class HomeFeedRepository(private val api: CentralDataApi) {

    val isConfigured: Boolean get() = api.isConfigured

    /** Every non-empty row, in server order. A category that fails to load is left out. */
    suspend fun getSections(): List<HomeSection> = coroutineScope {
        api.subcategories(CentralDataConfig.HOME_CATEGORY_ID).toCategories()
            .map { category ->
                async {
                    runCatching { api.assetsInSubcategory(category.id) }.getOrNull()?.let { assets ->
                        val frames = assets.toFrames(api::absoluteUrl)
                        if (frames.isEmpty()) null else HomeSection(category, assets.homeSectionKind(), frames)
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
    }
}

/**
 * Whether these assets are frames or templates. The server doesn't say so in a field; it stores
 * each catalog's pictures under its own folder (`…/central_data/frames/…`), so the paths tell.
 * A row is frames if most of its assets are.
 */
internal fun List<CentralAssetDto>.homeSectionKind(): HomeSectionKind {
    val frames = count { asset ->
        listOfNotNull(asset.thumbnailUrl, asset.moreFields.frames?.imageUrl).any { FramesPathSegment in it }
    }
    return if (frames * 2 > size) HomeSectionKind.Frames else HomeSectionKind.Templates
}

private const val FramesPathSegment = "/central_data/frames/"
