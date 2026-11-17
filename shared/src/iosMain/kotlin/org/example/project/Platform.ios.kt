package org.example.project

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice
import kotlin.experimental.ExperimentalNativeApi

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    @OptIn(ExperimentalNativeApi::class)
    override val isDebugBuild: Boolean = kotlin.native.Platform.isDebugBinary

    /** GoogleMobileAds reads this itself; exposed for logging. Set per configuration in Config.xcconfig. */
    override val adMobAppId: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("GADApplicationIdentifier") as? String ?: ""
}

actual fun getPlatform(): Platform = IOSPlatform()
