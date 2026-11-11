package org.example.project.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ozi.nextgen.compose.ads_kmp.NativeAdCard
import com.ozi.nextgen.compose.ads_manager.ads.type.NativeAdLayout
import com.ozi.nextgen.compose.ads_manager.remote_config.model.AdPlacementKeys
import org.example.project.data.AppSettings
import org.koin.compose.koinInject

/**
 * The `internal_app_native` ad at the bottom of a screen, in the compact media-left layout
 * (media left, headline / "Ad" / body right, full-width CTA under both), with a matching shimmer
 * while it loads. Nothing at all for premium users, or when the placement or [point] is disabled
 * in Remote Config, or the load fails. Colours follow the app's theme, not the system's.
 */
@Composable
internal fun BottomNativeAd(point: String, modifier: Modifier = Modifier) {
    val isPremium by koinInject<AppSettings>().isPremium.collectAsStateWithLifecycle()
    if (isPremium) return
    NativeAdCard(
        placementKey = AdPlacementKeys.INTERNAL_APP_NATIVE,
        point = point,
        layout = NativeAdLayout.MEDIA_LEFT,
        isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
        modifier = modifier,
    )
}
