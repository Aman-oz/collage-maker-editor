package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ozi.nextgen.compose.ads_kmp.OziAds
import org.example.project.data.AppSettings
import org.example.project.data.ThemeMode
import org.example.project.data.reminders.UsageTracker
import org.example.project.navigation.AppNavDisplay
import org.example.project.ui.common.EyedropperHost
import org.example.project.ui.theme.AppTheme
import org.example.project.ui.theme.SystemBarsAppearance
import org.koin.compose.koinInject

@Composable
fun App() {
    val settings = koinInject<AppSettings>()
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    SystemBarsAppearance(themeMode, darkTheme)

    // Usage reminders: cancelled while the app is on screen, planned again when it leaves.
    // Ads follow the subscription as it changes (a purchase or restore mid-session), so the
    // startup flow and every placement stop showing ads straight away.
    val premium by settings.isPremium.collectAsStateWithLifecycle()
    LaunchedEffect(premium) { OziAds.isPremium = premium }

    val usage = koinInject<UsageTracker>()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { usage.onAppForeground() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { usage.onAppBackground() }

    AppTheme(darkTheme = darkTheme) {
        // The colour picker's eyedropper samples whatever is on screen, so it wraps everything.
        EyedropperHost(modifier = Modifier.fillMaxSize()) {
            AppNavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }
    }
}
