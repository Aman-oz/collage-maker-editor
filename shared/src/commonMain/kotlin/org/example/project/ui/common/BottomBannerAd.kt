package org.example.project.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.ozi.nextgen.compose.ads_kmp.BannerAd
import com.ozi.nextgen.compose.ads_kmp.OziAds
import com.ozi.nextgen.compose.ads_manager.ads.type.BannerAdSizeType
import com.ozi.nextgen.compose.ads_manager.ads.type.BannerType
import com.ozi.nextgen.compose.ads_manager.remote_config.model.AdPlacementKeys

/**
 * A banner pinned to the bottom of a screen ([placementKey], `internal_app_banner` unless given),
 * with a thin divider above it and the app's shimmer while it loads. Takes no space at all when the
 * placement or [point] is disabled in Remote Config, the user is premium, or the load fails.
 * Callers skip it for premium users and where there is no room (a phone on its side).
 */
@Composable
internal fun BottomBannerAd(
    point: String,
    modifier: Modifier = Modifier,
    placementKey: String = AdPlacementKeys.INTERNAL_APP_BANNER,
    type: BannerType = BannerType.ADAPTIVE,
    adSizeType: BannerAdSizeType = BannerAdSizeType.ANCHORED_ADAPTIVE,
) {
    var adHeightPx by remember { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxWidth()) {
        if (adHeightPx > 0) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
        BannerAd(
            placementKey = placementKey,
            point = point,
            type = type,
            adSizeType = adSizeType,
            modifier = Modifier.fillMaxWidth().onSizeChanged { adHeightPx = it.height },
            // Fills the space the SDK holds while the banner loads; gone once it loads or fails.
            placeholder = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .shimmer(),
                )
            },
        )
    }
}

/** A banner style chosen in Remote Config: the banner type and size to request. */
internal data class BannerStyle(val type: BannerType, val adSizeType: BannerAdSizeType)

/**
 * The banner style in Remote Config [key] (a number): `0` adaptive, `1` collapsible, `2` large;
 * anything else, or no value, is adaptive.
 */
internal fun remoteBannerStyle(key: String): BannerStyle = when (OziAds.remoteLong(key, default = 0L)) {
    1L -> BannerStyle(BannerType.COLLAPSABLE, BannerAdSizeType.ANCHORED_ADAPTIVE)
    2L -> BannerStyle(BannerType.ADAPTIVE, BannerAdSizeType.LARGE_ANCHORED)
    else -> BannerStyle(BannerType.ADAPTIVE, BannerAdSizeType.ANCHORED_ADAPTIVE)
}
