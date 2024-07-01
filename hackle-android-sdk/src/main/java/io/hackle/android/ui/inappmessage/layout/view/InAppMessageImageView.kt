package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import io.hackle.android.ui.inappmessage.layout.activity.InAppMessageActivity
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.InAppMessage
import kotlin.math.min

internal class InAppMessageImageView : ImageView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var path = Path()
    private var rect = RectF()
    private var cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private var aspectRatio = -1f
    private var heightRatio = 1.0

    fun setCornersRadius(px: Float) {
        setCornersRadii(px, px, px, px)
    }

    fun setCornersRadii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
        cornerRadii = floatArrayOf(
            topLeft, topLeft,
            topRight, topRight,
            bottomLeft, bottomLeft,
            bottomRight, bottomRight
        )
    }

    fun setAspectRatio(aspectRatio: Float) {
        this.aspectRatio = aspectRatio
        requestLayout()
    }

    fun setHeightRatio(heightRatio: Double) {
        this.heightRatio = heightRatio
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (aspectRatio != -1f && measuredWidth > 0 && measuredHeight > 0) {
            val newWidth = measuredWidth
            val maxHeight = (measuredWidth / aspectRatio).toInt()
            val newHeight = min(measuredHeight, maxHeight) + 1
            setMeasuredDimension(newWidth, newHeight)
        } else {
            setMeasuredDimension(measuredWidth, measuredHeight)
        }

        if (heightRatio != 1.0) {
            val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
            val newHeight = min(measuredHeight, (parentHeight * heightRatio).toInt())
            setMeasuredDimension(measuredWidth, newHeight)
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
            path.addRoundRect(rect, cornerRadii, Path.Direction.CW)
            canvas.clipPath(path)
        } catch (e: Throwable) {
            log.error { "Failed to clip in-app message image: $e" }
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
