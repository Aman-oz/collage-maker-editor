package org.example.project.ui.home

import androidx.lifecycle.ViewModel
import org.example.project.Platform

/**
 * Home screen state holder. [Platform] is injected by Koin to show that non-ViewModel singletons
 * are wired the same way on both platforms.
 */
class HomeViewModel(platform: Platform) : ViewModel() {
    val platformName: String = platform.name
}
