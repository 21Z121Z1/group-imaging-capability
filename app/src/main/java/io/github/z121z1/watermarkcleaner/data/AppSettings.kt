package io.github.z121z1.watermarkcleaner.data

import android.content.Context
import android.net.Uri

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("watermark_cleaner", Context.MODE_PRIVATE)

    var outputTree: Uri?
        get() = prefs.getString(KEY_OUTPUT_TREE, null)?.let(Uri::parse)
        set(value) { prefs.edit().putString(KEY_OUTPUT_TREE, value?.toString()).apply() }

    var jpegQuality: Int
        get() = prefs.getInt(KEY_JPEG_QUALITY, 90).coerceIn(70, 100)
        set(value) { prefs.edit().putInt(KEY_JPEG_QUALITY, value.coerceIn(70, 100)).apply() }

    var cleanHdrGainMap: Boolean
        get() = prefs.getBoolean(KEY_CLEAN_GAINMAP, true)
        set(value) { prefs.edit().putBoolean(KEY_CLEAN_GAINMAP, value).apply() }

    companion object {
        private const val KEY_OUTPUT_TREE = "output_tree"
        private const val KEY_JPEG_QUALITY = "jpeg_quality"
        private const val KEY_CLEAN_GAINMAP = "clean_gainmap"
    }
}
