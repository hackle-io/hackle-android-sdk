package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import io.hackle.sdk.core.internal.log.Logger
import kotlin.math.min

internal class InAppMessageImageView : ImageView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var path = Path()
    private var rect = RectF()
    private var cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private var aspectRatio = -1f

    fun setCornersRadiiPx(px: Float) {
        setCornersRadiiPx(px, px, px, px)
    }

    fun setCornersRadiiPx(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpec =
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY)
        super.onMeasure(widthSpec, heightMeasureSpec)

        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (aspectRatio != -1f && measuredHeight > 0 && measuredWidth > 0) {
            var newWidth = measuredWidth
            val maxHeight = min((newWidth / aspectRatio).toInt(), (parentHeight * 0.8).toInt()) + 1
            newWidth = (maxHeight * aspectRatio).toInt()
            val newHeight = min(measuredHeight, maxHeight)
            setMeasuredDimension(newWidth, newHeight)
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
