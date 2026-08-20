package dev.groupimaging.unmark.image

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import dev.groupimaging.unmark.data.AppSettings
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OutputWriter(
    private val context: Context,
    private val settings: AppSettings,
) {
    data class Result(
        val uri: Uri,
        val ultraHdrVerified: Boolean?,
    )

    fun write(bitmap: Bitmap, inputWasUltraHdr: Boolean): Result {
        val name = "Unmark_${LocalDateTime.now().format(NAME_FORMAT)}.jpg"
        val uri = settings.outputTreeUri?.let { writeToTree(it, name, bitmap) }
            ?: writeToMediaStore(name, bitmap)

        val verified = if (inputWasUltraHdr) verifyUltraHdr(uri) else null
        return Result(uri, verified)
    }

    private fun writeToMediaStore(name: String, bitmap: Bitmap): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/去印")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("无法创建媒体输出")
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, output)) {
                    "JPEG 编码失败"
                }
            } ?: throw IOException("无法打开媒体输出")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )
            return uri
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
    }

    private fun writeToTree(treeUri: Uri, name: String, bitmap: Bitmap): Uri {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val uri = DocumentsContract.createDocument(resolver, parent, "image/jpeg", name)
            ?: throw IOException("无法在所选目录创建文件")
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, output)) {
                    "JPEG 编码失败"
                }
            } ?: throw IOException("无法打开所选目录中的输出文件")
            return uri
        } catch (t: Throwable) {
            runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            throw t
        }
    }

    private fun verifyUltraHdr(uri: Uri): Boolean = runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > 1024) decoder.setTargetSampleSize((longest / 1024).coerceAtLeast(1))
        }
        try {
            bitmap.gainmap != null
        } finally {
            bitmap.recycle()
        }
    }.getOrDefault(false)

    companion object {
        private val NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
    }
}
