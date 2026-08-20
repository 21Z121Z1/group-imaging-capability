package io.github.z121z1.watermarkcleaner.platform

import android.content.Context
import android.os.Build
import android.view.WindowManager

data class PlatformUiCapabilities(
    val colorOsFamily: Boolean,
    val crossWindowBlur: Boolean,
    val freeformFriendly: Boolean = true,
)

object ColorOsCompat {
    /**
     * Deliberately uses only Android public API. ColorOS/OPlus private UX material APIs are
     * signature/system surfaces and are not reflected into from a third-party app.
     */
    fun detect(context: Context): PlatformUiCapabilities {
        val brand = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        val colorOs = listOf("oppo", "oneplus", "realme").any(brand::contains)
        val blur = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
        } else false
        return PlatformUiCapabilities(colorOsFamily = colorOs, crossWindowBlur = blur)
    }
}
