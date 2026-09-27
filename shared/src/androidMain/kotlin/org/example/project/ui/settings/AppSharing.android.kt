package org.example.project.ui.settings

import android.content.Intent
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context

internal actual fun shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    FileKit.context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
