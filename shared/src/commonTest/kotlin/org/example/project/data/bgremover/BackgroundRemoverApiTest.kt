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
}
