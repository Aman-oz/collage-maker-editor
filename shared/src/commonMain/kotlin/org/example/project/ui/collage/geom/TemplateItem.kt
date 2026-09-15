package org.example.project.ui.collage.geom

/**
 * One collage layout, ported from the LAS `TemplateItem`. [photoItemList] holds the slot geometry
 * (built by the frame generators); [preview] is the remote thumbnail URL and [isPremium] the lock
 * flag, both filled in from `collages.json`. [title] is the layout name, e.g. `collage_3_5`.
 */
class TemplateItem {
    var id: String = ""
    var title: String? = null
    var preview: String? = null
    var isPremium = false
    var isSelected = false
    val photoItemList = ArrayList<PhotoItem>()

    /** Photo count = number of slots; the picker groups layouts by this, like `CollageActivity`. */
    val imageCount: Int get() = photoItemList.size
}
