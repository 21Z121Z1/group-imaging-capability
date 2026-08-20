package dev.groupimaging.unmark.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

class ScreenshotFinder(private val context: Context) {
    fun findRecent(afterMillis: Long): Uri? {
        require(MediaAccess.current(context) == MediaAccess.Full) {
            "Automatic screenshot import requires full photo access"
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
        )
        val afterSeconds = (afterMillis / 1000L - 3L).coerceAtLeast(0L)
        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
        val args = arrayOf(afterSeconds.toString())

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            var inspected = 0
            while (cursor.moveToNext() && inspected++ < 32) {
                val name = cursor.getString(nameColumn).orEmpty()
                val path = cursor.getString(pathColumn).orEmpty()
                if (!looksLikeScreenshot(name, path)) continue

                val taken = cursor.getLong(takenColumn)
                val added = cursor.getLong(addedColumn) * 1000L
                val timestamp = if (taken > 0L) taken else added
                if (timestamp + 2_000L < afterMillis) continue

                return ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idColumn),
                )
            }
        }
        return null
    }

    internal fun looksLikeScreenshot(name: String, path: String): Boolean {
        val joined = "$path/$name".lowercase()
        return joined.contains("screenshot") ||
            joined.contains("screenshots") ||
            joined.contains("截屏") ||
            joined.contains("截图")
    }
}
