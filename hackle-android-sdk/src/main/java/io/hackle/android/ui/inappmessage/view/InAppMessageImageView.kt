package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import io.hackle.sdk.core.internal.log.Logger
import kotlin.math.min

internal class InAppMessageImageView : ImageView {

    private var path = Path()
    private var rect = RectF()
    private var radius = 8f * context.resources.displayMetrics.density
    private var radii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
    private var aspectRatio = -1f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

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

    override fun onDraw(canvas: Canvas?) {
        try {
            canvas?.let {
                rect.set(0f, 0f, width.toFloat(), height.toFloat())
                path.reset()
                path.addRoundRect(rect, radii, Path.Direction.CW)
                it.clipPath(path)
                super.onDraw(it)
            }
        } catch (e: Exception) {
            log.error { "Encountered exception while trying to clip image" }
        }

    }


    fun setAspectRatio(aspectRatio: Float) {
        this.aspectRatio = aspectRatio
        requestLayout()
    }


    companion object {
        private val log = Logger<InAppMessageImageView>()
    }

}