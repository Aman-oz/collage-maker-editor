package org.example.project.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.common.DialogShape
import org.example.project.ui.preview.ThemePreviews

/** One face per star, worst to best. The index + 1 is the rating reported to [RateUsDialog]'s onRate. */
private val RatingEmojis = listOf("😵", "😑", "🙂", "😀", "😍")

/** "How's your experience so far?" panel, shown inside a [GlassDialogHost]: pick a face (best preselected), then Rate Us. */
@Composable
internal fun RateUsDialog(
    onRate: (rating: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var rating by remember { mutableIntStateOf(RatingEmojis.size) }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "How’s your experience so far?",
            color = colors.onSurface,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "We would love to know!",
            color = colors.onSurface.copy(alpha = 0.75f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            RatingEmojis.forEachIndexed { index, emoji ->
                RatingFace(
                    emoji = emoji,
                    rating = index + 1,
                    selected = rating == index + 1,
                    onClick = { rating = index + 1 },
                )
            }
        }
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = { onRate(rating) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = SettingsAccent, contentColor = Color.White),
        ) {
            Text(text = "Rate Us", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onDismiss) {
            Text(text = "Maybe later", color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RatingFace(emoji: String, rating: Int, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        if (selected) SettingsAccent else SettingsAccent.copy(alpha = 0.14f),
    )
    val border by animateColorAsState(
        if (selected) SettingsAccent else SettingsAccent.copy(alpha = 0.3f),
    )
    // A little springy pop on the picked face.
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, border, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "$rating of ${RatingEmojis.size}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 26.sp)
    }
}

@Preview
@Composable
private fun RateUsDialogPreview() {
    ThemePreviews {
        Box(Modifier.padding(24.dp).background(MaterialTheme.colorScheme.surface, DialogShape)) {
            RateUsDialog(onRate = {}, onDismiss = {})
        }
    }
}
