package org.example.project.ui.collage

/** Every image count the template picker offers, in the order the "N Photos" row shows them. */
internal val CollageImageCounts: List<Int> = listOf(2, 3, 4, 5, 6)

internal object CollageTemplates {

    fun getTemplatesByImageCount(count: Int): List<CollageTemplate> = when (count) {
        2 -> twoImageTemplates
        3 -> threeImageTemplates
        4 -> fourImageTemplates
        5 -> fiveImageTemplates
        6 -> sixImageTemplates
        else -> twoImageTemplates
    }

    fun findById(id: String): CollageTemplate? = CollageImageCounts
        .flatMap { getTemplatesByImageCount(it) }
        .find { it.id == id }

    private val twoImageTemplates = listOf(
        CollageTemplate(
            id = "2_side_by_side",
            label = "Side by Side",
            imageCount = 2,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.5f, 1f),
                CollageSlot(1, 0.5f, 0f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "2_top_bottom",
            label = "Top & Bottom",
            imageCount = 2,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 1f, 0.5f),
                CollageSlot(1, 0f, 0.5f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "2_large_left",
            label = "Focus Left",
            imageCount = 2,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.65f, 1f),
                CollageSlot(1, 0.65f, 0f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "2_large_right",
            label = "Focus Right",
            imageCount = 2,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.35f, 1f),
                CollageSlot(1, 0.35f, 0f, 1f, 1f),
            ),
        ),
    )

    private val threeImageTemplates = listOf(
        CollageTemplate(
            id = "3_top_large",
            label = "Featured Top",
            imageCount = 3,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 1f, 0.55f),
                CollageSlot(1, 0f, 0.55f, 0.5f, 1f),
                CollageSlot(2, 0.5f, 0.55f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "3_bottom_large",
            label = "Featured Bottom",
            imageCount = 3,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.5f, 0.45f),
                CollageSlot(1, 0.5f, 0f, 1f, 0.45f),
                CollageSlot(2, 0f, 0.45f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "3_left_large",
            label = "Featured Left",
            imageCount = 3,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.55f, 1f),
                CollageSlot(1, 0.55f, 0f, 1f, 0.5f),
                CollageSlot(2, 0.55f, 0.5f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "3_columns",
            label = "Triptych",
            imageCount = 3,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.33f, 1f),
                CollageSlot(1, 0.33f, 0f, 0.66f, 1f),
                CollageSlot(2, 0.66f, 0f, 1f, 1f),
            ),
        ),
    )

    private val fourImageTemplates = listOf(
        CollageTemplate(
            id = "4_grid",
            label = "Classic Grid",
            imageCount = 4,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.5f, 0.5f),
                CollageSlot(1, 0.5f, 0f, 1f, 0.5f),
                CollageSlot(2, 0f, 0.5f, 0.5f, 1f),
                CollageSlot(3, 0.5f, 0.5f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "4_left_stack",
            label = "Magazine Left",
            imageCount = 4,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.5f, 1f),
                CollageSlot(1, 0.5f, 0f, 1f, 0.33f),
                CollageSlot(2, 0.5f, 0.33f, 1f, 0.66f),
                CollageSlot(3, 0.5f, 0.66f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "4_top_feature",
            label = "Hero Top",
            imageCount = 4,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 1f, 0.5f),
                CollageSlot(1, 0f, 0.5f, 0.33f, 1f),
                CollageSlot(2, 0.33f, 0.5f, 0.66f, 1f),
                CollageSlot(3, 0.66f, 0.5f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "4_mosaic",
            label = "Mosaic",
            imageCount = 4,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.6f, 0.6f),
                CollageSlot(1, 0.6f, 0f, 1f, 0.4f),
                CollageSlot(2, 0.6f, 0.4f, 1f, 1f),
                CollageSlot(3, 0f, 0.6f, 0.6f, 1f),
            ),
        ),
    )

    private val fiveImageTemplates = listOf(
        CollageTemplate(
            id = "5_center_focus",
            label = "Center Stage",
            imageCount = 5,
            slots = listOf(
                CollageSlot(0, 0.2f, 0.2f, 0.8f, 0.8f),
                CollageSlot(1, 0f, 0f, 0.3f, 0.3f),
                CollageSlot(2, 0.7f, 0f, 1f, 0.3f),
                CollageSlot(3, 0f, 0.7f, 0.3f, 1f),
                CollageSlot(4, 0.7f, 0.7f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "5_cross",
            label = "Cross",
            imageCount = 5,
            slots = listOf(
                CollageSlot(0, 0.33f, 0f, 0.66f, 0.33f),
                CollageSlot(1, 0f, 0.33f, 0.33f, 0.66f),
                CollageSlot(2, 0.33f, 0.33f, 0.66f, 0.66f),
                CollageSlot(3, 0.66f, 0.33f, 1f, 0.66f),
                CollageSlot(4, 0.33f, 0.66f, 0.66f, 1f),
            ),
        ),
        CollageTemplate(
            id = "5_pyramid",
            label = "Pyramid",
            imageCount = 5,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.5f, 0.5f),
                CollageSlot(1, 0.5f, 0f, 1f, 0.5f),
                CollageSlot(2, 0f, 0.5f, 0.33f, 1f),
                CollageSlot(3, 0.33f, 0.5f, 0.66f, 1f),
                CollageSlot(4, 0.66f, 0.5f, 1f, 1f),
            ),
        ),
    )

    private val sixImageTemplates = listOf(
        CollageTemplate(
            id = "6_grid_2x3",
            label = "Grid 2×3",
            imageCount = 6,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.5f, 0.33f),
                CollageSlot(1, 0.5f, 0f, 1f, 0.33f),
                CollageSlot(2, 0f, 0.33f, 0.5f, 0.66f),
                CollageSlot(3, 0.5f, 0.33f, 1f, 0.66f),
                CollageSlot(4, 0f, 0.66f, 0.5f, 1f),
                CollageSlot(5, 0.5f, 0.66f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "6_grid_3x2",
            label = "Grid 3×2",
            imageCount = 6,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.33f, 0.5f),
                CollageSlot(1, 0.33f, 0f, 0.66f, 0.5f),
                CollageSlot(2, 0.66f, 0f, 1f, 0.5f),
                CollageSlot(3, 0f, 0.5f, 0.33f, 1f),
                CollageSlot(4, 0.33f, 0.5f, 0.66f, 1f),
                CollageSlot(5, 0.66f, 0.5f, 1f, 1f),
            ),
        ),
        CollageTemplate(
            id = "6_magazine",
            label = "Magazine",
            imageCount = 6,
            slots = listOf(
                CollageSlot(0, 0f, 0f, 0.5f, 0.6f),
                CollageSlot(1, 0.5f, 0f, 1f, 0.3f),
                CollageSlot(2, 0.5f, 0.3f, 1f, 0.6f),
                CollageSlot(3, 0f, 0.6f, 0.33f, 1f),
                CollageSlot(4, 0.33f, 0.6f, 0.66f, 1f),
                CollageSlot(5, 0.66f, 0.6f, 1f, 1f),
            ),
        ),
    )
}
