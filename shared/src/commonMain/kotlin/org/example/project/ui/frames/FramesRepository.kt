package org.example.project.ui.frames

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplateCategoryResponseDto
import org.example.project.ui.templates.TemplateFrame
import org.example.project.ui.templates.TemplateFrameResponseDto
import org.example.project.ui.templates.TemplatesRepository
import org.example.project.ui.templates.toCategories
import org.example.project.ui.templates.toFrames

/**
 * Fetches the Frames catalog from the LAS "collagemaker" server — the endpoints the Android
 * `FramesActivity` uses via `RemoteRepositoryImpl`/`ApiService`:
 *
 * - `GET /api/collagemaker/getSubCategories` → the category list (rvCategory).
 * - `GET /api/collagemaker/getassets?subcategoryId=<id>` → that category's frames (rvFrames).
 *
 * Same server and same `FrameItem` wire shape as the Templates catalog (in the LAS app both open
 * `NewFrameEditor` with an identical `Frame` mapping), so this reuses the templates DTOs and mapping
 * and only the endpoints differ.
 */
class FramesRepository(private val client: HttpClient) {

    suspend fun getCategories(): List<TemplateCategory> =
        client.get("${TemplatesRepository.BASE_URL}$CATEGORIES_PATH")
            .body<TemplateCategoryResponseDto>()
            .toCategories()

    suspend fun getFrames(categoryId: String): List<TemplateFrame> =
        client.get("${TemplatesRepository.BASE_URL}$FRAMES_PATH?subcategoryId=${categoryId.encodeURLParameter()}")
            .body<TemplateFrameResponseDto>()
            .toFrames()

    private companion object {
        const val CATEGORIES_PATH = "/api/collagemaker/getSubCategories"
        const val FRAMES_PATH = "/api/collagemaker/getassets"
    }
}
