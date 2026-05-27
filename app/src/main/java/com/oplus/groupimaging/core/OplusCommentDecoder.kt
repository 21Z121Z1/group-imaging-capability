package com.oplus.groupimaging.core

private const val BIT_PROFESSIONAL = 0x100L
private const val BIT_PANORAMA = 0x4L
private const val BIT_STICKER = 0x200L
private const val BIT_NIGHT = 0x800L
private const val BIT_ID_PHOTO = 0x4000L
private const val FLAG_STORAGE_LIVE = 0x800000L
private const val FLAG_EXIF_HASSEL = 0x100000L
private const val FLAG_EXIF_ULTRA_HDR = 0x200000L
private const val FLAG_EXIF_OPLUS_UHDR = 0x20000000L

data class DecodedOplusComment(
    val rawValue: Long,
    val captureModeLabel: String?,
    val livePhotoFlag: Boolean,
    val namedExifFlags: Set<String>,
)

object OplusCommentDecoder {
    fun decode(raw: String?): DecodedOplusComment? {
        if (raw.isNullOrBlank()) {
            return null
        }
        val normalized = raw.trim().removePrefix("oplus_").removePrefix("Oplus_")
        val value = normalized.toLongOrNull() ?: return null
        return DecodedOplusComment(
            rawValue = value,
            captureModeLabel = captureModeLabel(value),
            livePhotoFlag = value and FLAG_STORAGE_LIVE != 0L,
            namedExifFlags = buildSet {
                if (value and FLAG_EXIF_HASSEL != 0L) add("EXIF_TAG_SUPPORT_HASSEL_WATER_MARK")
                if (value and FLAG_EXIF_ULTRA_HDR != 0L) add("EXIF_TAG_ULTRA_HDR")
                if (value and FLAG_EXIF_OPLUS_UHDR != 0L) add("EXIF_TAG_OPLUS_UHDR")
            },
        )
    }

    private fun captureModeLabel(value: Long): String? = when {
        value and BIT_PROFESSIONAL != 0L -> "大师"
        value and BIT_PANORAMA != 0L -> "全景"
        value and BIT_STICKER != 0L -> "贴纸"
        value and BIT_NIGHT != 0L -> "夜景"
        value and BIT_ID_PHOTO != 0L -> "证件照"
        else -> null
    }
}
