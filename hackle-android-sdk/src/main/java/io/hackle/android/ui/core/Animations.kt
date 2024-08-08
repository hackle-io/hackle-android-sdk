package io.hackle.android.ui.core

import android.view.animation.*

internal object Animations {

    private val ACCELERATE_INTERPOLATOR = AccelerateInterpolator()
    private val DECELERATE_INTERPOLATOR = DecelerateInterpolator()

    fun slideInBottom(durationMillis: Long, delayMillis: Long = 0): Animation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_SELF, 1f,
            Animation.RELATIVE_TO_SELF, 0f
        )
        animation.interpolator = DECELERATE_INTERPOLATOR
        animation.duration = durationMillis
        animation.startOffset = delayMillis
        return animation
    }

    fun slideOutBottom(durationMillis: Long, delayMillis: Long = 0): Animation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 1f
        )
        animation.interpolator = DECELERATE_INTERPOLATOR
        animation.duration = durationMillis
        animation.startOffset = delayMillis
        return animation
    }

    fun fadeIn(durationMillis: Long, delayMillis: Long = 0): Animation {
        val animation = AlphaAnimation(0f, 1f)
        animation.interpolator = ACCELERATE_INTERPOLATOR
        animation.duration = durationMillis
        animation.startOffset = delayMillis
        return animation
    }

    fun fadeOut(durationMillis: Long, delayMillis: Long = 0): Animation {
        val animation = AlphaAnimation(1f, 0f)
        animation.interpolator = ACCELERATE_INTERPOLATOR
        animation.duration = durationMillis
        animation.startOffset = delayMillis
        return animation
    }
}
