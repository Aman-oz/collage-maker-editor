package org.example.project.data.bgremover

import kotlin.test.Test
import kotlin.test.assertEquals

class BackgroundRemoverApiTest {

    @Test
    fun joinUrl_usesExactlyOneSlash() {
        assertEquals("http://host:9000/bg-remover/remove", joinUrl("http://host:9000", "bg-remover/remove"))
        assertEquals("http://host:9000/bg-remover/remove", joinUrl("http://host:9000/", "/bg-remover/remove"))
    }

    @Test
    fun resolveCutOutUrl_keepsAbsoluteUrls() {
        assertEquals("https://cdn.example/out.png", resolveCutOutUrl("http://host", "https://cdn.example/out.png"))
        assertEquals("http://cdn.example/out.png", resolveCutOutUrl("http://host", "http://cdn.example/out.png"))
    }

    @Test
    fun resolveCutOutUrl_resolvesRelativePathsAgainstBase() {
        assertEquals("http://host/media/out.png", resolveCutOutUrl("http://host/", "/media/out.png"))
    }

    @Test
    fun resolveRedirect_takesAbsoluteLocationsAsIs() {
        // The live server's answer to the old plain-HTTP upload URL.
        assertEquals(
            "https://aiapps.example/api/bg-remover/remove",
            resolveRedirect("http://aiapps.example/api/bg-remover/remove", "https://aiapps.example/api/bg-remover/remove"),
        )
    }

    @Test
    fun resolveRedirect_resolvesRelativeLocationsAgainstTheCurrentUrl() {
        assertEquals("http://host/v2/remove", resolveRedirect("http://host/api/bg-remover/remove", "/v2/remove"))
    }
}
