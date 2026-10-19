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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.i18n.tr
import org.example.project.ui.layouts.CollageLayout
import org.example.project.navigation.GalleryTarget
import org.example.project.ui.common.TopBarHeight
import org.example.project.ui.common.isSideBySideEditor
import org.example.project.ui.common.TopBarPremiumIconSize
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplateFrame
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
    onOpenTemplates: (category: TemplateCategory?) -> Unit,
    onOpenFrames: (category: TemplateCategory?) -> Unit,
    onOpenTemplateEditor: (TemplateFrame) -> Unit,
    onOpenFrameEditor: (TemplateFrame) -> Unit,
    onOpenPip: () -> Unit,
    onOpenLayouts: () -> Unit,
    onOpenLayout: (CollageLayout) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenProject: (imagePath: String) -> Unit,
    modifier: Modifier = Modifier,
    projectsViewModel: ProjectsViewModel = koinViewModel(),
    feedViewModel: HomeFeedViewModel = koinViewModel(),
) {
    val projects by projectsViewModel.projects.collectAsStateWithLifecycle()
    val feed by feedViewModel.uiState.collectAsStateWithLifecycle()
    // Rows that failed to load (offline at launch) are tried again each time Home comes back.
    LifecycleResumeEffect(feedViewModel) {
        feedViewModel.refresh()
        onPauseOrDispose {}
    }
    val thumbnails by projectsViewModel.thumbnails.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val openEditor = { onOpenGallery(1, GalleryTarget.Editor) }
    HomeContent(
        snackbarHostState = snackbarHostState,
        onCreateCollage = { onOpenGallery(CollageMaxSelection, GalleryTarget.Collage) },
        onOpenFreestyle = { onOpenGallery(FreestyleMaxSelection, GalleryTarget.Freestyle) },
        onOpenTemplates = { onOpenTemplates(null) },
        onOpenEditor = openEditor,
        // Filters is a tool inside the photo editor, so it starts from a photo pick and opens the
        // Filter tool over the editor straight away.
        onOpenFilters = { onOpenGallery(1, GalleryTarget.EditorFilter) },
        onOpenFrames = { onOpenFrames(null) },
        onOpenPip = onOpenPip,
        onOpenBgRemove = { onOpenGallery(1, GalleryTarget.BackgroundRemover) },
        feed = feed,
        onOpenLayouts = onOpenLayouts,
        onOpenLayout = onOpenLayout,
        // View All opens the row's catalog on its category; a thumbnail opens its editor.
        onViewAllFeedSection = { section ->
            when (section.kind) {
                HomeSectionKind.Templates -> onOpenTemplates(section.category)
                HomeSectionKind.Frames -> onOpenFrames(section.category)
            }
        },
        onOpenFeedFrame = { section, frame ->
            when (section.kind) {
                HomeSectionKind.Templates -> onOpenTemplateEditor(frame)
                HomeSectionKind.Frames -> onOpenFrameEditor(frame)
            }
        },
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
    feed: HomeFeedUiState,
    onOpenLayouts: () -> Unit,
    onOpenLayout: (CollageLayout) -> Unit,
    onViewAllFeedSection: (HomeSection) -> Unit,
    onOpenFeedFrame: (HomeSection, TemplateFrame) -> Unit,
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
                    feed = feed,
                    onOpenLayouts = onOpenLayouts,
                    onOpenLayout = onOpenLayout,
                    onViewAllFeedSection = onViewAllFeedSection,
                    onOpenFeedFrame = onOpenFeedFrame,
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
                contentDescription = tr("Premium"),
                modifier = Modifier.size(TopBarPremiumIconSize),
            )
        }
        BouncyIconButton(onClick = onSettings) {
            // Tinted rather than drawn as-is: the vector's own fill is a fixed dark grey that
            // disappears on the dark theme.
            Icon(
                painter = painterResource(Res.drawable.ic_settings_icon),
                contentDescription = tr("Settings"),
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
    feed: HomeFeedUiState,
    onOpenLayouts: () -> Unit,
    onOpenLayout: (CollageLayout) -> Unit,
    onViewAllFeedSection: (HomeSection) -> Unit,
    onOpenFeedFrame: (HomeSection, TemplateFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The cards are sized for a phone. On a wider screen (a tablet) they grow with the width, so
    // they keep the phone's proportions instead of only stretching sideways.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // A phone on its side is wide but short: grown with the width, the cards alone would be
        // taller than the screen. There they keep the phone's size and the tools card moves up
        // beside them, so the template rows start within the first screen.
        val landscapePhone = isSideBySideEditor(maxWidth, maxHeight)
        val scale = if (landscapePhone) {
            1f
        } else {
            ((maxWidth - HomeContentPadding * 2) / HomePhoneContentWidth).coerceIn(1f, HomeMaxScale)
        }
        // Text and the small tool circles grow more gently than the cards, or they would look bloated.
        val softScale = 1f + (scale - 1f) * 0.5f
        val featureCards: @Composable (Modifier) -> Unit = { cardsModifier ->
            HomeFeatureCards(
                scale = scale,
                softScale = softScale,
                onCreateCollage = onCreateCollage,
                onOpenFreestyle = onOpenFreestyle,
                onOpenTemplates = onOpenTemplates,
                onOpenEditor = onOpenEditor,
                modifier = cardsModifier,
            )
        }
        val toolsCard: @Composable (Modifier) -> Unit = { cardModifier ->
            HomeToolsCard(
                scale = softScale,
                modifier = cardModifier,
                tools = listOf(
                    HomeTool(tr("PIP"), Res.drawable.ic_pip_icon, onOpenPip),
                    HomeTool(tr("BG Remove"), Res.drawable.ic_bg_remover_icon, onOpenBgRemove),
                    HomeTool(tr("Filters"), Res.drawable.ic_filter_icon, onOpenFilters),
                    HomeTool(tr("Frames"), Res.drawable.ic_frame_icon, onOpenFrames),
                ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Leave room for the bottom bar that floats over this content.
                .padding(start = HomeContentPadding, end = HomeContentPadding, top = 16.dp, bottom = 120.dp),
        ) {
            if (landscapePhone) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(HomeFeatureCardsHeight),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    featureCards(Modifier.weight(1f).fillMaxHeight())
                    toolsCard(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                featureCards(Modifier.fillMaxWidth().height(HomeFeatureCardsHeight * scale))
                toolsCard(Modifier.padding(top = 16.dp))
            }

            HomeLayoutsSection(
                layouts = feed.layouts,
                isLoading = feed.isLoadingLayouts,
                scale = softScale,
                onViewAll = onOpenLayouts,
                onLayoutClick = onOpenLayout,
            )

            HomeFeedSections(
                uiState = feed,
                scale = softScale,
                onViewAll = onViewAllFeedSection,
                onFrameClick = onOpenFeedFrame,
            )
        }
    }
}

/** The four gradient cards: Create Collages on the left; Free Style, Templates and Editor on the right. */
@Composable
private fun HomeFeatureCards(
    scale: Float,
    softScale: Float,
    onCreateCollage: () -> Unit,
    onOpenFreestyle: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FeatureCard(
            title = tr("Create Collages"),
            icon = Res.drawable.ic_collage_icon,
            iconSize = 60.dp * scale,
            gradient = CollageGradient,
            vertical = true,
            textScale = softScale,
            onClick = onCreateCollage,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureCard(
                title = tr("Free Style"),
                icon = Res.drawable.ic_freestyle_icon,
                iconSize = 36.dp * scale,
                gradient = FreestyleGradient,
                vertical = false,
                textScale = softScale,
                onClick = onOpenFreestyle,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Row(
                modifier = Modifier.fillMaxWidth().weight(1.1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeatureCard(
                    title = tr("Templates"),
                    icon = Res.drawable.ic_template_icon,
                    iconSize = 34.dp * scale,
                    gradient = TemplatesGradient,
                    vertical = true,
                    iconGap = 2.dp * scale,
                    textScale = softScale,
                    onClick = onOpenTemplates,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                FeatureCard(
                    title = tr("Editor"),
                    icon = Res.drawable.ic_editor_icon,
                    iconSize = 34.dp * scale,
                    gradient = EditorGradient,
                    vertical = true,
                    iconGap = 2.dp * scale,
                    textScale = softScale,
                    onClick = onOpenEditor,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

/** The feature cards' height on a phone; a tablet scales it. */
private val HomeFeatureCardsHeight = 160.dp

/** The Home content's side padding. */
internal val HomeContentPadding = 20.dp

/** The content width the Home cards were designed at (a 412dp phone less its padding). */
private val HomePhoneContentWidth = 372.dp

/** The most the cards grow, so a landscape tablet doesn't blow them up to fill the screen height. */
private const val HomeMaxScale = 2f

/**
 * A gradient entry card. [vertical] stacks the icon above the title (square-ish cards); otherwise
 * they sit side by side (the wide Free Style card). [iconGap] is the space between the stacked icon
 * and title; the small Templates and Editor cards tighten it.
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
    iconGap: Dp = 8.dp,
    textScale: Float = 1f,
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
                fontSize = (if (vertical) 15.sp else 16.sp) * textScale,
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
                verticalArrangement = Arrangement.spacedBy(iconGap),
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
            feed = HomeFeedUiState(),
            onOpenLayouts = {},
            onOpenLayout = {},
            onViewAllFeedSection = {},
            onOpenFeedFrame = { _, _ -> },
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
