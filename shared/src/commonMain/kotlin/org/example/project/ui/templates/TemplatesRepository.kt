package org.example.project.ui.templates

import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.CentralDataApi
import org.example.project.data.centraldata.CentralDataConfig
import org.example.project.data.centraldata.CentralFrameDto
import org.example.project.data.centraldata.CentralSubcategoryDto

/**
 * Fetches template categories and templates from the central-data server:
 *
 * - `subcategories?categoryId=<templates category>` → the category list (rvCategory).
 * - `assets?subCategoryId=<id>` → that category's templates (rvFrames).
 */
class TemplatesRepository(private val api: CentralDataApi) {

    suspend fun getCategories(): List<TemplateCategory> =
        api.subcategories(CentralDataConfig.TEMPLATES_CATEGORY_ID).toCategories()

    suspend fun getTemplates(categoryId: String): List<TemplateFrame> =
        api.assetsInSubcategory(categoryId).toFrames(api::absoluteUrl)
}

/** Enabled categories in server order. Shared with the Frames catalog. */
internal fun List<CentralSubcategoryDto>.toCategories(): List<TemplateCategory> =
    filter { it.isEnabled && it.id.isNotBlank() }
        .sortedBy { it.sequence }
        .map { TemplateCategory(id = it.id, name = it.name) }

/**
 * Enabled frames with a picture, in server order. Shared with the Frames catalog, whose assets have
 * the same shape. [absoluteUrl] resolves the server's relative image paths.
 */
internal fun List<CentralAssetDto>.toFrames(absoluteUrl: (String) -> String): List<TemplateFrame> =
    filter { it.isEnabled }
        .sortedBy { it.sequence }
        .mapNotNull { asset ->
            val frame = asset.moreFields.frames?.takeIf { it.imageUrl.isNotBlank() } ?: return@mapNotNull null
            TemplateFrame(
                id = asset.id,
                thumbnailUrl = absoluteUrl(asset.thumbnailUrl?.takeIf { it.isNotBlank() } ?: frame.imageUrl),
                imageUrl = absoluteUrl(frame.imageUrl),
                layout = frame.layout(),
                isPremium = asset.isPremium,
                slots = frame.coordinates.mapIndexed { index, c ->
                    // The server gives the top-left corner; the editor positions slots by their centre.
                    TemplateSlot(
                        index = index,
                        x = c.x + c.width / 2f,
                        y = c.y + c.height / 2f,
                        width = c.width,
                        height = c.height,
                        rotation = c.rotation,
                        elevation = c.elevation,
                    )
                },
            )
        }

/**
 * The grid cell shape for a frame picture, from its pixel size (the server no longer sends a layout
 * name). Frames come in 0.67, 0.78, 1.28, 1.34 and 1.49 as well as square, so it rounds to the
 * nearest family; an unknown size is treated as portrait, the common case.
 */
private fun CentralFrameDto.layout(): TemplateLayout {
    if (width <= 0f || height <= 0f) return TemplateLayout.Portrait
    val ratio = width / height
    return when {
        ratio < 0.9f -> TemplateLayout.Portrait
        ratio > 1.1f -> TemplateLayout.Landscape
        else -> TemplateLayout.Square
    }
}
