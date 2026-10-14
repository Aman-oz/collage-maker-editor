package org.example.project.data.collage

import org.example.project.data.centraldata.CentralAssetDto

/** A layout as the server describes it, its Dark and Light assets joined into one entry. */
internal data class RemoteCollage(
    val id: String,
    /** The layout name that keys its slot geometry, e.g. `collage_3_5`. */
    val name: String,
    val previewLight: String,
    val previewDark: String,
    val isPremium: Boolean,
    val sequence: Int,
)

/**
 * Joins the server's per-theme assets into one [RemoteCollage] per layout. The server lists every
 * layout twice, once under the `Dark` sub-category and once under `Light`, and the only thing tying
 * the pair together is the image's file name — whose extension can differ between the two
 * (`collage_2_7.webp` / `collage_2_7.png`), so the name is taken without it. A layout missing one
 * theme uses the other's preview for both; disabled assets are dropped. [absoluteUrl] resolves the
 * server's relative image paths.
 */
internal fun List<CentralAssetDto>.toRemoteCollages(absoluteUrl: (String) -> String): List<RemoteCollage> =
    filter { it.isEnabled }
        .groupBy { asset ->
            val file = asset.moreFields.image.firstOrNull()?.filename
                ?: asset.thumbnailUrl.orEmpty().substringAfterLast('/')
            file.substringBeforeLast('.')
        }
        .filterKeys { it.isNotEmpty() }
        .map { (name, assets) ->
            val dark = assets.firstOrNull { it.subcategoryName.equals("Dark", ignoreCase = true) }
            val light = assets.firstOrNull { it.subcategoryName.equals("Light", ignoreCase = true) }
            val primary = light ?: dark ?: assets.first()
            fun previewOf(asset: CentralAssetDto?) = asset?.let {
                absoluteUrl(it.moreFields.image.firstOrNull()?.url ?: it.thumbnailUrl.orEmpty())
            }
            val lightUrl = previewOf(light) ?: previewOf(primary).orEmpty()
            RemoteCollage(
                id = primary.id,
                name = name,
                previewLight = lightUrl,
                previewDark = previewOf(dark) ?: lightUrl,
                isPremium = assets.any { it.isPremium },
                sequence = primary.sequence,
            )
        }
