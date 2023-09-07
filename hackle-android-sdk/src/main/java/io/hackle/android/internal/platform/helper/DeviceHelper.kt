package io.hackle.android.internal.platform.helper

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import java.util.Locale

internal object DeviceHelper {

    private const val WINDOW_SIZE_MEDIUM = 600

    fun getDeviceType(context: Context): String {
        val service = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return when (service.currentModeType) {
            Configuration.UI_MODE_TYPE_NORMAL ->
                // Support different screen sizes
                // https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes
                if (context.resources.configuration.smallestScreenWidthDp < WINDOW_SIZE_MEDIUM)
                    "phone" else "tablet"
            Configuration.UI_MODE_TYPE_TELEVISION -> "tv"
            Configuration.UI_MODE_TYPE_DESK -> "pc"
            Configuration.UI_MODE_TYPE_CAR -> "car"
            Configuration.UI_MODE_TYPE_APPLIANCE -> "appliance"
            Configuration.UI_MODE_TYPE_WATCH -> "watch"
            Configuration.UI_MODE_TYPE_VR_HEADSET -> "vr"
            else -> "undefined"
        }
    }

    @Suppress("DEPRECATION")
    fun getDeviceLocale(): Locale {
        val configuration = Resources.getSystem().configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return if (configuration.locales.isEmpty)
                Locale.getDefault() else configuration.locales[0]
        } else {
            return configuration.locale
        }
    }

    @Suppress("DEPRECATION")
    fun getDisplayMetrics(context: Context): DisplayMetrics {
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            display.getRealMetrics(metrics)
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            display.getMetrics(metrics)
        }
        return metrics
    }
}