package org.example.project.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserMessagesTest {

    private class SocketTimeoutException : Exception("timeout after 15000 ms")
    private class UnknownHostException : Exception("Unable to resolve host \"api.example.com\"")

    private val offline = "No internet connection. Please try again"

    @Test
    fun serverErrorsNeverShowTheirOwnText() {
        val error = IllegalStateException("""Request failed (401): {"error":{"code":"UNAUTHORIZED","message":"Invalid API key"}}""")
        assertEquals("Couldn't load templates", error.userMessage("Couldn't load templates", network = true))
    }

    @Test
    fun connectionProblemsSaySo() {
        assertEquals(offline, SocketTimeoutException().userMessage("Couldn't load templates", network = true))
        // Wrapped by the client: the cause counts too.
        val wrapped = RuntimeException("request failed", UnknownHostException())
        assertTrue(wrapped.isConnectionProblem())
        assertEquals(offline, wrapped.userMessage("Couldn't load frames", network = true))
    }

    @Test
    fun localWorkAlwaysUsesTheFallback() {
        assertEquals("Could not save the image", SocketTimeoutException().userMessage("Could not save the image"))
        assertEquals("Could not open this image", IllegalArgumentException("/private/var/x.jpg: bad header").userMessage("Could not open this image"))
    }

    @Test
    fun otherFailuresAreNotConnectionProblems() {
        assertFalse(IllegalStateException("Request failed (500)").isConnectionProblem())
    }
}
