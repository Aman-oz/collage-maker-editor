package org.example.project.data

import android.content.Context
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context

internal actual fun createKeyValueStore(): KeyValueStore {
    // FileKit already holds the application context (initialized at startup), as sharing uses it too.
    val prefs = FileKit.context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return object : KeyValueStore {
        override fun getString(key: String): String? = prefs.getString(key, null)
        override fun putString(key: String, value: String) {
            prefs.edit().putString(key, value).apply()
        }
    }
}
