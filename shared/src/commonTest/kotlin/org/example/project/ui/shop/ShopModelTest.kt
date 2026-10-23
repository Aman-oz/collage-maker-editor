package org.example.project.ui.shop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopModelTest {

    private fun item(name: String) = ShopItem(id = name, name = name, thumbnailUrl = "", fileUrl = "", isPremium = false)

    private val animals = ShopCategory("1", "Animals")
    private val food = ShopCategory("2", "Food")
    private val catalog = listOf(
        animals to listOf(ShopPack("a", "1", "Bears and Pandas", listOf(item("teddy"))), ShopPack("b", "1", "Capybara", listOf(item("bath")))),
        food to listOf(ShopPack("c", "2", "Fruit", listOf(item("Panda bun")))),
    )

    @Test
    fun searchShopPacks_matchesPackNameIgnoringCase() {
        assertEquals(listOf("b"), searchShopPacks(catalog, "capy").map { it.id })
    }

    @Test
    fun searchShopPacks_matchesItemNames() {
        assertEquals(listOf("a", "c"), searchShopPacks(catalog, "panda").map { it.id })
    }

    @Test
    fun searchShopPacks_categoryNameMatchesItsWholeCategory() {
        assertEquals(listOf("c"), searchShopPacks(catalog, " food ").map { it.id })
    }

    @Test
    fun searchShopPacks_blankQueryMatchesNothing() {
        assertTrue(searchShopPacks(catalog, "  ").isEmpty())
    }

    @Test
    fun shopPicks_takeOnlyForItsTabAndOnlyOnce() {
        val picks = ShopPicks()
        picks.post(ShopPick(ShopTab.Fonts, "c", "p"))
        assertNull(picks.take(ShopTab.Stickers))
        assertEquals("p", picks.take(ShopTab.Fonts)?.packId)
        assertNull(picks.take(ShopTab.Fonts))
    }
}
