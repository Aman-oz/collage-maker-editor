package org.example.project.ui.language

import org.example.project.ui.common.bubbleClick
import org.example.project.ui.common.rememberBubbleClick
import org.example.project.ui.theme.Brand
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.topBar
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

/** Accent for the Done button, the selected card outline and the selected radio. */
private val LanguageAccent = Color(0xFF8B5CF6)

private val CardShape = RoundedCornerShape(percent = 50)

@Composable
fun LanguageScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: LanguageViewModel = koinViewModel(),
) {
    val selectedCode by viewModel.selectedCode.collectAsStateWithLifecycle()

    LanguageContent(
        languages = viewModel.languages,
        selectedCode = selectedCode,
        onSelect = viewModel::select,
        onDone = {
            viewModel.save()
            onDone()
        },
        showBack = showBack,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun LanguageContent(
    languages: List<AppLanguage>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
) {
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().selectableGroup(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                LanguageHeader(showBack = showBack, onBack = onBack, onDone = onDone)
            }
            items(languages, key = { it.code }) { language ->
                LanguageRow(
                    language = language,
                    selected = language.code == selectedCode,
                    onClick = { onSelect(language.code) },
                )
            }
        }

        // Fades the list out into the background at the bottom edge, as in the design.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .background(Brush.verticalGradient(listOf(background.copy(alpha = 0f), background))),
        )
    }
}

@Composable
private fun LanguageHeader(showBack: Boolean, onBack: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier.topBar(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            GlassTopBarButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                onClick = onBack,
                contentColor = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(16.dp))
        }
        Text(
            text = "Select Language",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        val bubble = rememberBubbleClick()
        Button(
            onClick = { bubble.tap(onDone) },
            modifier = Modifier.height(32.dp).bubbleClick(bubble, Brand),
            interactionSource = bubble.interactionSource,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 22.dp),
            colors = ButtonDefaults.buttonColors(
                // Primary, like every other Done button.
                containerColor = Brand,
                contentColor = Color.White,
            ),
        ) {
            Text(text = "Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        if (selected) LanguageAccent else colors.onSurface.copy(alpha = 0.55f),
    )
    // Soft top-to-bottom sheen that gives the cards their raised look; tinted when selected.
    val top = if (selected) lerp(colors.surface, LanguageAccent, 0.04f) else colors.surface
    val bottom = lerp(colors.surface, colors.onSurface, 0.06f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CardShape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, borderColor, CardShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = language.flag,
            modifier = Modifier.width(28.dp),
            fontSize = 20.sp,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = language.nativeName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
            maxLines = 1,
        )
        LanguageRadio(selected = selected)
    }
}

/** Radio indicator drawn to the design: accent ring + dot when selected, plain ring otherwise. */
@Composable
private fun LanguageRadio(selected: Boolean) {
    val ringColor by animateColorAsState(
        if (selected) LanguageAccent else MaterialTheme.colorScheme.onSurface,
    )
    val dotSize by animateDpAsState(if (selected) 10.dp else 0.dp)

    Box(
        modifier = Modifier
            .size(20.dp)
            .border(1.5.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(LanguageAccent, CircleShape),
        )
    }
}

@Preview
@Composable
private fun LanguageScreenPreview() {
    ThemePreviews {
        LanguageContent(
            languages = SupportedLanguages,
            selectedCode = "en",
            onSelect = {},
            onDone = {},
        )
    }
}
