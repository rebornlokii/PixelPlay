package com.theveloper.pixelplay.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.theveloper.pixelplay.data.model.Song
import java.io.File
import java.io.FileOutputStream

/**
 * Writes a generated share card to disk and either hands it to the system share sheet or
 * saves it to the device gallery.
 */
object ShareCardSharing {

    private const val CACHE_DIR = "share_cards"
    private const val GALLERY_SUBDIR = "PixelPlayer"

    fun fileNameFor(song: Song, aspect: ShareCardAspect): String {
        val safeTitle = song.title
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .take(40)
            .ifBlank { "song" }
        val suffix = if (aspect == ShareCardAspect.STORY) "story" else "square"
        return "PixelPlayer_${safeTitle}_$suffix.png"
    }

    fun share(context: Context, bitmap: Bitmap, fileName: String, chooserTitle: String): Result<Unit> =
        runCatching {
            val file = writeToCache(context, bitmap, fileName)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }

    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Result<Uri> = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$GALLERY_SUBDIR")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create a gallery entry")
        resolver.openOutputStream(uri)?.use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                error("Unable to encode the share card")
            }
        } ?: error("Unable to open the gallery output stream")

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri
    }

    private fun writeToCache(context: Context, bitmap: Bitmap, fileName: String): File {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return file
    }
}
