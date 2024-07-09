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

internal class InAppMessageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var path = Path()
    private var rect = RectF()
    private var cornerRadii: CornersRadii = CornersRadii.ZERO
    private var aspectRatio: AspectRatio? = null

    fun setCornersRadius(px: Float) {
        setCornersRadii(CornersRadii.of(px))
    }

    fun setCornersRadii(cornerRadii: CornersRadii) {
        this.cornerRadii = cornerRadii
    }

    fun setAspectRatio(aspectRatio: AspectRatio?) {
        this.aspectRatio = aspectRatio
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val aspectRatio = aspectRatio
        if (aspectRatio != null && measuredWidth > 0 && measuredHeight > 0) {
            val newWidth = measuredWidth
            val maxHeight = aspectRatio.calculateHeight(measuredWidth)
            val newHeight = min(measuredHeight, maxHeight) + 1
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

    /*
     *   2          3
     * 1 ┌──────────┐ 4
     *   │          │
     *   │          │
     * 8 └──────────┘ 5
     *   7          6
     */
    data class CornersRadii(
        val v1: Float,
        val v2: Float,
        val v3: Float,
        val v4: Float,
        val v5: Float,
        val v6: Float,
        val v7: Float,
        val v8: Float
    ) {
        fun toFloatArray(): FloatArray {
            return floatArrayOf(v1, v2, v3, v4, v5, v6, v7, v8)
        }

        companion object {
            val ZERO = of(0f)

            fun of(radius: Float): CornersRadii {
                return CornersRadii(radius, radius, radius, radius, radius, radius, radius, radius)
            }

            fun of(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float): CornersRadii {
                return CornersRadii(
                    topLeft,
                    topLeft,
                    topRight,
                    topRight,
                    bottomRight,
                    bottomRight,
                    bottomLeft,
                    bottomLeft
                )
            }
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
