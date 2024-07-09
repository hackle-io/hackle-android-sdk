package io.hackle.android.ui.inappmessage

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.sdk.core.model.InAppMessage

internal typealias AndroidOrientation = Int
internal typealias AndroidColor = Int

internal val Activity.orientation: InAppMessage.Orientation?
    get() {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> InAppMessage.Orientation.VERTICAL
            Configuration.ORIENTATION_LANDSCAPE -> InAppMessage.Orientation.HORIZONTAL
            else -> null
        }
    }
internal val View.orientation: InAppMessage.Orientation?
    get() {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> InAppMessage.Orientation.VERTICAL
            Configuration.ORIENTATION_LANDSCAPE -> InAppMessage.Orientation.HORIZONTAL
            else -> null
        }
    }

internal val View.requiredOrientation: InAppMessage.Orientation
    get() {
        return requireNotNull(orientation) { "Failed to get orientation [${javaClass.name}]" }
    }

internal fun InAppMessage.supports(orientation: InAppMessage.Orientation): Boolean {
    return orientation in messageContext.orientations
}

internal fun InAppMessage.Orientation.supports(orientation: AndroidOrientation): Boolean {
    return when (this) {
        InAppMessage.Orientation.VERTICAL -> orientation == Configuration.ORIENTATION_PORTRAIT
        InAppMessage.Orientation.HORIZONTAL -> orientation == Configuration.ORIENTATION_LANDSCAPE
    }
}

internal fun InAppMessage.Message.outerButtonOrNull(alignment: InAppMessage.Message.Alignment): InAppMessage.Message.PositionalButton? {
    return outerButtons.find { it.alignment == alignment }
}

internal val InAppMessage.Message.backgroundColor: AndroidColor get() = Color.parseColor(background.color)

internal val InAppMessage.Message.Text.Attribute.color: AndroidColor get() = Color.parseColor(style.textColor)

internal val InAppMessage.Message.Button.textColor: AndroidColor get() = Color.parseColor(style.textColor)
internal val InAppMessage.Message.Button.backgroundColor: AndroidColor get() = Color.parseColor(style.bgColor)
internal val InAppMessage.Message.Button.borderColor: AndroidColor get() = Color.parseColor(style.borderColor)


internal fun InAppMessagePresentationContext.imageOrNull(orientation: InAppMessage.Orientation): InAppMessage.Message.Image? {
    return message.images.firstOrNull { it.orientation == orientation }
}

internal fun InAppMessagePresentationContext.image(orientation: InAppMessage.Orientation): InAppMessage.Message.Image {
    return requireNotNull(imageOrNull(orientation)) { "Not found in-app message image [${inAppMessage.id}, $orientation]" }
}