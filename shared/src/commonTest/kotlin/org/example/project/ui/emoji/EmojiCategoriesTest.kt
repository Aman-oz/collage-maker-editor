package org.example.project.ui.emoji

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmojiCategoriesTest {

    @Test
    fun everyCategoryHasEmojis() {
        for (category in EmojiCategory.entries) {
            val emojis = EmojisByCategory[category]
            assertTrue(!emojis.isNullOrEmpty(), "category $category has no emojis")
        }
    }

    @Test
    fun everyCategoryEntryHasAListMapped() {
        assertEquals(EmojiCategory.entries.size, EmojisByCategory.size)
    }

    @Test
    fun noEmojiListHasDuplicates() {
        for ((category, emojis) in EmojisByCategory) {
            assertEquals(emojis.size, emojis.toSet().size, "duplicate emoji within $category")
        }
    }

    @Test
    fun placedEmoji_defaultsToCenteredAndUnscaled() {
        val placed = PlacedEmoji(id = 1L, emoji = "😀")
        assertEquals(0.5f, placed.offsetFraction.x)
        assertEquals(0.5f, placed.offsetFraction.y)
        assertEquals(1f, placed.scale)
    }

    @Test
    fun emojiScaleRange_containsDefaultScale() {
        assertTrue(1f in EmojiScaleRange)
    }
}
