package org.example.project.ui.proeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.glassnav.GlassBottomNav
import org.example.project.ui.glassnav.GlassNavItem
import org.example.project.ui.preview.ThemePreviews

private val ProEditorBackground = Color(0xFF000000)

private val DiscoverCardGradients = listOf(
    listOf(Color(0xFFFF8A65), Color(0xFFFFB088)),
    listOf(Color(0xFF7C83FD), Color(0xFF5B63E8)),
    listOf(Color(0xFF4ECDC4), Color(0xFF2FA89E)),
    listOf(Color(0xFFEF5F77), Color(0xFFE13F63)),
    listOf(Color(0xFF6EE7A8), Color(0xFF34D399)),
    listOf(Color(0xFFFFB25C), Color(0xFFFFE066)),
    listOf(Color(0xFF5B9DF9), Color(0xFF3E6FE0)),
    listOf(Color(0xFFC968E8), Color(0xFFA34FD6)),
)

private val ProEditorNavItems = listOf(
    GlassNavItem(label = "Home", icon = Icons.Filled.Home),
    GlassNavItem(label = "Search", icon = Icons.Filled.Search),
    GlassNavItem(label = "Saved", icon = Icons.Filled.Favorite, badgeCount = 3),
    GlassNavItem(label = "Profile", icon = Icons.Filled.Person, showDot = true),
)

/**
 * Demo/showcase screen for the reusable [GlassBottomNav] module: a scrollable grid of decorative
 * cards behind a floating, frosted-glass bottom navigation bar. [org.example.project.ui.glassnav]
 * is theme-agnostic and self-contained, so it can be dropped into any other screen the same way.
 */
@Composable
fun ProEditorScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val hazeState = remember { HazeState() }
    var selectedIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProEditorBackground)
            .safeContentPadding(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProEditorHeader(onBack = onBack)
            }

            items(List(16) { DiscoverCardGradients[it % DiscoverCardGradients.size] }) { gradient ->
                DiscoverCard(gradient = gradient)
            }
        }

        GlassBottomNav(
            items = ProEditorNavItems,
            selectedIndex = selectedIndex,
            onItemSelected = { selectedIndex = it },
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        )
    }
}

@Composable
private fun ProEditorHeader(onBack: () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Discover",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Pro Editor",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun DiscoverCard(gradient: List<Color>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(gradient)),
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.85f)),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.55f)),
            )
        }
    }
}

@Preview
@Composable
private fun ProEditorScreenPreview() {
    ThemePreviews { ProEditorScreen(onBack = {}) }
}
