package org.example.project.ui.templates

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter

/**
 * Fetches template categories and templates from the LAS "collagemaker" server — the same endpoints
 * the Android `FramesTemplatesActivity` uses via `RemoteRepositoryImpl`/`ApiService`:
 *
 * - `GET /api/collagemaker/getTempcategories` → the category list (rvCategory).
 * - `GET /api/collagemaker/gettemplates?tempCategoryId=<id>` → that category's templates (rvFrames).
 *
 * The server is plain HTTP on an IP, so cleartext traffic must be permitted per platform (Android
 * `usesCleartextTraffic`, iOS `NSAppTransportSecurity`). The endpoints are unauthenticated.
 */
class TemplatesRepository(private val client: HttpClient) {

    suspend fun getCategories(): List<TemplateCategory> =
        client.get("$BASE_URL$CATEGORIES_PATH")
            .body<TemplateCategoryResponseDto>()
            .toCategories()

    suspend fun getTemplates(categoryId: String): List<TemplateFrame> =
        client.get("$BASE_URL$TEMPLATES_PATH?tempCategoryId=${categoryId.encodeURLParameter()}")
            .body<TemplateFrameResponseDto>()
            .toFrames()

    internal companion object {
        const val BASE_URL = "http://161.97.164.28:9006"
        private const val CATEGORIES_PATH = "/api/collagemaker/getTempcategories"
        private const val TEMPLATES_PATH = "/api/collagemaker/gettemplates"
    }
}

/**
 * Enabled categories in server order. Shared with the Frames catalog, whose `getSubCategories`
 * response has the same shape.
 */
internal fun TemplateCategoryResponseDto.toCategories(): List<TemplateCategory> =
    data
        .filter { it.isEnable && it._id.isNotBlank() }
        .sortedBy { it.sequence }
        .map { TemplateCategory(id = it._id, name = it.name) }

/**
 * Enabled, thumbnailed frames in server order. Shared with the Frames catalog: its `getassets`
 * items are the same LAS `FrameItem` and open the same `NewFrameEditor` in the LAS app.
 */
internal fun TemplateFrameResponseDto.toFrames(): List<TemplateFrame> =
    data
        .filter { it.isEnable && it.thumbnail.isNotBlank() }
        .sortedBy { it.sequence }
        .map { dto ->
            TemplateFrame(
                id = dto._id,
                thumbnailUrl = dto.thumbnail,
                imageUrl = dto.image,
                layout = dto.layout.toTemplateLayout(dto.isPortrait),
                isPremium = dto.isPremium,
                slots = dto.coordinates.mapIndexed { index, c ->
                    TemplateSlot(
                        index = index,
                        x = c.x,
                        y = c.y,
                        width = c.width,
                        height = c.height,
                        rotation = c.rotation,
                        elevation = c.elevation,
                    )
                },
            )
        }

/** Maps the server's `layout` string to [TemplateLayout], tolerating its "sqaure" typo. */
private fun String.toTemplateLayout(isPortrait: Boolean): TemplateLayout = when {
    equals("landscape", ignoreCase = true) -> TemplateLayout.Landscape
    startsWith("sq", ignoreCase = true) -> TemplateLayout.Square
    equals("portrait", ignoreCase = true) -> TemplateLayout.Portrait
    else -> if (isPortrait) TemplateLayout.Portrait else TemplateLayout.Landscape
}
