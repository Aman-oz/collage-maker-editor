package org.example.project.data.centraldata

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One category from the `categories` endpoint (a sticker category pill), under a super-category. */
@Serializable
internal data class CentralCategoryDto(
    val id: String = "",
    val name: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    val sequence: Int = 0,
)

/** One sub-category from the `subcategories` endpoint (a Templates or Frames category pill). */
@Serializable
internal data class CentralSubcategoryDto(
    val id: String = "",
    val name: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    val sequence: Int = 0,
)

/** One asset from the `assets` endpoint. Only the fields the catalogs read. */
@Serializable
internal data class CentralAssetDto(
    val id: String = "",
    val name: String = "",
    @SerialName("subcategory_name") val subcategoryName: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("is_premium") val isPremium: Boolean = false,
    val sequence: Int = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("more_fields") val moreFields: CentralAssetFieldsDto = CentralAssetFieldsDto(),
)

/**
 * The per-category payload of an asset: collages carry their preview under [image], templates and
 * frames their frame picture and photo slots under [frames], stickers their picture under [svg].
 */
@Serializable
internal data class CentralAssetFieldsDto(
    val image: List<CentralImageDto> = emptyList(),
    val frames: CentralFrameDto? = null,
    val svg: List<CentralImageDto> = emptyList(),
)

/** A file of an asset; [width] × [height] is its size in px, or 0 where the server leaves it out. */
@Serializable
internal data class CentralImageDto(
    val url: String = "",
    val filename: String = "",
    val width: Float = 0f,
    val height: Float = 0f,
)

/** A template's or frame's picture ([imageUrl], [width] × [height] px) and its photo slots. */
@Serializable
internal data class CentralFrameDto(
    @SerialName("image_url") val imageUrl: String = "",
    val filename: String = "",
    val width: Float = 0f,
    val height: Float = 0f,
    val coordinates: List<CentralSlotDto> = emptyList(),
)

/**
 * One photo slot in the frame picture's pixel space. Unlike the old LAS server, [x]/[y] are the
 * slot's top-left corner before [rotation] (degrees clockwise, about the slot's centre).
 */
@Serializable
internal data class CentralSlotDto(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val rotation: Float = 0f,
    val elevation: Float = 0f,
)

/**
 * One font from the `assets` endpoint: [name] is the family, `more_fields.url.file` its TTF. Its own
 * type rather than a field of [CentralAssetFieldsDto], whose `url` key another category may use for
 * something else.
 */
@Serializable
internal data class CentralFontAssetDto(
    val name: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("is_premium") val isPremium: Boolean = false,
    val sequence: Int = 0,
    @SerialName("more_fields") val moreFields: CentralFontFieldsDto = CentralFontFieldsDto(),
)

@Serializable
internal data class CentralFontFieldsDto(val url: CentralFontFileDto? = null)

@Serializable
internal data class CentralFontFileDto(val file: String = "")

/** [path] as an absolute URL: the server returns its own images as paths relative to [baseUrl]. */
internal fun resolveUrl(baseUrl: String, path: String): String = when {
    path.isEmpty() || path.startsWith("http://") || path.startsWith("https://") -> path
    else -> baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}

/**
 * Client for the central-data server that serves the collage, template, frame, sticker and font catalogs:
 * `GET {base}/api/v1/instance/{instanceId}/categories?superCategoryId=…`,
 * `…/subcategories?categoryId=…` and `…/assets?categoryId=…` and/or `?subCategoryId=…`, authenticated with `X-API-Key`. The server's static images need the same
 * header, which [authorize] adds for the image loader.
 */
class CentralDataApi(private val client: HttpClient) {

    val isConfigured: Boolean
        get() = CentralDataConfig.API_KEY.isNotBlank() && CentralDataConfig.BASE_URL.isNotBlank()

    internal suspend fun categories(superCategoryId: String): List<CentralCategoryDto> =
        request("categories", "superCategoryId" to superCategoryId).body()

    internal suspend fun subcategories(categoryId: String): List<CentralSubcategoryDto> =
        request("subcategories", "categoryId" to categoryId).body()

    internal suspend fun assetsInCategory(categoryId: String): List<CentralAssetDto> =
        request("assets", "categoryId" to categoryId).body()

    internal suspend fun assetsInSubcategory(subCategoryId: String): List<CentralAssetDto> =
        request("assets", "subCategoryId" to subCategoryId).body()

    /** The server only narrows the assets to one sub-category when it is given its category too. */
    internal suspend fun assets(categoryId: String, subCategoryId: String): List<CentralAssetDto> =
        request("assets", "categoryId" to categoryId, "subCategoryId" to subCategoryId).body()

    /** One font sub-category's fonts; like [assets], the server needs the category too. */
    internal suspend fun fontAssets(categoryId: String, subCategoryId: String): List<CentralFontAssetDto> =
        request("assets", "categoryId" to categoryId, "subCategoryId" to subCategoryId).body()

    /** [path] resolved against the server, for the relative image URLs in its responses. */
    internal fun absoluteUrl(path: String): String = resolveUrl(CentralDataConfig.BASE_URL, path)

    private suspend fun request(endpoint: String, vararg query: Pair<String, String>): HttpResponse {
        check(isConfigured) { "The central-data API key is not set up" }
        val response = client.get(absoluteUrl("api/v1/instance/${CentralDataConfig.INSTANCE_ID}/$endpoint")) {
            header(API_KEY_HEADER, CentralDataConfig.API_KEY)
            for ((name, value) in query) parameter(name, value)
        }
        if (!response.status.isSuccess()) {
            error("Request failed (${response.status.value}): ${response.bodyAsText().take(200)}")
        }
        return response
    }

    companion object {
        private const val API_KEY_HEADER = "X-API-Key"

        /** Adds the API key to a request for one of the server's own URLs, and to nothing else. */
        fun authorize(request: HttpRequestBuilder, url: String) {
            val base = CentralDataConfig.BASE_URL
            if (base.isNotBlank() && CentralDataConfig.API_KEY.isNotBlank() && url.startsWith(base.trimEnd('/') + "/")) {
                request.header(API_KEY_HEADER, CentralDataConfig.API_KEY)
            }
        }
    }
}
