package org.example.project.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds the splash screen state. Any real start-up work (remote config, billing, assets) belongs
 * here, replacing the fixed delay below.
 */
class SplashViewModel : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            delay(SPLASH_DURATION_MS)
            _isReady.value = true
        }
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1_500L
    }
}
