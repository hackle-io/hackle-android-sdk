package io.hackle.android.ui.core

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import io.hackle.android.ui.inappmessage.AndroidColor

internal object Drawables {

    fun transparent(): ColorDrawable {
        return ColorDrawable(Color.TRANSPARENT)
    }

    fun of(
        shape: Int = GradientDrawable.RECTANGLE,
        radii: CornerRadii = CornerRadii.ZERO,
        color: AndroidColor
    ): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = shape
        drawable.cornerRadii = radii.toFloatArray()
        drawable.setColor(color)
        return drawable
    }
}
