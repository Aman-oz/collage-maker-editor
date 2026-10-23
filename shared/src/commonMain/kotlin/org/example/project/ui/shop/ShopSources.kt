package org.example.project.ui.shop

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.example.project.data.centraldata.CentralDataConfig
import org.example.project.data.fonts.FontLibrary
import org.example.project.ui.layouteditor.LayoutArtLibrary
import org.example.project.ui.stickers.StickerSubcategory
import org.example.project.ui.stickers.StickersRepository

/**
 * One tab's library, read through the repository its tool already uses, so the Shop and the tool
 * share one copy of each answer. Packs with nothing in them are left out.
 */
internal interface ShopSource {
    suspend fun categories(): List<ShopCategory>

    suspend fun packs(category: ShopCategory): List<ShopPack>

    /**
     * The New pill: each category's first pack. The server says nothing of when a pack was added, and
     * lists each category's newest first.
     */
    suspend fun newPacks(categories: List<ShopCategory>): List<ShopPack> = coroutineScope {
        categories.map { async { runCatching { firstPack(it) }.getOrNull() } }.awaitAll().filterNotNull()
    }

    suspend fun firstPack(category: ShopCategory): ShopPack? = packs(category).firstOrNull()
}

/** The Stickers tool's library: a pack is a sub-category (a tile), its items the stickers. */
internal class StickerShopSource(private val repository: StickersRepository) : ShopSource {

    override suspend fun categories(): List<ShopCategory> =
        repository.getCategories().map { ShopCategory(it.id, it.name) }

    override suspend fun packs(category: ShopCategory): List<ShopPack> = coroutineScope {
        repository.getSubcategories(category.id)
            .map { sub -> async { pack(sub) } }
            .awaitAll()
            .filter { it.items.isNotEmpty() }
    }

    // Only as far as the first sub-category with stickers: a category can have many.
    override suspend fun firstPack(category: ShopCategory): ShopPack? =
        repository.getSubcategories(category.id).firstNotNullOfOrNull { sub -> pack(sub).takeIf { it.items.isNotEmpty() } }

    private suspend fun pack(sub: StickerSubcategory) = ShopPack(
        id = sub.id,
        categoryId = sub.categoryId,
        name = sub.name,
        items = repository.getStickers(sub).map {
            ShopItem(id = it.id, name = it.name, thumbnailUrl = it.thumbnailUrl, fileUrl = it.imageUrl, isPremium = it.isPremium)
        },
    )
}

/**
 * One of the layout editor's picture libraries ([LayoutArtLibrary]): the frames (the Border tab) or
 * the backgrounds, under [superCategoryId]. It reads the whole library at once anyway.
 */
internal class ArtShopSource(private val library: LayoutArtLibrary, private val superCategoryId: String) : ShopSource {

    override suspend fun categories(): List<ShopCategory> =
        library.getCategories(superCategoryId).map { ShopCategory(it.id, it.name) }

    override suspend fun packs(category: ShopCategory): List<ShopPack> =
        library.getCategories(superCategoryId).firstOrNull { it.id == category.id }?.subcategories.orEmpty().map { sub ->
            ShopPack(
                id = sub.id,
                categoryId = category.id,
                name = sub.name,
                items = sub.items.map {
                    ShopItem(id = it.id, name = it.name, thumbnailUrl = it.thumbnailUrl, fileUrl = it.imageUrl, isPremium = it.isPremium)
                },
            )
        }

    companion object {
        fun backgrounds(library: LayoutArtLibrary) = ArtShopSource(library, CentralDataConfig.BACKGROUNDS_SUPER_CATEGORY_ID)
        fun borders(library: LayoutArtLibrary) = ArtShopSource(library, CentralDataConfig.BORDERS_SUPER_CATEGORY_ID)
    }
}

/** The Text tool's fonts: a category is a main category, a pack one of its sub-categories (a chip). */
internal class FontShopSource(private val library: FontLibrary) : ShopSource {

    override suspend fun categories(): List<ShopCategory> =
        library.catalog().sections.map { section ->
            // A section has no id of its own; its sub-categories all carry it.
            ShopCategory(section.subcategories.first().categoryId, section.title)
        }

    override suspend fun packs(category: ShopCategory): List<ShopPack> = coroutineScope {
        val section = library.catalog().sections.firstOrNull { it.subcategories.first().categoryId == category.id }
        section?.subcategories.orEmpty()
            .map { sub ->
                async {
                    ShopPack(
                        id = sub.id,
                        categoryId = sub.categoryId,
                        name = sub.name,
                        items = library.fonts(sub).map {
                            ShopItem(id = it.family, name = it.family, thumbnailUrl = "", fileUrl = it.fileUrl, isPremium = it.isPremium, font = it)
                        },
                    )
                }
            }
            .awaitAll()
            .filter { it.items.isNotEmpty() }
    }
}
