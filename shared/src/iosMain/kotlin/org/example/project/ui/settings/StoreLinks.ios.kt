package org.example.project.ui.settings

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.koin.mp.KoinPlatform
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication

internal actual suspend fun openStoreRating() {
    val id = appStoreId()
    if (id == null) {
        // Not on the App Store yet: Apple's in-app prompt is the only way to rate. iOS decides
        // whether it actually shows (always in debug builds, never in TestFlight, rate-limited live).
        @Suppress("DEPRECATION")
        val scene = UIApplication.sharedApplication.keyWindow?.windowScene ?: return
        SKStoreReviewController.requestReviewInScene(scene)
        return
    }
    val url = NSURL.URLWithString("itms-apps://apps.apple.com/app/id$id?action=write-review") ?: return
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
}

internal actual suspend fun appStoreUrl(): String =
    appStoreId()?.let { "https://apps.apple.com/app/id$it" } ?: SettingsLinks.MoreAppsUrl

/** Found once per launch; a failed lookup (offline, not published) is retried next time. */
private var resolvedAppStoreId: String? = null

/**
 * The app's numeric App Store id: [SettingsLinks.AppStoreId] when set, otherwise looked up from
 * Apple's public iTunes Search API by bundle id, so Rate Us and Share App pick up the real listing
 * as soon as the app is live without a code change. `null` while the app isn't on the store.
 */
private suspend fun appStoreId(): String? {
    SettingsLinks.AppStoreId.takeIf { it.isNotBlank() }?.let { return it }
    resolvedAppStoreId?.let { return it }
    val bundleId = NSBundle.mainBundle.bundleIdentifier ?: return null
    return runCatching {
        // The response is served as text/javascript, so it's parsed by hand rather than by
        // ContentNegotiation.
        val body = KoinPlatform.getKoin().get<HttpClient>()
            .get("https://itunes.apple.com/lookup") { parameter("bundleId", bundleId) }
            .bodyAsText()
        Json.parseToJsonElement(body).jsonObject["results"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("trackId")?.jsonPrimitive?.longOrNull?.toString()
    }.getOrNull()?.also { resolvedAppStoreId = it }
}
