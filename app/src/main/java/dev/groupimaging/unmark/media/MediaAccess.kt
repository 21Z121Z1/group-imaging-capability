package dev.groupimaging.unmark.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

/** Android 14+ selected-photos access is deliberately not treated as full library access. */
enum class MediaAccess {
    Full,
    Partial,
    PickerOnly,
    ;

    companion object {
        fun current(context: Context): MediaAccess = when {
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> Full
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED -> Partial
            else -> PickerOnly
        }
    }
}
