package org.example.project.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.navigation.GalleryTarget
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_bg_remover_icon
import photocollagemaker.shared.generated.resources.ic_collage_icon
import photocollagemaker.shared.generated.resources.ic_editor_icon
import photocollagemaker.shared.generated.resources.ic_filter_icon
import photocollagemaker.shared.generated.resources.ic_frame_icon
import photocollagemaker.shared.generated.resources.ic_freestyle_icon
import photocollagemaker.shared.generated.resources.ic_pip_icon
import photocollagemaker.shared.generated.resources.ic_premium_icon
import photocollagemaker.shared.generated.resources.ic_settings_icon
import photocollagemaker.shared.generated.resources.ic_template_icon
import photocollagemaker.shared.generated.resources.img_no_projects

/** Brand purple for the "+" button and the selected tab; matches Splash/Onboarding. */
internal val HomeAccent = Color(0xFF8B5CF6)
internal val HomeAccentDeep = Color(0xFF6D28D9)

private val CollageGradient = listOf(Color(0xFF4F46E5), Color(0xFFA855F7))
private val FreestyleGradient = listOf(Color(0xFF22D3EE), Color(0xFF3B82F6))
private val TemplatesGradient = listOf(Color(0xFFFF6BAE), Color(0xFFF43F8E))
private val EditorGradient = listOf(Color(0xFFFFA24C), Color(0xFFFF7A3D))
private val PremiumColor = Color(0xFFFF9F1C)

private const val CollageMaxSelection = 8
private const val FreestyleMaxSelection = 12

@Composable
fun HomeScreen(
    onOpenGallery: (maxSelection: Int, target: GalleryTarget) -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openEditor = { onOpenGallery(1, GalleryTarget.Editor) }
    HomeContent(
        snackbarHostState = snackbarHostState,
        onCreateCollage = { onOpenGallery(CollageMaxSelection, GalleryTarget.Collage) },
        onOpenFreestyle = { onOpenGallery(FreestyleMaxSelection, GalleryTarget.Freestyle) },
        onOpenTemplates = onOpenTemplates,
        onOpenEditor = openEditor,
        // Filters and Frames are tools inside the photo editor, so they start from a photo pick.
        onOpenFilters = openEditor,
        onOpenFrames = openEditor,
        onComingSoon = { feature ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("$feature is coming soon")
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    snackbarHostState: SnackbarHostState,
    onCreateCollage: () -> Unit,
    onOpenFreestyle: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenFrames: () -> Unit,
    onComingSoon: (feature: String) -> Unit,
    modifier: Modifier = Modifier,
    initialTab: HomeTab = HomeTab.Home,
) {
    // Saved by name: an enum is not saveable on every platform, a String is.
    var selectedTabName by rememberSaveable { mutableStateOf(initialTab.name) }
    val selectedTab = HomeTab.valueOf(selectedTabName)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            HomeTopBar(
                onPremium = { onComingSoon("Premium") },
                onSettings = { onComingSoon("Settings") },
            )
            when (selectedTab) {
                HomeTab.Home -> HomeTabContent(
                    onCreateCollage = onCreateCollage,
                    onOpenFreestyle = onOpenFreestyle,
                    onOpenTemplates = onOpenTemplates,
                    onOpenEditor = onOpenEditor,
                    onOpenPip = { onComingSoon("PIP") },
                    onOpenBgRemove = { onComingSoon("BG Remove") },
                    onOpenFilters = onOpenFilters,
                    onOpenFrames = onOpenFrames,
                    modifier = Modifier.weight(1f),
                )
                HomeTab.Projects -> ProjectsTabContent(modifier = Modifier.weight(1f))
            }
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            SnackbarHost(hostState = snackbarHostState)
            HomeBottomBar(
                selectedTab = selectedTab,
                onSelectTab = { selectedTabName = it.name },
                onCreate = onCreateCollage,
            )
        }
    }
}

@Composable
private fun HomeTopBar(onPremium: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Pic Collage Maker",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onPremium) {
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = "Premium",
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = onSettings) {
            Image(
                painter = painterResource(Res.drawable.ic_settings_icon),
                contentDescription = "Settings",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun HomeTabContent(
    onCreateCollage: () -> Unit,
    onOpenFreestyle: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenPip: () -> Unit,
    onOpenBgRemove: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenFrames: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            // Leave room for the bottom bar that floats over this content.
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureCard(
                title = "Create Collages",
                icon = Res.drawable.ic_collage_icon,
                iconSize = 46.dp,
                gradient = CollageGradient,
                vertical = true,
                onClick = onCreateCollage,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeatureCard(
                    title = "Free Style",
                    icon = Res.drawable.ic_freestyle_icon,
                    iconSize = 32.dp,
                    gradient = FreestyleGradient,
                    vertical = false,
                    onClick = onOpenFreestyle,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1.1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeatureCard(
                        title = "Templates",
                        icon = Res.drawable.ic_template_icon,
                        iconSize = 30.dp,
                        gradient = TemplatesGradient,
                        vertical = true,
                        onClick = onOpenTemplates,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    FeatureCard(
                        title = "Editor",
                        icon = Res.drawable.ic_editor_icon,
                        iconSize = 24.dp,
                        gradient = EditorGradient,
                        vertical = true,
                        onClick = onOpenEditor,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }

        ToolsCard(
            modifier = Modifier.padding(top = 16.dp),
            tools = {
                ToolItem("PIP", Res.drawable.ic_pip_icon, onOpenPip)
                ToolItem("BG Remove", Res.drawable.ic_bg_remover_icon, onOpenBgRemove)
                ToolItem("Filters", Res.drawable.ic_filter_icon, onOpenFilters)
                ToolItem("Frames", Res.drawable.ic_frame_icon, onOpenFrames)
            },
        )
    }
}

/**
 * A gradient entry card. [vertical] stacks the icon above the title (square-ish cards); otherwise
 * they sit side by side (the wide Free Style card).
 */
@Composable
private fun FeatureCard(
    title: String,
    icon: DrawableResource,
    iconSize: Dp,
    gradient: List<Color>,
    vertical: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradient), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val label = @Composable {
            Text(
                text = title,
                color = Color.White,
                fontSize = if (vertical) 13.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        val image = @Composable {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
        if (vertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                image()
                label()
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                image()
                label()
            }
        }
    }
}

@Composable
private fun ToolsCard(tools: @Composable RowScope.() -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = shape, ambientColor = HomeAccent, spotColor = HomeAccent)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        content = tools,
    )
}

@Composable
private fun RowScope.ToolItem(label: String, icon: DrawableResource, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE9E9EE))),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
        }
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ProjectsTabContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.img_no_projects),
            contentDescription = "No projects yet",
            modifier = Modifier.size(84.dp),
        )
        Text(
            text = "No projects yet",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Your saved collages and edits will show up here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun HomeContentPreview(initialTab: HomeTab) {
    ThemePreviews {
        HomeContent(
            snackbarHostState = remember { SnackbarHostState() },
            onCreateCollage = {},
            onOpenFreestyle = {},
            onOpenTemplates = {},
            onOpenEditor = {},
            onOpenFilters = {},
            onOpenFrames = {},
            onComingSoon = {},
            initialTab = initialTab,
        )
    }
}

@Preview(widthDp = 810, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    HomeContentPreview(HomeTab.Home)
}

@Preview(widthDp = 810, heightDp = 852)
@Composable
private fun HomeScreenProjectsPreview() {
    HomeContentPreview(HomeTab.Projects)
}
