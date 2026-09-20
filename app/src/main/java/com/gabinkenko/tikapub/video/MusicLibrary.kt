package com.gabinkenko.tikapub.video

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg")

/**
 * Picks a random royalty-free background track from the folder the user granted access to via
 * Storage Access Framework (see Settings screen). Returns null if no folder was configured or it's
 * empty, in which case videos are generated without background music.
 */
object MusicLibrary {

    fun pickRandomTrack(context: Context, musicFolderUri: String?): Uri? {
        if (musicFolderUri.isNullOrBlank()) return null
        val treeUri = Uri.parse(musicFolderUri)
        val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val tracks = folder.listFiles().filter { doc ->
            doc.isFile && (doc.name?.substringAfterLast('.', "")?.lowercase() in AUDIO_EXTENSIONS)
        }
        if (tracks.isEmpty()) return null
        return tracks.random().uri
    }
}
