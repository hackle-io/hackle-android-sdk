package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.RelativeLayout
import io.hackle.android.ui.core.CornerRadii

internal open class InAppMessageContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val path = Path()
    private val rect = RectF()
    private var cornerRadii: CornerRadii = CornerRadii.ZERO

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        updateClipPath()
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    fun setCornerRadii(radii: CornerRadii) {
        cornerRadii = radii
        updateClipPath()
        invalidate()
    }

    private fun updateClipPath() {
        path.reset()
        path.addRoundRect(rect, cornerRadii.toFloatArray(), Path.Direction.CW)
    }
}
