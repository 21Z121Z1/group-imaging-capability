package com.oplus.groupimaging.data

import com.oplus.groupimaging.core.MediaScanner

private const val EXTERNAL_STORAGE_PREFIX = "/storage/emulated/0/"
private val normalizedDefaultScanDirectories = MediaScanner.defaultPrefixes
    .map { it.trim('/').lowercase() }
    .toSet()

internal fun normalizeExtraScanDirectory(raw: String): String {
    val trimmed = raw.trim()
    require(trimmed.isNotEmpty()) { "目录不能为空" }
    val relative = when {
        trimmed.startsWith(EXTERNAL_STORAGE_PREFIX, ignoreCase = true) ->
            trimmed.substring(EXTERNAL_STORAGE_PREFIX.length)
        trimmed.startsWith("storage/emulated/0/", ignoreCase = true) ->
            trimmed.substring("storage/emulated/0/".length)
        else -> trimmed.removePrefix("/")
    }.trim()
    require(relative.isNotEmpty()) { "目录不能为空" }
    val normalized = relative.trim('/').replace(Regex("/+"), "/") + "/"
    require(normalized.trim('/').lowercase() !in normalizedDefaultScanDirectories) {
        "默认目录已内置，无需重复添加"
    }
    return normalized
}

internal fun normalizeExtraScanDirectories(rawDirectories: List<String>): List<String> {
    val normalizedDirectories = mutableListOf<String>()
    val seen = mutableSetOf<String>()
    rawDirectories.forEach { directory ->
        val normalized = normalizeExtraScanDirectory(directory)
        require(seen.add(normalized.lowercase())) { "目录重复：$normalized" }
        normalizedDirectories += normalized
    }
    return normalizedDirectories.sortedBy { it.lowercase() }
}
