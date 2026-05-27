package com.oplus.groupimaging.core

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class ExtractedMetadata(
    val make: String?,
    val model: String?,
    val focalLengthMm: Double?,
    val focalLengthEq: Int?,
    val userComment: String?,
    val hasRequiredExif: Boolean,
    val capturedAt: Long,
)

class MetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val logTag = "GroupImagingScan"
    private val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    suspend fun extract(candidate: MediaCandidate): ExtractedMetadata = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val uri = Uri.parse(candidate.uri)
        val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        val make = exif?.getAttribute(ExifInterface.TAG_MAKE)?.trim().takeUnless { it.isNullOrEmpty() }
        val model = exif?.getAttribute(ExifInterface.TAG_MODEL)?.trim().takeUnless { it.isNullOrEmpty() }
        val focalLengthMm = exif?.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)?.takeIf { it > 0.0 }
        val focalLengthEq = exif?.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)?.takeIf { it > 0 }
        val userComment = exif?.getAttribute(ExifInterface.TAG_USER_COMMENT)?.trim().takeUnless { it.isNullOrEmpty() }
        val capturedAt = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() }
            ?: candidate.createdAt
        val hasRequiredExif = make != null && model != null && focalLengthEq != null && userComment != null
        val elapsedMs = System.currentTimeMillis() - startedAt
        if (elapsedMs >= 250L) {
            Log.d(logTag, "Slow metadata extract elapsedMs=$elapsedMs file=${candidate.displayName}")
        }
        ExtractedMetadata(
            make = make,
            model = model,
            focalLengthMm = focalLengthMm,
            focalLengthEq = focalLengthEq,
            userComment = userComment,
            hasRequiredExif = hasRequiredExif,
            capturedAt = capturedAt,
        )
    }
}
