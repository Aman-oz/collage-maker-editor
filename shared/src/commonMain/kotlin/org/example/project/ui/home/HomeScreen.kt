package org.example.project.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.navigation.GalleryTarget
import org.example.project.ui.common.TopBarHeight
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.projects.ProjectsTabContent
import org.example.project.ui.projects.ProjectsViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
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

/** Brand purple for the "+" button and the selected tab; matches Splash/Onboarding. */
internal val HomeAccent = Color(0xFF8B5CF6)

/** How much a feature card squeezes in while pressed, and the spring that bounces it back. */
private const val FeatureCardPressedSqueeze = 0.05f
private val FeatureCardSpring = spring<Float>(dampingRatio = 0.35f, stiffness = 450f)
internal val HomeAccentDeep = Color(0xFF6D28D9)

private val CollageGradient = listOf(Color(0xFF4F46E5), Color(0xFFA855F7))
private val FreestyleGradient = listOf(Color(0xFF22D3EE), Color(0xFF3B82F6))
private val TemplatesGradient = listOf(Color(0xFFFF6BAE), Color(0xFFF43F8E))
private val EditorGradient = listOf(Color(0xFFFFA24C), Color(0xFFFF7A3D))
private val PremiumColor = Color(0xFFFF9F1C)

internal const val CollageMaxSelection = 8
internal const val FreestyleMaxSelection = 12

@Composable
fun HomeScreen(
    onOpenGallery: (maxSelection: Int, target: GalleryTarget) -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenFrames: () -> Unit,
    onOpenPip: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenProject: (imagePath: String) -> Unit,
    modifier: Modifier = Modifier,
    projectsViewModel: ProjectsViewModel = koinViewModel(),
) {
    val projects by projectsViewModel.projects.collectAsStateWithLifecycle()
    val thumbnails by projectsViewModel.thumbnails.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val openEditor = { onOpenGallery(1, GalleryTarget.Editor) }
    HomeContent(
        snackbarHostState = snackbarHostState,
        onCreateCollage = { onOpenGallery(CollageMaxSelection, GalleryTarget.Collage) },
        onOpenFreestyle = { onOpenGallery(FreestyleMaxSelection, GalleryTarget.Freestyle) },
        onOpenTemplates = onOpenTemplates,
        onOpenEditor = openEditor,
        // Filters is a tool inside the photo editor, so it starts from a photo pick and opens the
        // Filter tool over the editor straight away.
        onOpenFilters = { onOpenGallery(1, GalleryTarget.EditorFilter) },
        onOpenFrames = onOpenFrames,
        onOpenPip = onOpenPip,
        onOpenBgRemove = { onOpenGallery(1, GalleryTarget.BackgroundRemover) },
        onOpenSettings = onOpenSettings,
        onOpenPremium = onOpenPremium,
        projects = projects,
        thumbnails = thumbnails,
        onRequestThumbnail = projectsViewModel::loadThumbnail,
        onOpenProject = onOpenProject,
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
    onOpenPip: () -> Unit,
    onOpenBgRemove: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPremium: () -> Unit,
    projects: List<String>?,
    thumbnails: Map<String, ImageBitmap>,
    onRequestThumbnail: (path: String) -> Unit,
    onOpenProject: (path: String) -> Unit,
    modifier: Modifier = Modifier,
    initialTab: HomeTab = HomeTab.Home,
) {
    // Saved by name: an enum is not saveable on every platform, a String is.
    var selectedTabName by rememberSaveable { mutableStateOf(initialTab.name) }
    val selectedTab = HomeTab.valueOf(selectedTabName)

    // The bottom bar is Liquid Glass over the tab content, so the content is its liquefiable
    // backdrop; the bar itself sits outside it as a sibling drawn on top.
    val liquidState = rememberLiquidState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .liquefiable(liquidState)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            HomeTopBar(
                onPremium = onOpenPremium,
                onSettings = onOpenSettings,
            )
            when (selectedTab) {
                HomeTab.Home -> HomeTabContent(
                    onCreateCollage = onCreateCollage,
                    onOpenFreestyle = onOpenFreestyle,
                    onOpenTemplates = onOpenTemplates,
                    onOpenEditor = onOpenEditor,
                    onOpenPip = onOpenPip,
                    onOpenBgRemove = onOpenBgRemove,
                    onOpenFilters = onOpenFilters,
                    onOpenFrames = onOpenFrames,
                    modifier = Modifier.weight(1f),
                )
                HomeTab.Projects -> ProjectsTabContent(
                    projects = projects,
                    thumbnails = thumbnails,
                    onRequestThumbnail = onRequestThumbnail,
                    onOpenProject = onOpenProject,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            SnackbarHost(hostState = snackbarHostState)
            HomeBottomBar(
                liquidState = liquidState,
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
            .height(TopBarHeight)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Pic Collage Maker",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        BouncyIconButton(onClick = onPremium) {
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = "Premium",
                modifier = Modifier.size(28.dp),
            )
        }
        BouncyIconButton(onClick = onSettings) {
            // Tinted rather than drawn as-is: the vector's own fill is a fixed dark grey that
            // disappears on the dark theme.
            Icon(
                painter = painterResource(Res.drawable.ic_settings_icon),
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground,
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

        HomeToolsCard(
            modifier = Modifier.padding(top = 16.dp),
            tools = listOf(
                HomeTool("PIP", Res.drawable.ic_pip_icon, onOpenPip),
                HomeTool("BG Remove", Res.drawable.ic_bg_remover_icon, onOpenBgRemove),
                HomeTool("Filters", Res.drawable.ic_filter_icon, onOpenFilters),
                HomeTool("Frames", Res.drawable.ic_frame_icon, onOpenFrames),
            ),
        )
    }
}

/**
 * A gradient entry card. [vertical] stacks the icon above the title (square-ish cards); otherwise
 * they sit side by side (the wide Free Style card).
 *
 * Pressing squeezes the card in under a white ripple and a glassy highlight, with the icon zooming
 * a little; releasing springs it back with an overshoot. A tap holds that pressed state for
 * [ToolNavigationDelayMillis] before navigating, as the Tools card does: in this scrolling screen a
 * quick tap registers its press only for an instant, so without the hold it would barely show.
 * The card has no Liquid node inside it, so a `graphicsLayer` scale is safe here.
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    // Holds the pressed look through the navigation delay, and blocks a double tap meanwhile.
    var launching by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed || launching) 1f else 0f, FeatureCardSpring)
    val t = press.coerceIn(0f, 1f)
    val currentOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = modifier
            .graphicsLayer {
                // The underdamped spring carries this past 1 on release: the bounce.
                val scale = 1f - FeatureCardPressedSqueeze * press
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Brush.linearGradient(gradient), shape)
            .drawWithContent {
                drawContent()
                if (t <= 0f) return@drawWithContent
                // A glassy lift: an even wash plus a sheen over the top.
                drawRect(Color.White.copy(alpha = 0.1f * t))
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.28f * t),
                        0.45f to Color.White.copy(alpha = 0f),
                    ),
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),
                role = Role.Button,
            ) {
                if (launching) return@clickable
                launching = true
                scope.launch {
                    try {
                        delay(ToolNavigationDelayMillis)
                        currentOnClick()
                    } finally {
                        launching = false
                    }
                }
            }
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
                modifier = Modifier.size(iconSize).graphicsLayer {
                    val zoom = 1f + 0.12f * press
                    scaleX = zoom
                    scaleY = zoom
                },
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

/**
 * An [IconButton]-sized top-bar button with the Home cards' press feel: the icon squeezes in under a
 * round ripple, springs back past full size on release, and a tap holds the pressed look for
 * [ToolNavigationDelayMillis] before [onClick] runs, so a quick tap shows the whole bounce.
 */
@Composable
private fun BouncyIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    // Holds the pressed look through the navigation delay, and blocks a double tap meanwhile.
    var launching by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed || launching) 1f else 0f, FeatureCardSpring)
    val currentOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                role = Role.Button,
            ) {
                if (launching) return@clickable
                launching = true
                scope.launch {
                    try {
                        delay(ToolNavigationDelayMillis)
                        currentOnClick()
                    } finally {
                        launching = false
                    }
                }
            }
            .graphicsLayer {
                // The underdamped spring carries this past 1 on release: the bounce.
                val scale = 1f - IconButtonPressedSqueeze * press
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Icons are small, so they squeeze further than the cards to read as the same bounce. */
private const val IconButtonPressedSqueeze = 0.18f

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
            onOpenPip = {},
            onOpenBgRemove = {},
            onOpenSettings = {},
            onOpenPremium = {},
            projects = emptyList(),
            thumbnails = emptyMap(),
            onRequestThumbnail = {},
            onOpenProject = {},
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
