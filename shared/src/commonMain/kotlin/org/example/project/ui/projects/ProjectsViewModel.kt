package org.example.project.ui.projects

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.ProjectsRepository

/** Backs Home's Projects tab: the saved projects and their lazily decoded thumbnails. */
class ProjectsViewModel(private val repository: ProjectsRepository) : ViewModel() {

    val projects: StateFlow<List<String>?> = repository.projects
    val thumbnails: StateFlow<Map<String, ImageBitmap>> = repository.thumbnails

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun loadThumbnail(path: String) {
        viewModelScope.launch { repository.loadThumbnail(path) }
    }
}
