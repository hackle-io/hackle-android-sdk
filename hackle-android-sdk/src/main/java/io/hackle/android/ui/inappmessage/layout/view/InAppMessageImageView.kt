package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import io.hackle.android.ui.core.CornerRadii
import io.hackle.android.ui.inappmessage.layout.activity.InAppMessageActivity
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.InAppMessage
import kotlin.math.min

internal class InAppMessageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var path = Path()
    private var rect = RectF()
    private var cornerRadii: CornerRadii = CornerRadii.ZERO
    private var aspectRatio: AspectRatio? = null

    fun setCornerRadius(px: Float) {
        setCornerRadii(CornerRadii.of(px))
    }

    fun setCornerRadii(cornerRadii: CornerRadii) {
        this.cornerRadii = cornerRadii
    }

    fun setAspectRatio(aspectRatio: AspectRatio?) {
        this.aspectRatio = aspectRatio
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val aspectRatio = aspectRatio
        if (aspectRatio != null && measuredWidth > 0) {
            val newWidth = measuredWidth
            val maxHeight = aspectRatio.calculateHeight(measuredWidth)
            val newHeight = if (measuredHeight > 0) min(measuredHeight, maxHeight) else maxHeight
            setMeasuredDimension(newWidth, newHeight + 1)
        } else {
            setMeasuredDimension(measuredWidth, measuredHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        clip(canvas)
        super.onDraw(canvas)
    }

    private fun clip(canvas: Canvas) {
        try {
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            path.reset()
            path.addRoundRect(rect, cornerRadii.toFloatArray(), Path.Direction.CW)
            canvas.clipPath(path)
        } catch (e: Throwable) {
            log.error { "Failed to clip in-app message image: $e" }
        }
    }

    fun configure(inAppMessageView: InAppMessageView, image: InAppMessage.Message.Image, scaleType: ScaleType? = null) {
        if (scaleType != null) {
            this.scaleType = scaleType
        }
        render(image, inAppMessageView)
        setOnClickListener(inAppMessageView.createImageClickListener(image))
    }

    data class AspectRatio(val width: Float, val height: Float) {
        val value: Float get() = width / height

        fun calculateHeight(width: Int): Int {
            return (width / value).toInt()
        }
    }

    companion object {
        private val log = Logger<InAppMessageImageView>()
    }
}

internal fun InAppMessageImageView.render(image: InAppMessage.Message.Image, view: InAppMessageView) {
    view.controller.ui.imageLoader.renderTo(view.getContext(), image.imagePath, this)
}

internal fun InAppMessageImageView.render(image: InAppMessage.Message.Image, activity: InAppMessageActivity) {
    activity.controller.ui.imageLoader.renderTo(activity, image.imagePath, this)
}
