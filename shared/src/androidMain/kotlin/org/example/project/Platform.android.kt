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
}

actual fun getPlatform(): Platform = AndroidPlatform()
