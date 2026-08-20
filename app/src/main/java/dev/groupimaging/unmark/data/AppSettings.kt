package dev.groupimaging.unmark.data

import android.content.Context
import android.net.Uri

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("unmark-settings", Context.MODE_PRIVATE)

    var jpegQuality: Int
        get() = prefs.getInt(KEY_JPEG_QUALITY, 90).coerceIn(70, 100)
        set(value) {
            prefs.edit().putInt(KEY_JPEG_QUALITY, value.coerceIn(70, 100)).apply()
        }

    var outputTreeUri: Uri?
        get() = prefs.getString(KEY_OUTPUT_TREE, null)?.let(Uri::parse)
        set(value) {
            prefs.edit().putString(KEY_OUTPUT_TREE, value?.toString()).apply()
        }

    companion object {
        private const val KEY_JPEG_QUALITY = "jpeg-quality"
        private const val KEY_OUTPUT_TREE = "output-tree"
    }
}
