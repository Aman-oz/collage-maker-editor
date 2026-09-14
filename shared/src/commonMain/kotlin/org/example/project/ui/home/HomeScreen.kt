package org.example.project.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.navigation.GalleryTarget
import org.example.project.ui.preview.ThemePreviews
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onOpenGallery: (maxSelection: Int, target: GalleryTarget) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    HomeContent(
        platformName = viewModel.platformName,
        onPickImage = { onOpenGallery(1, GalleryTarget.Editor) },
        onPickCollageImages = { onOpenGallery(8, GalleryTarget.Collage) },
        onPickFreestyleImages = { onOpenGallery(12, GalleryTarget.Freestyle) },
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    platformName: String,
    onPickImage: () -> Unit,
    onPickCollageImages: () -> Unit,
    onPickFreestyleImages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Photo Collage Maker",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Running on $platformName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
        )
        Button(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                text = "Photo Editor",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Button(
            onClick = onPickCollageImages,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
            ),
        ) {
            Text(
                text = "Collage",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Button(
            onClick = onPickFreestyleImages,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
            ),
        ) {
            Text(
                text = "Freestyle",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    ThemePreviews {
        HomeContent(
            platformName = "Android",
            onPickImage = {},
            onPickCollageImages = {},
            onPickFreestyleImages = {},
        )
    }
}
