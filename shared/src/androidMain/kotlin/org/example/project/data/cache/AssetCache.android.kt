package org.example.project.data.cache

// On Android java.io.tmpdir is the app's own cache dir, so no Context is needed.
internal actual fun platformCacheDir(): String = System.getProperty("java.io.tmpdir")
