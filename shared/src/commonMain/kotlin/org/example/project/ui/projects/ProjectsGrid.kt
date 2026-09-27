package org.example.project.ui.projects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.common.navSharedElement
import org.example.project.ui.common.projectImageKey
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.img_no_projects

private val ItemShape = RoundedCornerShape(8.dp)

/**
 * Home's Projects tab: saved creations in a 3-column grid matching the photo gallery. [projects]
 * is `null` while the first listing is still loading, which shows nothing rather than flashing the
 * empty state.
 */
@Composable
internal fun ProjectsTabContent(
    projects: List<String>?,
    thumbnails: Map<String, ImageBitmap>,
    onRequestThumbnail: (path: String) -> Unit,
    onOpenProject: (path: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        projects == null -> Box(modifier)
        projects.isEmpty() -> ProjectsEmptyState(modifier)
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            // Leave room for the bottom bar that floats over this content.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(projects, key = { it }) { path ->
                ProjectGridItem(
                    thumbnail = thumbnails[path],
                    sharedKey = projectImageKey(path),
                    onRequestThumbnail = { onRequestThumbnail(path) },
                    onClick = { onOpenProject(path) },
                )
            }
        }
    }
}

@Composable
private fun ProjectGridItem(
    thumbnail: ImageBitmap?,
    sharedKey: String,
    onRequestThumbnail: () -> Unit,
    onClick: () -> Unit,
) {
    if (thumbnail == null) LaunchedEffect(sharedKey) { onRequestThumbnail() }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            // Before the clip, so the rounded corners travel with the image during the transition.
            .navSharedElement(sharedKey)
            .clip(ItemShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Image, onClickLabel = "Open project", onClick = onClick),
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = "Project",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ProjectsEmptyState(modifier: Modifier = Modifier) {
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

@Preview
@Composable
private fun ProjectsGridPreview() {
    ThemePreviews {
        ProjectsTabContent(
            projects = List(7) { "project_$it.jpg" },
            thumbnails = emptyMap(),
            onRequestThumbnail = {},
            onOpenProject = {},
        )
    }
}
