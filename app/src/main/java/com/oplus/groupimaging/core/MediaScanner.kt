package com.oplus.groupimaging.core

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ScanCursorPosition(
    val modifiedAt: Long,
    val mediaStoreId: Long,
)

data class MediaCandidate(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val relativePath: String?,
    val absolutePath: String?,
    val createdAt: Long,
    val modifiedAt: Long,
)

class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val logTag = "GroupImagingScan"
    suspend fun scan(
        extraRoots: Set<String>,
        startAfter: ScanCursorPosition? = null,
    ): List<MediaCandidate> = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val roots = effectiveRoots(extraRoots)
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.RELATIVE_PATH)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.DATA)
            }
            add(MediaStore.Images.ImageColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
        }.toTypedArray()
        val selection = buildSelection(roots, startAfter)
        val selectionArgs = buildSelectionArgs(roots, startAfter)
        val out = mutableListOf<MediaCandidate>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs.toTypedArray(),
            "${MediaStore.MediaColumns.DATE_MODIFIED} ASC, ${MediaStore.MediaColumns._ID} ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val relativeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val takenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val addedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val relativePath = cursor.getString(relativeIndex)
                val mimeType = cursor.getString(mimeIndex).orEmpty()
                val id = cursor.getLong(idIndex)
                val modifiedAt = cursor.getLong(modifiedIndex) * 1000
                val createdAt = cursor.getLongOrNull(takenIndex)
                    ?: cursor.getLongOrNull(addedIndex)?.times(1000)
                    ?: modifiedAt
                out += MediaCandidate(
                    id = id,
                    uri = mediaStoreImageUriForId(id),
                    displayName = cursor.getString(nameIndex).orEmpty(),
                    mimeType = mimeType,
                    size = cursor.getLong(sizeIndex),
                    relativePath = relativePath,
                    absolutePath = dataIndex.takeIf { it >= 0 }?.let(cursor::getString),
                    createdAt = createdAt,
                    modifiedAt = modifiedAt,
                )
            }
        }
        Log.i(
            logTag,
            "MediaStore query finished count=${out.size} elapsedMs=${System.currentTimeMillis() - startedAt} startAfter=$startAfter",
        )
        out
    }

    private fun buildSelection(roots: List<String>, startAfter: ScanCursorPosition?): String {
        val rootClauses = roots.joinToString(separator = " OR ", prefix = "(", postfix = ")") {
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        }
        val resumeClause = if (startAfter == null) {
            ""
        } else {
            " AND (${MediaStore.MediaColumns.DATE_MODIFIED} > ? OR (${MediaStore.MediaColumns.DATE_MODIFIED} = ? AND ${MediaStore.MediaColumns._ID} > ?))"
        }
        return "$rootClauses$resumeClause"
    }

    private fun buildSelectionArgs(
        roots: List<String>,
        startAfter: ScanCursorPosition?,
    ): List<String> = buildList {
        roots.forEach { add("$it%") }
        if (startAfter != null) {
            val modifiedAtSeconds = startAfter.modifiedAt / 1000L
            add(modifiedAtSeconds.toString())
            add(modifiedAtSeconds.toString())
            add(startAfter.mediaStoreId.toString())
        }
    }

    companion object {
        val defaultPrefixes: List<String> = listOf(
            "DCIM/Camera",
            "DCIM/OPPO",
            "Pictures/OPPO",
        )

        fun effectiveRoots(extraRoots: Set<String>): List<String> =
            (defaultPrefixes + extraRoots.map(String::trim))
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
    }
}

internal fun mediaStoreImageUriForId(id: Long): String =
    "content://media/external/images/media/$id"

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (index < 0 || isNull(index)) null else getLong(index).takeIf { it > 0L }
