package org.example.project.ui.collage.frames

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import org.example.project.ui.collage.geom.PhotoItem
import org.example.project.ui.collage.geom.PointF
import org.example.project.ui.collage.geom.RectF
import org.example.project.ui.collage.geom.TemplateItem

/**
 * Created by admin on 7/4/2016.
 */
object ElevenFrameImage {

    fun collage_11_0(): TemplateItem {
        val item = FrameImageUtils.collage("collage_11_0.png")
        //first frame
        var photoItem = PhotoItem()
        photoItem.index = 0
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            0.0f,
            53.25f,
            52.28f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //second frame
        photoItem = PhotoItem()
        photoItem.index = 1
        photoItem.bound = getBounds(
            photoItem,
            53.25f,
            0.0f,
            45.23f,
            26.15f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //third frame
        photoItem = PhotoItem()
        photoItem.index = 2
        photoItem.bound = getBounds(
            photoItem,
            53.25f,
            26.15f,
            45.23f,
            26.15f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //fourth frame
        photoItem = PhotoItem()
        photoItem.index = 3
        photoItem.bound = getBounds(
            photoItem,
            99f,
            0.0f,
            61f,
            84f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //fifth frame
        photoItem = PhotoItem()
        photoItem.index = 4
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            52.27f,
            35.24f,
            41.20f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //sixth frame
        photoItem = PhotoItem()
        photoItem.index = 5
        photoItem.bound = getBounds(
            photoItem,
            35.24f,
            52.27f,
            63.34f,
            31.68f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //seventh frame
        photoItem = PhotoItem()
        photoItem.index = 6
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            93.56f,
            35.24f,
            37.66f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //eighth frame
        photoItem = PhotoItem()
        photoItem.index = 7
        photoItem.bound = getBounds(
            photoItem,
            35.24f,
            83.96f,
            124.75f,
            52.03f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //ninth frame
        photoItem = PhotoItem()
        photoItem.index = 8
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            131.22f,
            35.24f,
            28.77f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //tenth frame
        photoItem = PhotoItem()
        photoItem.index = 9
        photoItem.bound = getBounds(
            photoItem,
            35.24f,
            136f,
            82.78f,
            24f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //eleven frame
        photoItem =PhotoItem()
        photoItem.index = 10
        photoItem.bound = getBounds(
            photoItem,
            118.02f,
            136f,
            41.97f,
            24f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        return item
    }
    fun getBounds(
        photoItem: PhotoItem,
        bound_x: Float,
        bound_y: Float,
        bound_width: Float,
        bound_height: Float,
        frame_width: Float,
        frame_height: Float
    ): RectF {
        val relative_x = bound_x / frame_width
        val relative_y = bound_y / frame_height
        val relative_width = bound_width / frame_width
        val relative_height = bound_height / frame_height
        photoItem.bound[relative_x, relative_y, relative_x + relative_width] =
            relative_y + relative_height
        return photoItem.bound
    }
    fun collage_11_1(): TemplateItem {
        val item = FrameImageUtils.collage("collage_11_1.png")
        //first frame
        var photoItem = PhotoItem()
        photoItem.index = 0
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            0.0f,
            53.34f,
            41.74f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //second frame
        photoItem = PhotoItem()
        photoItem.index = 1
        photoItem.bound =getBounds(
            photoItem,
            53.34f,
            0.0f,
            53.34f,
            41.74f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //third frame
        photoItem = PhotoItem()
        photoItem.index = 2
        photoItem.bound = getBounds(
            photoItem,
            106.65f,
            0.0f,
            53.34f,
            41.74f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //fourth frame
        photoItem = PhotoItem()
        photoItem.index = 3
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            41.74f,
            36.68f,
            38.38f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //fifth frame
        photoItem = PhotoItem()
        photoItem.index = 4
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            80.12f,
            36.68f,
            38.37f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //sixth frame
        photoItem = PhotoItem()
        photoItem.index = 5
        photoItem.bound = getBounds(
            photoItem,
            36.68f,
            41.74f,
            86.62f,
            76.65f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //seventh frame
        photoItem = PhotoItem()
        photoItem.index = 6
        photoItem.bound = getBounds(
            photoItem,
            123.31f,
            41.74f,
            36.68f,
            38.38f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //eighth frame
        photoItem = PhotoItem()
        photoItem.index = 7
        photoItem.bound = getBounds(
            photoItem,
            123.31f,
            80.12f,
            36.68f,
            38.37f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //ninth frame
        photoItem = PhotoItem()
        photoItem.index = 8
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            118.5f,
            53.34f,
            41.74f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //tenth frame
        photoItem = PhotoItem()
        photoItem.index = 9
        photoItem.bound = getBounds(
            photoItem,
            53.34f,
            118.5f,
            53.31f,
            41.74f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //eleven frame
        photoItem = PhotoItem()
        photoItem.index = 10
        photoItem.bound = getBounds(
            photoItem,
            106.65f,
            118.5f,
            53.34f,
            41.74f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        return item
    }

    fun collage_11_2(): TemplateItem {
        val item = FrameImageUtils.collage("collage_11_2.png")
        //first frame
        var photoItem = PhotoItem()
        photoItem.index = 0
        photoItem.bound = getBounds(
            photoItem,
            36.46f,
            22.78f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //second frame
        photoItem =PhotoItem()
        photoItem.index = 1
        photoItem.bound = getBounds(
            photoItem,
            97.87f,
            22.78f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //third frame
        photoItem = PhotoItem()
        photoItem.index = 2
        photoItem.bound = getBounds(
            photoItem,
            5.74f,
            53.74f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //fourth frame
        photoItem = PhotoItem()
        photoItem.index = 3
        photoItem.bound = getBounds(
            photoItem,
            36.46f,
            53.74f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //fifth frame
        photoItem = PhotoItem()
        photoItem.index = 4
        photoItem.bound = getBounds(
            photoItem,
            67.18f,
            53.74f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //sixth frame
        photoItem =PhotoItem()
        photoItem.index = 5
        photoItem.bound = getBounds(
            photoItem,
            97.87f,
            53.74f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //seventh frame
        photoItem = PhotoItem()
        photoItem.index = 6
        photoItem.bound = getBounds(
            photoItem,
            128.59f,
            53.74f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //eighth frame
        photoItem = PhotoItem()
        photoItem.index = 7
        photoItem.bound = getBounds(
            photoItem,
            36.46f,
            84.68f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //ninth frame
        photoItem = PhotoItem()
        photoItem.index = 8
        photoItem.bound = getBounds(
            photoItem,
            67.18f,
            84.68f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //tenth frame
        photoItem = PhotoItem()
        photoItem.index = 9
        photoItem.bound = getBounds(
            photoItem,
            97.87f,
            84.68f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //eleven frame
        photoItem = PhotoItem()
        photoItem.index = 10
        photoItem.bound = getBounds(
            photoItem,
            67.18f,
            115.65f,
            25.87f,
            25.62f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        return item
    }
}
