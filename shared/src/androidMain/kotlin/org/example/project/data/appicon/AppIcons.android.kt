package org.example.project.data.appicon

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context
import org.example.project.AppLog

/*
 * Android launcher icons are the `<activity-alias android:name=".Launcher_<id>">` entries in
 * androidApp's manifest, all opening MainActivity, exactly one enabled at a time. Switching enables
 * the new alias and disables the rest.
 *
 * Doing that while the app is on screen can close it or drop its home-screen shortcut on some
 * launchers, so a switch is only remembered here and made once the app leaves the screen (its
 * last started activity stops). The launcher then shows the new icon within a few seconds.
 */

/** The manifest's namespace (androidApp `namespace`), which the alias names are relative to. */
private const val AliasPackage = "org.example.project"

private const val Tag = "AppIcons"

private val allIds get() = listOf(AppIcons.DEFAULT) + AppIcons.ids

private fun aliasOf(context: Context, id: String) = ComponentName(context.packageName, "$AliasPackage.Launcher_$id")

/** A switch waiting for the app to leave the screen, or null. */
private var pendingIcon: String? = null

private var watchingLifecycle = false

internal actual fun currentAppIcon(): String {
    val context = FileKit.context
    val pm = context.packageManager
    // The one alias that is enabled, either by a past switch or by the manifest (only default).
    return pendingIcon ?: allIds.firstOrNull { id ->
        when (pm.getComponentEnabledSetting(aliasOf(context, id))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> id == AppIcons.DEFAULT
            else -> false
        }
    } ?: AppIcons.DEFAULT
}

internal actual fun setAppIcon(id: String) {
    pendingIcon = id
    val app = FileKit.context as? Application ?: return applyIcon(id)
    if (watchingLifecycle) return
    watchingLifecycle = true
    app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        // This runs from the splash, so MainActivity is already started.
        private var started = 1

        override fun onActivityStarted(activity: Activity) {
            started++
        }

        override fun onActivityStopped(activity: Activity) {
            started--
            if (started > 0 || activity.isChangingConfigurations) return
            pendingIcon?.let(::applyIcon)
            pendingIcon = null
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    })
}

/** Enables [id]'s alias first (so there is never a moment without a launcher entry), then disables the rest. */
private fun applyIcon(id: String) {
    val context = FileKit.context
    val pm = context.packageManager
    runCatching {
        pm.setComponentEnabledSetting(
            aliasOf(context, id),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        for (other in allIds) {
            if (other == id) continue
            pm.setComponentEnabledSetting(
                aliasOf(context, other),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        AppLog.i(Tag, "App icon is now $id")
    }.onFailure { AppLog.w(Tag, "Couldn't switch the app icon to '$id'", it) }
}
