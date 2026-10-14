package org.example.project.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals
import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.CentralAssetFieldsDto
import org.example.project.data.centraldata.CentralFrameDto
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.withInitialCategory

class HomeFeedRepositoryTest {

    private fun asset(folder: String) = CentralAssetDto(
        thumbnailUrl = "/static/central_data/$folder/birthday/thumb.png",
        moreFields = CentralAssetFieldsDto(frames = CentralFrameDto(imageUrl = "/static/central_data/$folder/birthday/frame.png")),
    )

    @Test
    fun homeSectionKind_readsTheCatalogFolderFromThePaths() {
        assertEquals(HomeSectionKind.Frames, listOf(asset("frames"), asset("frames")).homeSectionKind())
        assertEquals(HomeSectionKind.Templates, listOf(asset("templates"), asset("templates")).homeSectionKind())
    }

    @Test
    fun homeSectionKind_followsTheMajority() {
        assertEquals(HomeSectionKind.Frames, listOf(asset("frames"), asset("frames"), asset("templates")).homeSectionKind())
        assertEquals(HomeSectionKind.Templates, listOf(asset("frames"), asset("templates")).homeSectionKind())
    }

    @Test
    fun withInitialCategory_putsAHomeCategoryFirst() {
        val catalog = listOf(TemplateCategory("a", "Love"), TemplateCategory("b", "Travel"))
        val home = TemplateCategory("h", "Birthday")
        assertEquals(listOf(home) + catalog, withInitialCategory(catalog, home))
    }

    @Test
    fun withInitialCategory_keepsTheCatalogOrderWhenItHasTheCategory() {
        val catalog = listOf(TemplateCategory("a", "Love"), TemplateCategory("b", "Travel"))
        assertEquals(catalog, withInitialCategory(catalog, TemplateCategory("b", "Travel")))
        assertEquals(catalog, withInitialCategory(catalog, null))
    }
}
