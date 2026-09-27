package org.example.project.ui.premium

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.settings.SettingsAccent
import org.example.project.ui.settings.SettingsLinks
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_onboarding_collage_2
import photocollagemaker.shared.generated.resources.ic_onboarding_freestyle
import photocollagemaker.shared.generated.resources.ic_onboarding_templates
import photocollagemaker.shared.generated.resources.ic_premium_icon

/** Brand purple: feature icons, plan selection and badges. */
private val PremiumPrimary = SettingsAccent
// Chrome follows the app theme (AppTheme light/dark); only the brand accents are fixed.
private val PremiumText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val PremiumSubtext: Color @Composable get() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
private val PremiumDivider: Color @Composable get() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)

/**
 * Start Premium fill: the purple → gold → yellow sweep of `img_premium_card`. The magenta stop
 * keeps the purple-to-gold blend from going muddy brown in the middle, as it does on the card.
 */
private val StartPremiumGradient = Brush.horizontalGradient(
    0f to Color(0xFF7C3AED),
    0.38f to Color(0xFFD846C8),
    0.72f to Color(0xFFFFA62B),
    1f to Color(0xFFFFD84A),
)

private val PlanShape = RoundedCornerShape(16.dp)
private val StartButtonShape = RoundedCornerShape(percent = 50)

internal enum class PremiumPlan(
    val title: String,
    val subtitle: String,
    val price: String,
    val period: String,
    val badge: String? = null,
    val trial: String? = null,
) {
    Weekly("Weekly", "Then \$4.99/week", "\$4.99", "/week", badge = "Most Popular", trial = "3-Day Free Trial"),
    Monthly("Monthly", "\$9.99 / month", "\$9.99", "/month"),
    Annual("Annual", "\$49.99 / year", "\$49.99", "/year", badge = "Best Value"),
}

private class PremiumFeature(val icon: ImageVector, val label: String)

private val PremiumFeatures = listOf(
    PremiumFeature(Icons.Outlined.Block, "Remove\nAds"),
    PremiumFeature(Icons.Outlined.GridView, "Unlimited\nCollage Creation"),
    PremiumFeature(Icons.Outlined.Layers, "Premium\nTemplates"),
    PremiumFeature(Icons.Outlined.AutoAwesome, "Premium Filters\n& Effects"),
    PremiumFeature(Icons.Outlined.FontDownload, "Exclusive Fonts\n& Stickers"),
    PremiumFeature(Icons.Outlined.HighQuality, "High-Quality\nExport"),
)

/** Features laid out per row, as in the design: four on top, the rest centered underneath. */
private const val FeaturesPerRow = 4

@Composable
fun PremiumScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // Billing is not wired up yet; every purchase action explains itself instead of doing nothing.
    PremiumContent(
        snackbarHostState = snackbarHostState,
        onClose = onClose,
        onStart = { plan -> showMessage("${plan.title} plan purchase is coming soon") },
        onRestore = { showMessage("Restore purchase is coming soon") },
        onTermsAndPrivacy = {
            val url = SettingsLinks.PrivacyPolicyUrl.ifBlank { SettingsLinks.TermsOfUseUrl }
            if (url.isBlank()) showMessage("Terms & Privacy is coming soon") else uriHandler.openUri(url)
        },
        modifier = modifier,
    )
}

@Composable
private fun PremiumContent(
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onStart: (PremiumPlan) -> Unit,
    onRestore: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Saved by name: an enum is not saveable on every platform, a String is.
    var selectedName by rememberSaveable { mutableStateOf(PremiumPlan.Weekly.name) }
    val selected = PremiumPlan.valueOf(selectedName)

    val background = MaterialTheme.colorScheme.background
    // A light background washes the glow out less, so it needs far less of it.
    val glowAlpha = if (background.luminance() < 0.5f) 0.38f else 0.16f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .drawBehind {
                // Soft brand glow behind the title and hero, fading out before the plans.
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(PremiumPrimary.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.2f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PremiumHeader(onClose = onClose)
                Spacer(Modifier.height(12.dp))
                PremiumHero()
                Spacer(Modifier.height(20.dp))
                PremiumFeatureGrid()
                Spacer(Modifier.height(18.dp))
                PremiumPlan.entries.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        selected = plan == selected,
                        onSelect = { selectedName = plan.name },
                    )
                }
            }
            PremiumFooter(
                onStart = { onStart(selected) },
                onRestore = onRestore,
                onTermsAndPrivacy = onTermsAndPrivacy,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp),
        )
    }
}

@Composable
private fun PremiumHeader(onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PremiumText.copy(alpha = 0.08f))
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = PremiumText, modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Unlock Premium",
                color = PremiumText,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Create without limits",
                color = PremiumSubtext,
                fontSize = 15.sp,
            )
        }
    }
}

/** Three tilted polaroids, the middle one on top with a handwritten caption. */
@Composable
private fun PremiumHero() {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Polaroid(
            image = Res.drawable.ic_onboarding_templates,
            width = 112.dp,
            height = 128.dp,
            modifier = Modifier.offset(x = (-104).dp, y = 8.dp).rotate(-9f),
        )
        Polaroid(
            image = Res.drawable.ic_onboarding_freestyle,
            width = 112.dp,
            height = 128.dp,
            modifier = Modifier.offset(x = 104.dp, y = 8.dp).rotate(8f),
        )
        Polaroid(
            image = Res.drawable.ic_onboarding_collage_2,
            width = 142.dp,
            height = 150.dp,
            caption = "Good Vibes ♡",
            modifier = Modifier.rotate(-3f),
        )
        Sparkle(Modifier.offset(x = (-150).dp, y = (-78).dp), 18.dp)
        Sparkle(Modifier.offset(x = 150.dp, y = (-62).dp), 14.dp)
        Sparkle(Modifier.offset(x = 92.dp, y = 90.dp), 12.dp)
    }
}

@Composable
private fun Polaroid(
    image: DrawableResource,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = shape, spotColor = PremiumPrimary)
            .background(Color.White, shape)
            .padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = if (caption == null) 16.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.size(width = width, height = height).clip(RoundedCornerShape(3.dp)),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        if (caption != null) {
            Text(
                text = caption,
                modifier = Modifier.padding(top = 4.dp),
                color = Color(0xFF2A2433),
                fontSize = 16.sp,
                fontFamily = FontFamily.Cursive,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun Sparkle(modifier: Modifier, size: Dp) {
    Icon(
        imageVector = Icons.Filled.AutoAwesome,
        contentDescription = null,
        tint = Color(0xFFFFB020),
        modifier = modifier.size(size),
    )
}

@Composable
private fun PremiumFeatureGrid() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val itemWidth = maxWidth / FeaturesPerRow
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PremiumFeatures.chunked(FeaturesPerRow).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    row.forEach { feature -> FeatureItem(feature, Modifier.width(itemWidth)) }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(feature: PremiumFeature, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PremiumPrimary.copy(alpha = 0.16f))
                .border(1.dp, PremiumPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(feature.icon, contentDescription = null, tint = PremiumPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = feature.label,
            color = PremiumText,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlanCard(plan: PremiumPlan, selected: Boolean, onSelect: () -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    val borderColor by animateColorAsState(if (selected) PremiumPrimary else PremiumText.copy(alpha = 0.14f))
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp)
    val fill = if (selected) {
        Brush.horizontalGradient(listOf(PremiumPrimary.copy(alpha = 0.22f), PremiumPrimary.copy(alpha = 0.06f)))
    } else {
        Brush.horizontalGradient(listOf(surface, surface))
    }

    // Top padding leaves room for the half of the badge that sits above the card's top edge.
    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    when {
                        plan.trial != null -> 88.dp
                        plan.badge != null -> 80.dp
                        else -> 72.dp
                    },
                )
                .clip(PlanShape)
                .background(fill)
                .border(borderWidth, borderColor, PlanShape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlanRadio(selected)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.title, color = PremiumText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                if (plan.trial != null) {
                    TrialChip(plan.trial)
                    Spacer(Modifier.height(3.dp))
                }
                Text(plan.subtitle, color = PremiumSubtext, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .padding(horizontal = 14.dp)
                    .width(1.dp)
                    .fillMaxHeight(0.6f)
                    .background(PremiumDivider),
            )
            // The badge's lower half hangs into the card above the price, so push the price below it.
            Column(modifier = Modifier.width(90.dp).padding(top = if (plan.badge != null) 14.dp else 0.dp)) {
                Text(plan.price, color = PremiumText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(plan.period, color = PremiumSubtext, fontSize = 12.sp)
            }
        }
        if (plan.badge != null) {
            PlanBadge(
                text = plan.badge,
                showStar = plan == PremiumPlan.Weekly,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp)
                    // Centers the badge on the card's top border, whatever its measured height.
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, -placeable.height / 2)
                        }
                    },
            )
        }
    }
}

@Composable
private fun PlanRadio(selected: Boolean) {
    val ringColor by animateColorAsState(if (selected) PremiumPrimary else PremiumText.copy(alpha = 0.45f))
    Box(
        modifier = Modifier.size(22.dp).border(2.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(11.dp).background(PremiumPrimary, CircleShape))
    }
}

@Composable
private fun TrialChip(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PremiumPrimary)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Redeem, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlanBadge(text: String, showStar: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PremiumPrimary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showStar) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PremiumFooter(onStart: () -> Unit, onRestore: () -> Unit, onTermsAndPrivacy: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        StartPremiumButton(onClick = onStart)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = PremiumPrimary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Cancel anytime  •  100% secure payment", color = PremiumSubtext, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(PremiumDivider))
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterLink(Icons.Outlined.Restore, "Restore Purchase", onRestore, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(20.dp).background(PremiumDivider))
            FooterLink(Icons.Outlined.Shield, "Terms & Privacy", onTermsAndPrivacy, Modifier.weight(1f), showChevron = true)
        }
    }
}

@Composable
private fun StartPremiumButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(elevation = 14.dp, shape = StartButtonShape, ambientColor = PremiumPrimary, spotColor = Color(0xFFFFA62B))
            .clip(StartButtonShape)
            .background(StartPremiumGradient)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The gradient ends in pale yellow; a soft shadow keeps the white label readable there.
        Text(
            text = "Start Premium",
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, 2f), blurRadius = 6f)),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(10.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FooterLink(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxHeight().clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = PremiumPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = PremiumText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PremiumSubtext,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PremiumScreenPreview() {
    ThemePreviews {
        PremiumContent(
            snackbarHostState = remember { SnackbarHostState() },
            onClose = {},
            onStart = {},
            onRestore = {},
            onTermsAndPrivacy = {},
        )
    }
}
