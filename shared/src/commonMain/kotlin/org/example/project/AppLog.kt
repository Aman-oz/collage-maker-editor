package org.example.project

/** Severity of an [AppLog] line; maps onto each platform's own levels. */
internal enum class LogLevel { Debug, Info, Warn, Error }

/**
 * Writes one line to the platform log: Logcat on Android, the Xcode / Console.app output on iOS.
 * Filter by [tag] (e.g. `adb logcat -s BgRemoverApi`).
 */
internal expect fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?)

/** Tiny multiplatform logger, so shared code can leave a trail without a logging dependency. */
internal object AppLog {
    fun d(tag: String, message: String) = platformLog(LogLevel.Debug, tag, message, null)
    fun i(tag: String, message: String) = platformLog(LogLevel.Info, tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = platformLog(LogLevel.Warn, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = platformLog(LogLevel.Error, tag, message, throwable)
}
