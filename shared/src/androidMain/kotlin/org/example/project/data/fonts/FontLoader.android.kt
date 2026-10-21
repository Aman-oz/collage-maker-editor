package org.example.project.data.fonts

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File

internal actual suspend fun fontFamilyFromFile(path: String): FontFamily = FontFamily(Font(File(path)))
