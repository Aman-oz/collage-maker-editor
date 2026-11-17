package org.example.project

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import org.koin.mp.KoinPlatform

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    // The app module's BuildConfig isn't visible here; the manifest's debuggable flag is the same thing.
    override val isDebugBuild: Boolean
        get() = (KoinPlatform.getKoin().get<Context>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    // Looked up by name: the app module generates it (resValue per build type), so this module's R
    // doesn't have it. Falls back to Google's test app ID if it is missing, so ads stay test ads.
    override val adMobAppId: String
        get() {
            val context = KoinPlatform.getKoin().get<Context>()
            val id = context.resources.getIdentifier("admob_app_id", "string", context.packageName)
            return if (id != 0) context.getString(id) else TestAdMobAppId
        }

    private companion object {
        const val TestAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
