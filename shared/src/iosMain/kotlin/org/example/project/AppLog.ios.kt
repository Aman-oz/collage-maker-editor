package org.example.project

internal actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    // stdout reaches the Xcode console; the level prefix mirrors Logcat's so logs read the same.
    println("${level.name.first()}/$tag: $message${throwable?.let { "\n" + it.stackTraceToString() }.orEmpty()}")
}
