package io.hackle.android.ui.inappmessage

import android.app.Activity
import android.content.res.Configuration
import io.hackle.sdk.core.model.InAppMessage

internal val Activity.orientation: InAppMessage.Orientation?
    get() {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> InAppMessage.Orientation.VERTICAL
            Configuration.ORIENTATION_LANDSCAPE -> InAppMessage.Orientation.HORIZONTAL
            else -> null
        }
    }

internal fun InAppMessage.supports(orientation: InAppMessage.Orientation): Boolean {
    return orientation in messageContext.orientations
}
