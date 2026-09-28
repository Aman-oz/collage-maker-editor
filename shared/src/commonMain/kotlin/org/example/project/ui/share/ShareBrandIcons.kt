package org.example.project.ui.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Simplified brand marks drawn in code, since the project bundles no brand artwork. Swap these
 * for the official assets from each brand's press kit before shipping.
 */

private val TileShape = RoundedCornerShape(10.dp)

/** The rounded tile each share target sits in. */
@Composable
internal fun ShareTargetIcon(target: ShareTarget, modifier: Modifier = Modifier) {
    val background = when (target) {
        ShareTarget.Instagram -> Brush.linearGradient(
            listOf(Color(0xFF515BD4), Color(0xFF8134AF), Color(0xFFDD2A7B), Color(0xFFF58529), Color(0xFFFEDA77)),
            start = Offset(Float.POSITIVE_INFINITY, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY),
        )
        ShareTarget.WhatsApp -> Brush.linearGradient(listOf(Color(0xFF25D366), Color(0xFF128C3E)))
        ShareTarget.Snapchat -> Brush.linearGradient(listOf(Color(0xFFFFFC00), Color(0xFFFFFC00)))
        ShareTarget.Facebook -> Brush.linearGradient(listOf(Color(0xFF3B5998), Color(0xFF2D4373)))
        ShareTarget.More -> Brush.linearGradient(listOf(Color(0xFFD9D9D9), Color(0xFFD9D9D9)))
    }
    Box(
        modifier = modifier.clip(TileShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        when (target) {
            ShareTarget.Instagram -> InstagramGlyph()
            ShareTarget.WhatsApp -> WhatsAppGlyph()
            ShareTarget.Snapchat -> SnapchatGlyph()
            ShareTarget.Facebook -> Text(
                text = "f",
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 9.dp),
                color = Color.White,
                fontSize = 42.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Black,
            )
            ShareTarget.More -> Icon(
                imageVector = Icons.Outlined.IosShare,
                contentDescription = null,
                tint = Color(0xFF1B1B1F),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Camera outline: rounded square, lens ring and flash dot. */
@Composable
private fun InstagramGlyph() {
    Canvas(modifier = Modifier.fillMaxSize().padding(8.5.dp)) {
        val stroke = size.minDimension * 0.1f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(size.minDimension * 0.3f),
            style = Stroke(stroke),
        )
        drawCircle(Color.White, radius = size.minDimension * 0.22f, style = Stroke(stroke))
        drawCircle(
            Color.White,
            radius = stroke * 0.7f,
            center = Offset(size.width * 0.76f, size.height * 0.24f),
        )
    }
}

/** Speech bubble ring with a tail at the bottom-left, around a handset. */
@Composable
private fun WhatsAppGlyph() {
    Box(modifier = Modifier.fillMaxSize().padding(7.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.09f
            val radius = size.minDimension / 2 - stroke
            drawCircle(Color.White, radius = radius, style = Stroke(stroke))
            val tail = Path().apply {
                moveTo(size.width * 0.16f, size.height * 0.66f)
                lineTo(size.width * 0.06f, size.height * 0.96f)
                lineTo(size.width * 0.36f, size.height * 0.86f)
                close()
            }
            drawPath(tail, Color.White)
        }
        Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** Ghost outline: dome head, side flaps and a scalloped hem. */
@Composable
private fun SnapchatGlyph() {
    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 8.5.dp)) {
        val w = size.width
        val h = size.height
        val ghost = Path().apply {
            moveTo(w * 0.22f, h * 0.42f)
            cubicTo(w * 0.2f, h * 0.05f, w * 0.8f, h * 0.05f, w * 0.78f, h * 0.42f)
            lineTo(w * 0.95f, h * 0.5f)
            lineTo(w * 0.78f, h * 0.6f)
            cubicTo(w * 0.82f, h * 0.72f, w * 0.9f, h * 0.78f, w, h * 0.82f)
            cubicTo(w * 0.85f, h * 0.9f, w * 0.72f, h * 0.86f, w * 0.64f, h * 0.95f)
            cubicTo(w * 0.56f, h * 1.02f, w * 0.44f, h * 1.02f, w * 0.36f, h * 0.95f)
            cubicTo(w * 0.28f, h * 0.86f, w * 0.15f, h * 0.9f, 0f, h * 0.82f)
            cubicTo(w * 0.1f, h * 0.78f, w * 0.18f, h * 0.72f, w * 0.22f, h * 0.6f)
            lineTo(w * 0.05f, h * 0.5f)
            close()
        }
        drawPath(ghost, Color.White)
        drawPath(ghost, Color.Black, style = Stroke(size.minDimension * 0.06f))
    }
}
