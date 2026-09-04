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
     * Returns the newest completed screenshot created after [captureStartedAtMillis]. Existing
     * calibration samples are explicitly excluded so a delayed ColorOS MediaStore insertion cannot
     * make us consume the previous level again while the new screenshot is still being encoded.
     */
    fun findAfter(captureStartedAtMillis: Long, excluded: Set<Uri> = emptySet()): Uri? {
        if (access() != PhotoLibraryAccess.FULL) return null
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.IS_PENDING,
        )
        val lowerSeconds = captureStartedAtMillis / 1000L - 1L
        val selection = "${MediaStore.Images.Media.DATE_ADDED}>=?"
        val args = arrayOf(lowerSeconds.toString())
        val candidates = ArrayList<Candidate>()
        val volumes = MediaStore.getExternalVolumeNames(context).ifEmpty {
            setOf(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        for (volume in volumes) {
            val collection = MediaStore.Images.Media.getContentUri(volume)
            resolver.query(
                collection,
                projection,
                selection,
                args,
                "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val pendingCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_PENDING)
                var inspected = 0
                while (cursor.moveToNext() && inspected++ < 64) {
                    if (cursor.getInt(pendingCol) != 0) continue
                    val name = cursor.getString(nameCol).orEmpty()
                    val path = cursor.getString(pathCol).orEmpty()
                    val looksLikeScreenshot =
                        name.contains("Screenshot", ignoreCase = true) ||
                            name.contains("截屏", ignoreCase = true) ||
                            name.contains("截图", ignoreCase = true) ||
                            path.contains("Screenshot", ignoreCase = true) ||
                            path.contains("Screenshots", ignoreCase = true)
                    if (!looksLikeScreenshot) continue

                    val taken = cursor.getLong(takenCol)
                    if (taken > 0 && taken + 2_000L < captureStartedAtMillis) continue
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    if (uri in excluded) continue
                    candidates += Candidate(
                        uri = uri,
                        dateAdded = cursor.getLong(addedCol),
                        id = id,
                    )
                }
            }
        }

        return candidates.maxWithOrNull(compareBy<Candidate>({ it.dateAdded }, { it.id }))?.uri
    }

    private data class Candidate(
        val uri: Uri,
        val dateAdded: Long,
        val id: Long,
    )
}
