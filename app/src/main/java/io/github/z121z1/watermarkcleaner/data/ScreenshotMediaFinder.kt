package io.github.z121z1.watermarkcleaner.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat

enum class PhotoLibraryAccess { FULL, PARTIAL, NONE }

class ScreenshotMediaFinder(private val context: Context) {
    private val resolver = context.contentResolver

    fun access(): PhotoLibraryAccess = when {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ->
            PhotoLibraryAccess.FULL
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED ->
            PhotoLibraryAccess.PARTIAL
        else -> PhotoLibraryAccess.NONE
    }

    /**
     * Returns a recent screenshot only when full library access was explicitly granted. Partial
     * selected-photo access must never be treated as permission to discover a newly created screenshot.
     */
    fun findAfter(captureStartedAtMillis: Long): Uri? {
        if (access() != PhotoLibraryAccess.FULL) return null
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
        )
        val lowerSeconds = captureStartedAtMillis / 1000L - 3L
        val selection = "${MediaStore.Images.Media.DATE_ADDED}>=?"
        val args = arrayOf(lowerSeconds.toString())
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            var inspected = 0
            while (cursor.moveToNext() && inspected++ < 32) {
                val name = cursor.getString(nameCol).orEmpty()
                val path = cursor.getString(pathCol).orEmpty()
                val taken = cursor.getLong(takenCol)
                val looksLikeScreenshot =
                    name.contains("Screenshot", ignoreCase = true) ||
                        name.contains("截屏", ignoreCase = true) ||
                        name.contains("截图", ignoreCase = true) ||
                        path.contains("Screenshot", ignoreCase = true) ||
                        path.contains("Screenshots", ignoreCase = true)
                if (!looksLikeScreenshot) continue
                if (taken > 0 && taken + 3_000L < captureStartedAtMillis) continue
                return ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idCol),
                )
            }
        }
        return null
    }
}
