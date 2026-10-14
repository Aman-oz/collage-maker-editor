package org.example.project.ui.frames

import org.example.project.data.centraldata.CentralDataApi
import org.example.project.data.centraldata.CentralDataConfig
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplateFrame
import org.example.project.ui.templates.toCategories
import org.example.project.ui.templates.toFrames

/**
 * Fetches the Frames catalog from the central-data server:
 *
 * - `subcategories?categoryId=<frames category>` → the category list (rvCategory).
 * - `assets?subCategoryId=<id>` → that category's frames (rvFrames).
 *
 * Frames are the same asset shape as templates and open the same editor, so this reuses the
 * templates mapping and only the category differs.
 */
class FramesRepository(private val api: CentralDataApi) {

    suspend fun getCategories(): List<TemplateCategory> =
        api.subcategories(CentralDataConfig.FRAMES_CATEGORY_ID).toCategories()

    suspend fun getFrames(categoryId: String): List<TemplateFrame> =
        api.assetsInSubcategory(categoryId).toFrames(api::absoluteUrl)
}
