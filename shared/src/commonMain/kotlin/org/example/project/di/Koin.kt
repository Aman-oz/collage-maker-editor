package org.example.project.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

private var started = false

/**
 * Starts Koin once per process.
 *
 * Android calls this from the `Application`, passing `androidContext(this)` as [config];
 * iOS calls this from `MainViewController`.
 */
fun initKoin(config: KoinAppDeclaration? = null) {
    if (started) return
    started = true
    startKoin {
        config?.invoke(this)
        modules(appModules)
    }
}
