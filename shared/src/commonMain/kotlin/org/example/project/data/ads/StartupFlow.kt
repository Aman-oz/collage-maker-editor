package org.example.project.data.ads

import com.ozi.nextgen.compose.ads_manager.navigation.NavScreen
import com.ozi.nextgen.compose.ads_manager.navigation.StartupState
import org.example.project.data.AppSettings
import org.example.project.navigation.Destination

/**
 * What the startup flow (`OziAds.navigation`) needs to know about the user. A language counts as
 * done once one is saved, onboarding once it has been finished.
 */
fun AppSettings.startupState(): StartupState = StartupState(
    languageDone = languageCode != null,
    onboardingDone = hasCompletedOnboarding,
    isPremium = isPremium.value,
)

/** The app's screen for a step of the startup flow. Paywalls opened by the flow are `fromSplash`. */
fun NavScreen.destination(): Destination = when (this) {
    NavScreen.LANGUAGE -> Destination.Language()
    NavScreen.ONBOARDING -> Destination.Onboarding
    NavScreen.PREMIUM -> Destination.Premium(fromSplash = true)
    NavScreen.OFFER -> Destination.LaunchOffer
    NavScreen.MAIN -> Destination.Home
}
