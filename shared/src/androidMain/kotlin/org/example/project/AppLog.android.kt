package org.example.project

import android.util.Log

internal actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    try {
        when (level) {
            LogLevel.Debug -> Log.d(tag, message, throwable)
            LogLevel.Info -> Log.i(tag, message, throwable)
            LogLevel.Warn -> Log.w(tag, message, throwable)
            LogLevel.Error -> Log.e(tag, message, throwable)
        }
    } catch (_: RuntimeException) {
        // JVM host tests run against a stubbed android.jar whose Log methods throw "not mocked".
        println("${level.name.first()}/$tag: $message${throwable?.let { "\n" + it.stackTraceToString() }.orEmpty()}")
    }
}
