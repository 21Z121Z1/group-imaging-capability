package io.github.z121z1.watermarkcleaner.platform

import android.content.Context
import android.provider.Settings

data class ColorOsPredictiveBackState(
    val predictiveContinuousProperty: Boolean?,
    val thirdPartyPredictiveBackSetting: Int?,
) {
    val systemContinuousEnabled: Boolean?
        get() = predictiveContinuousProperty
}

/**
 * Best-effort diagnostics for the ColorOS 17 gates recovered from SystemUI,
 * Launcher and Settings. These values are diagnostic only: the app must not
 * try to take ownership of ShellContinuousTransitionController or any
 * SurfaceControl leash used by the system predictive-back pipeline.
 */
object ColorOsPredictiveBackDiagnostics {
    private const val PROP_PREDICTIVE_CONTINUOUS = "persist.wm.enable.predictive.continuous"
    private const val GLOBAL_THIRD_PARTY_PREDICTIVE_BACK = "oplus_third_part_apps_predictive_back"

    fun inspect(context: Context): ColorOsPredictiveBackState = ColorOsPredictiveBackState(
        predictiveContinuousProperty = readSystemBoolean(PROP_PREDICTIVE_CONTINUOUS),
        thirdPartyPredictiveBackSetting = runCatching {
            Settings.Global.getInt(context.contentResolver, GLOBAL_THIRD_PARTY_PREDICTIVE_BACK)
        }.getOrNull(),
    )

    private fun readSystemBoolean(name: String): Boolean? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val getBoolean = clazz.getDeclaredMethod(
            "getBoolean",
            String::class.java,
            Boolean::class.javaPrimitiveType!!,
        ).apply { isAccessible = true }
        getBoolean.invoke(null, name, false) as Boolean
    }.getOrNull()
}
