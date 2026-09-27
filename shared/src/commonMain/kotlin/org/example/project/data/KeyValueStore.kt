package org.example.project.data

/** Minimal persistent string store for small user preferences (theme, language). */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

/** `SharedPreferences` on Android, `NSUserDefaults` on iOS. */
internal expect fun createKeyValueStore(): KeyValueStore
