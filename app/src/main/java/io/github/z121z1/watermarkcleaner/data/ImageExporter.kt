package io.github.z121z1.watermarkcleaner.data

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ImageExporter(
    private val resolver: ContentResolver,
    private val settings: AppSettings,
) {
    suspend fun export(bitmap: Bitmap, source: Uri?): Uri = withContext(Dispatchers.IO) {
        val displayName = buildName(source)
        val tree = settings.outputTree
        if (tree != null) exportToTree(tree, displayName, bitmap) else exportToMediaStore(displayName, bitmap)
    }

    private fun exportToMediaStore(name: String, bitmap: Bitmap): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/截图去水印")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建媒体库输出")
        try {
            resolver.openOutputStream(uri, "w")?.use { out ->
                // Deliberate post-process, not merely an output-format choice: the JPEG quantizer
                // suppresses sub-code-value residuals left by the inverse watermark composite.
                // When bitmap carries a Gainmap, Android's Bitmap JPEG encoder emits Ultra HDR
                // JPEG_R and encodes both the SDR base and attached gain map at this quality.
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, out)) {
                    "JPEG / Ultra HDR JPEG_R 编码失败"
                }
            } ?: error("无法打开媒体库输出")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            return uri
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
    }

    private fun exportToTree(tree: Uri, name: String, bitmap: Bitmap): Uri {
        val documentId = DocumentsContract.getTreeDocumentId(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
        val uri = DocumentsContract.createDocument(resolver, parent, "image/jpeg", name)
            ?: error("无法在自定义目录创建文件")
        try {
            resolver.openOutputStream(uri, "w")?.use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, out)) {
                    "JPEG / Ultra HDR JPEG_R 编码失败"
                }
            } ?: error("无法写入自定义目录")
            return uri
        } catch (t: Throwable) {
            runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            throw t
        }
    }

    private fun buildName(source: Uri?): String {
        val sourceName = source?.let(::queryName)
        val stem = sourceName
            ?.substringBeforeLast('.', sourceName)
            ?.takeIf { it.isNotBlank() }
            ?: "Screenshot_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))}"
        return "${stem}_clean.jpg"
    }

    private fun queryName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}
