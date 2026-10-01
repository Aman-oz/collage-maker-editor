package org.example.project.ui.onboarding

import androidx.lifecycle.ViewModel
import org.example.project.data.AppSettings

/** Records that the first-run walkthrough is done, so later launches skip straight to Home. */
class OnboardingViewModel(private val settings: AppSettings) : ViewModel() {

    fun complete() {
        settings.hasCompletedOnboarding = true
    }
}
