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
object TweleveFrameImage {

    fun collage_12_0(): TemplateItem {
        val item = FrameImageUtils.collage("collage_12_0.png")
        //first frame
        var photoItem = PhotoItem()
        photoItem.index = 0
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            0.0f,
            53.25f,
            52.29f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //second frame
        //second frame
        photoItem = PhotoItem()
        photoItem.index = 1
        photoItem.bound = getBounds(
            photoItem,
            53.25f,
            0.0f,
            45.33f,
            26.14f,
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
            26.14f,
            45.33f,
            26.14f,
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
            61.4f,
            83.95f,
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
            52.29f,
            35.26f,
            41.25f,
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
            37.66f,
            24f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)

        //Eleven frame
        photoItem = PhotoItem()
        photoItem.index = 10
        photoItem.bound = getBounds(
            photoItem,
            72.92f,
            136f,
            45.0f,
            24f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //twelve frame
        photoItem = PhotoItem()
        photoItem.index = 11
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

    fun collage_12_1(): TemplateItem {
        val item = FrameImageUtils.collage("collage_12_1.png")
        //first frame
        var photoItem = PhotoItem()
        photoItem.index = 0
        photoItem.bound = getBounds(
            photoItem,
            0.0f,
            0.0f,
            40f,
            48.05f,
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
            40f,
            0.0f,
            40f,
            48.05f,
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
            80f,
            0.0f,
            40f,
            48.05f,
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
            120f,
            0f,
            40f,
            48.05f,
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
        photoItem.bound =getBounds(
            photoItem,
            0.0f,
            48.05f,
            80f,
            31.94f,
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
            80f,
            48.05f,
            80f,
            31.94f,
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
            0f,
            80f,
            80f,
            31.94f,
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
            80f,
            80f,
            80f,
            31.94f,
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
            111.94f,
            40f,
            48.05f,
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
            40f,
            111.94f,
            40f,
            48.05f,
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
            80f,
            111.94f,
            40f,
            48.05f,
            160f,
            160f
        )
        photoItem.pointList.add(PointF(0f, 0f))
        photoItem.pointList.add(PointF(1f, 0f))
        photoItem.pointList.add(PointF(1f, 1f))
        photoItem.pointList.add(PointF(0f, 1f))
        item.photoItemList.add(photoItem)
        //Twelve frame
        photoItem = PhotoItem()
        photoItem.index = 11
        photoItem.bound = getBounds(
            photoItem,
            120f,
            111.94f,
            40f,
            48.05f,
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

    fun collage_12_2(): TemplateItem {
        val item = FrameImageUtils.collage("collage_12_2.png")
        //first frame
        var photoItem = PhotoItem()
        photoItem.index = 0
        photoItem.bound = getBounds(
            photoItem,
            4.55f,
            34.54f,
            28.78f,
            45.57f,
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
        photoItem.shrinkMethod = PhotoItem.SHRINK_METHOD_3_8
        photoItem.bound = getBounds(
            photoItem,
            39.82f,
            15.35f,
            32.86f,
            32.86f,
            160f,
            160f
        )
        photoItem.path = Path()
        photoItem.path!!.addOval(Rect(0f, 0f, 512f, 512f))
        photoItem.pathRatioBound = RectF(0.0f, 0f, 1f, 0f)
        photoItem.pathInCenterVertical = true
        item.photoItemList.add(photoItem)
        //third frame
        photoItem = PhotoItem()
        photoItem.index = 2
        photoItem.bound = getBounds(
            photoItem,
            77.96f,
            22.78f,
            24.46f,
            32.62f,
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
            108.09f,
            34.54f,
            23.74f,
            28.78f,
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
            41.59f,
            50.13f,
            33.58f,
            54.93f,
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
            80.35f,
            67.64f,
            24.46f,
            32.62f,
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
            108.09f,
            69.56f,
            27.82f,
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
            139.61f,
            79.64f,
            17.51f,
            31.18f,
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
            4.55f,
            86.83f,
            33.58f,
            55.65f,
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
            47.01f,
            112.98f,
            29.98f,
            29.05f,
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
        photoItem.shrinkMethod = PhotoItem.SHRINK_METHOD_3_8
        photoItem.bound = getBounds(
            photoItem,
            80.11f,
            112.05f,
            32.86f,
            32.86f,
            160f,
            160f
        )
        photoItem.path = Path()
        photoItem.path!!.addOval(Rect(0f, 0f, 512f, 512f))
        photoItem.pathRatioBound = RectF(0.0f, 0f, 1f, 0f)
        photoItem.pathInCenterVertical = true
        item.photoItemList.add(photoItem)
        //twelve frame
        photoItem = PhotoItem()
        photoItem.index = 11
        photoItem.bound = getBounds(
            photoItem,
            115.86f,
            114.66f,
            23.74f,
            30.22f,
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
        frame_height: Float): RectF {
        val relative_x = bound_x / frame_width
        val relative_y = bound_y / frame_height
        val relative_width = bound_width / frame_width
        val relative_height = bound_height / frame_height
        photoItem.bound[relative_x, relative_y, relative_x + relative_width] =
            relative_y + relative_height
        return photoItem.bound
    }
}
