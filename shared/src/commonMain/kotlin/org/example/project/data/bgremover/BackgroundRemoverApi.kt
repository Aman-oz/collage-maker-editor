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
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.TimeSource
import org.example.project.AppLog
import org.example.project.data.network.decodeImageBitmap

/** The server's answer to an upload: where to download the transparent cut-out (WebP or PNG). */
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
 * transparent image. Only its alpha is used (see `EraseOp.AiCutOut`).
 *
 * The endpoint and key come from [BgRemoverConfig], generated from `local.properties`.
 *
 * Every step logs under [Tag] (`adb logcat -s BgRemoverApi`); the API key's value never does.
 */
internal class BackgroundRemoverApi(private val client: HttpClient) {

    val isConfigured: Boolean get() = BgRemoverConfig.BASE_URL.isNotEmpty()

    /**
     * Where uploads actually land once a redirect has been followed, so later removals skip the
     * extra hop. The server moved from `http://` to `https://` and answers the old URL with a 308.
     */
    private var uploadUrl: String? = null

    suspend fun removeBackground(photo: ImageBitmap): ImageBitmap {
        if (!isConfigured) {
            AppLog.w(Tag, "Not configured: BG_REMOVER_BASE_URL (or BASE_URL) is missing from local.properties")
            throw BackgroundRemoverException.NotConfigured()
        }
        val started = TimeSource.Monotonic.markNow()
        val bytes = withContext(Dispatchers.Default) { photo.encodeToByteArray(ImageFormat.JPEG, UploadJpegQuality) }
        AppLog.d(Tag, "Encoded ${photo.width}x${photo.height} photo to ${bytes.size} bytes of JPEG")
        return try {
            val response = upload(bytes)
            if (!response.status.isSuccess()) {
                val body = runCatching { response.bodyAsText().take(LoggedBodyChars) }.getOrDefault("")
                AppLog.e(Tag, "Upload failed: HTTP ${response.status.value} from ${response.request.url}; body: $body")
                throw BackgroundRemoverException.Server(response.status.value)
            }
            val imageUrl = response.body<RemoveBackgroundResponse>().imageUrl
            val downloadUrl = resolveCutOutUrl(BgRemoverConfig.BASE_URL, imageUrl)
            AppLog.d(Tag, "Upload OK after ${started.elapsedNow()}; downloading cut-out from $downloadUrl")
            // A GET follows redirects on its own (Ktor only refuses to for other methods).
            val download = client.get(downloadUrl) {
                timeout { requestTimeoutMillis = RequestTimeoutMillis }
            }
            if (!download.status.isSuccess()) {
                AppLog.e(Tag, "Cut-out download failed: HTTP ${download.status.value} from ${download.request.url}")
                throw BackgroundRemoverException.Server(download.status.value)
            }
            val image = download.readRawBytes()
            AppLog.d(Tag, "Downloaded ${image.size} bytes (${download.headers[HttpHeaders.ContentType]})")
            withContext(Dispatchers.Default) { decodeImageBitmap(image) }.also {
                AppLog.i(Tag, "Background removed: ${it.width}x${it.height} cut-out in ${started.elapsedNow()}")
            }
        } catch (e: BackgroundRemoverException) {
            throw e
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            AppLog.d(Tag, "Cancelled after ${started.elapsedNow()}")
            throw e
        } catch (e: Exception) {
            AppLog.e(Tag, "Removal failed after ${started.elapsedNow()}: ${e::class.simpleName}: ${e.message}", e)
            throw BackgroundRemoverException.Network(e)
        }
    }

    /**
     * POSTs [photo] and follows up to [MaxRedirects] redirects by hand: Ktor's redirect plugin only
     * follows GET and HEAD, so a redirected POST would otherwise surface as a failed 3xx.
     */
    private suspend fun upload(photo: ByteArray): HttpResponse {
        var url = uploadUrl ?: joinUrl(BgRemoverConfig.BASE_URL, RemovePath)
        repeat(MaxRedirects + 1) {
            AppLog.d(
                Tag,
                "POST $url (${photo.size} bytes, key header " +
                    "'${BgRemoverConfig.API_KEY_HEADER.ifEmpty { "<none>" }}', key set: ${BgRemoverConfig.API_KEY.isNotEmpty()})",
            )
            val response = postPhoto(url, photo)
            AppLog.d(Tag, "POST $url -> HTTP ${response.status.value}")
            val location = response.headers[HttpHeaders.Location]
            if (response.status.value !in RedirectStatuses || location == null) {
                if (response.status.isSuccess()) uploadUrl = url
                return response
            }
            val next = resolveRedirect(url, location)
            AppLog.w(Tag, "Upload redirected (${response.status.value}) to $next; update BG_REMOVER_BASE_URL to skip this hop")
            url = next
        }
        AppLog.e(Tag, "Gave up after $MaxRedirects redirects")
        throw BackgroundRemoverException.Server(RedirectLoopStatus)
    }

    private suspend fun postPhoto(url: String, photo: ByteArray): HttpResponse =
        client.submitFormWithBinaryData(
            url = url,
            formData = formData {
                append(
                    "file",
                    photo,
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

    private companion object {
        const val Tag = "BgRemoverApi"
        const val RemovePath = "bg-remover/remove"
        const val UploadJpegQuality = 92
        const val RequestTimeoutMillis = 60_000L
        const val MaxRedirects = 3
        const val LoggedBodyChars = 500

        /** Reported when the server keeps redirecting; "Loop Detected". */
        const val RedirectLoopStatus = 508

        /** The statuses that carry a `Location` to retry the upload at. */
        val RedirectStatuses = setOf(301, 302, 303, 307, 308)
    }
}

/** A redirect's [location] (absolute, or relative to [current]) as an absolute URL. */
internal fun resolveRedirect(current: String, location: String): String =
    URLBuilder(current).takeFrom(location).buildString()

/** [base] and [path] joined by exactly one slash, whichever of them already has one. */
internal fun joinUrl(base: String, path: String): String = base.trimEnd('/') + "/" + path.trimStart('/')

/** The server's `image_url` as a fetchable URL: absolute as is, otherwise relative to [base]. */
internal fun resolveCutOutUrl(base: String, imageUrl: String): String =
    if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) imageUrl else joinUrl(base, imageUrl)
