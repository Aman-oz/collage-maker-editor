package org.example.project.ui.bgremover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr
import org.example.project.ui.common.DialogShape
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.img_remove_bg_image

/** Width / height of `img_remove_bg_image`, so the before/after banner is never cropped. */
private const val BannerAspectRatio = 1056f / 696f

/**
 * The round X under the unlock panel, as in the LAS app: the only way to turn the offer down, since
 * a tap outside the panel does nothing.
 */
@Composable
internal fun AiMagicCloseButton(onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .padding(top = 16.dp)
            .springBounce(bounce)
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClickLabel = tr("Close"),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Close, contentDescription = tr("Close"), tint = Color(0xFF1F1D24))
    }
}

/** How wide the host may make the panel when [AiMagicUnlockDialog] is [wide]. */
internal val AiMagicWideDialogWidth = 600.dp

/**
 * Ai Magic's unlock panel for free users, shown inside a
 * [org.example.project.ui.common.GlassDialogHost] over the eraser: a before/after banner, then
 * "Unlock free" (one free removal) or "Get PRO" (the paywall).
 *
 * @param wide puts the banner beside the text and buttons rather than above them, for a phone on
 * its side: stacked, the banner alone takes most of the screen's height and pushes the buttons off it.
 */
@Composable
internal fun AiMagicUnlockDialog(onUnlockFree: () -> Unit, onGetPro: () -> Unit, wide: Boolean = false) {
    if (wide) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The host's glass panel clips its corners, so the banner runs edge to edge under them.
            AiMagicBanner(modifier = Modifier.weight(1f))
            AiMagicOffer(
                onUnlockFree = onUnlockFree,
                onGetPro = onGetPro,
                stackButtons = true,
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AiMagicBanner(modifier = Modifier.fillMaxWidth())
            AiMagicOffer(
                onUnlockFree = onUnlockFree,
                onGetPro = onGetPro,
                stackButtons = false,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun AiMagicBanner(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.img_remove_bg_image),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.aspectRatio(BannerAspectRatio),
    )
}

/** The title, the pitch and the two buttons, side by side or (in the narrow wide-layout column) stacked. */
@Composable
private fun AiMagicOffer(
    onUnlockFree: () -> Unit,
    onGetPro: () -> Unit,
    stackButtons: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = tr("Ai Magic Remover"),
            color = colors.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = tr("Unlock BG Remover by watching an ad,\nor upgrade to premium."),
            color = colors.onSurface.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(if (stackButtons) 16.dp else 24.dp))
        val unlockFree: @Composable (Modifier) -> Unit = { buttonModifier ->
            OutlinedButton(
                onClick = onUnlockFree,
                modifier = buttonModifier,
                shape = CircleShape,
                border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
            ) {
                Text(text = tr("Unlock free"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        val getPro: @Composable (Modifier) -> Unit = { buttonModifier ->
            Button(
                onClick = onGetPro,
                modifier = buttonModifier,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text(text = tr("Get PRO"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (stackButtons) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                unlockFree(Modifier.fillMaxWidth().height(44.dp))
                getPro(Modifier.fillMaxWidth().height(44.dp))
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                unlockFree(Modifier.weight(1f).height(52.dp))
                getPro(Modifier.weight(1f).height(52.dp))
            }
        }
    }
}

@Preview
@Composable
private fun AiMagicUnlockDialogPreview() {
    ThemePreviews {
        Box(Modifier.padding(24.dp).background(MaterialTheme.colorScheme.surface, DialogShape)) {
            AiMagicUnlockDialog(onUnlockFree = {}, onGetPro = {})
        }
    }
}
