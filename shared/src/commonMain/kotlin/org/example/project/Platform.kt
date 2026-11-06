package org.example.project

interface Platform {
    val name: String

    /** True for debug builds; ads read the `debug_ad_configuration` Remote Config block then. */
    val isDebugBuild: Boolean
}

expect fun getPlatform(): Platform
