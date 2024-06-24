package io.hackle.android.ui.inappmessage

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import io.hackle.sdk.core.model.InAppMessage

internal typealias AndroidOrientation = Int

internal val Activity.orientation: InAppMessage.Orientation?
    get() {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> InAppMessage.Orientation.VERTICAL
            Configuration.ORIENTATION_LANDSCAPE -> InAppMessage.Orientation.HORIZONTAL
            else -> null
        }
    }

internal fun Activity.px(dp: Int): Float {
    return dp * resources.displayMetrics.density
}

internal fun View.px(dp: Int): Float {
    return dp * resources.displayMetrics.density
}

internal fun InAppMessage.supports(orientation: InAppMessage.Orientation): Boolean {
    return orientation in messageContext.orientations
}

internal fun InAppMessage.supports(orientation: AndroidOrientation): Boolean {
    return messageContext.orientations.any { it.supports(orientation) }
}

internal fun InAppMessage.Orientation.supports(orientation: AndroidOrientation): Boolean {
    return when (this) {
        InAppMessage.Orientation.VERTICAL -> orientation == Configuration.ORIENTATION_PORTRAIT
        InAppMessage.Orientation.HORIZONTAL -> orientation == Configuration.ORIENTATION_LANDSCAPE
    }
}

internal fun InAppMessage.Message.outerButtonOrNull(
    horizontal: InAppMessage.Message.Alignment.Horizontal,
    vertical: InAppMessage.Message.Alignment.Vertical
): InAppMessage.Message.PositionalButton? {
    return outerButtons.find { it.alignment.horizontal == horizontal && it.alignment.vertical == vertical }
}
