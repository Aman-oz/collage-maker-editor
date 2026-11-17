package org.example.project.ui.common

import org.example.project.AppLog
import org.example.project.i18n.tr

/**
 * The text to show the user for this failure: never the exception's own message, which can be a
 * raw server answer (`Request failed (401): {"error":...}`), a file path or a stack detail.
 *
 * - [network] `true` (a server request): a lost connection or timeout says so, so the user knows
 *   that retrying once online will help; any other failure (a server error, a bad answer) shows
 *   [fallback].
 * - [network] `false` (local work: opening, saving, sharing a file): always [fallback].
 *
 * The real error is logged under [tag] for debugging.
 */
fun Throwable.userMessage(fallback: String, network: Boolean = false, tag: String = "UserMessage"): String {
    val offline = network && isConnectionProblem()
    val shown = if (offline) tr("No internet connection. Please try again") else fallback
    AppLog.w(tag, "Showing \"$shown\" for ${this::class.simpleName}: $message", this)
    return shown
}

/**
 * Whether this failure (or one of its causes) is the connection itself: no network, the host not
 * found, a refused connection or a timeout. Matched by class name, since the exceptions differ per
 * engine (OkHttp on Android, NSURLSession via Darwin on iOS) and aren't all visible in common code.
 */
internal fun Throwable.isConnectionProblem(): Boolean =
    generateSequence(this) { it.cause }.take(MaxCauseDepth).any { error ->
        val name = error::class.simpleName.orEmpty()
        ConnectionErrorNames.any { name.contains(it) }
    }

private const val MaxCauseDepth = 8

private val ConnectionErrorNames = listOf(
    "Timeout", // HttpRequestTimeoutException, ConnectTimeoutException, SocketTimeoutException
    "UnknownHost",
    "UnresolvedAddress",
    "ConnectException",
    "NoRouteToHost",
    "SocketException",
    "IOException", // a request that broke off mid-way
    "DarwinHttpRequestException", // iOS: NSURLErrorNotConnectedToInternet and friends
)
