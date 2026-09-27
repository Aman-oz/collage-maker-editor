package org.example.project.data

import platform.Foundation.NSUserDefaults

internal actual fun createKeyValueStore(): KeyValueStore {
    val defaults = NSUserDefaults.standardUserDefaults
    return object : KeyValueStore {
        override fun getString(key: String): String? = defaults.stringForKey(key)
        override fun putString(key: String, value: String) {
            defaults.setObject(value, forKey = key)
        }
    }
}
