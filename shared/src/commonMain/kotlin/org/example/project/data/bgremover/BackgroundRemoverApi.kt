package org.example.project.data.bgremover

import androidx.compose.ui.graphics.ImageBitmap
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.data.network.decodeImageBitmap

/** The server's answer to an upload: where to download the cut-out PNG. */
@Serializable
internal data class RemoveBackgroundResponse(@SerialName("image_url") val imageUrl: String)

/** Why an AI removal failed, so the screen can say something more useful than "error". */
internal sealed class BackgroundRemoverException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** `local.properties` had no `BG_REMOVER_*` keys when the app was built. */
    class NotConfigured : BackgroundRemoverException("AI background remover is not configured")

    /** The server answered with a non-2xx status. */
    class Server(status: Int) : BackgroundRemoverException("Server error ($status)")

    /** Timeout, no connection, or an undecodable answer. */
    class Network(cause: Throwable) : BackgroundRemoverException(cause.message ?: "Network error", cause)
}

/**
 * The AI background remover, ported from the LAS app's `ApiService.removeBackground`: uploads the
 * photo as multipart `file` to `bg-remover/remove`, gets back an `image_url`, and downloads that
 * transparent PNG. Only its alpha is used (see `EraseOp.AiCutOut`).
 *
 * The endpoint and key come from [BgRemoverConfig], generated from `local.properties`.
 */
internal class BackgroundRemoverApi(private val client: HttpClient) {

    val isConfigured: Boolean get() = BgRemoverConfig.BASE_URL.isNotEmpty()

    suspend fun removeBackground(photo: ImageBitmap): ImageBitmap {
        if (!isConfigured) throw BackgroundRemoverException.NotConfigured()
        val bytes = withContext(Dispatchers.Default) { photo.encodeToByteArray(ImageFormat.JPEG, UploadJpegQuality) }
        return try {
            val response = client.submitFormWithBinaryData(
                url = joinUrl(BgRemoverConfig.BASE_URL, RemovePath),
                formData = formData {
                    append(
                        "file",
                        bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                            append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
                        },
                    )
                },
            ) {
                if (BgRemoverConfig.API_KEY_HEADER.isNotEmpty()) header(BgRemoverConfig.API_KEY_HEADER, BgRemoverConfig.API_KEY)
                // Segmentation is slow on the server; the shared client's 20 s is too short.
                timeout {
                    requestTimeoutMillis = RequestTimeoutMillis
                    socketTimeoutMillis = RequestTimeoutMillis
                }
            }
            if (!response.status.isSuccess()) throw BackgroundRemoverException.Server(response.status.value)
            val imageUrl = response.body<RemoveBackgroundResponse>().imageUrl
            val download = client.get(resolveCutOutUrl(BgRemoverConfig.BASE_URL, imageUrl)) {
                timeout { requestTimeoutMillis = RequestTimeoutMillis }
            }
            if (!download.status.isSuccess()) throw BackgroundRemoverException.Server(download.status.value)
            val png = download.readRawBytes()
            withContext(Dispatchers.Default) { decodeImageBitmap(png) }
        } catch (e: BackgroundRemoverException) {
            throw e
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw BackgroundRemoverException.Network(e)
        }
    }

    private companion object {
        const val RemovePath = "bg-remover/remove"
        const val UploadJpegQuality = 92
        const val RequestTimeoutMillis = 60_000L
    }
}

/** [base] and [path] joined by exactly one slash, whichever of them already has one. */
internal fun joinUrl(base: String, path: String): String = base.trimEnd('/') + "/" + path.trimStart('/')

/** The server's `image_url` as a fetchable URL: absolute as is, otherwise relative to [base]. */
internal fun resolveCutOutUrl(base: String, imageUrl: String): String =
    if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) imageUrl else joinUrl(base, imageUrl)
