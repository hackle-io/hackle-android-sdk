package io.hackle.android.ui.explorer.view.button

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.Gravity.BOTTOM
import android.view.Gravity.END
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import io.hackle.android.R
import io.hackle.android.ui.explorer.activity.HackleUserExplorerActivity
import kotlin.math.abs

internal class HackleUserExplorerButton : FrameLayout, OnTouchListener {

    private val logo: ImageView

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        val root = LayoutInflater.from(context)
            .inflate(R.layout.hackle_view_user_explorer_button, this, true)
        this.logo = root.findViewById(R.id.hackle_image_view_logo)
        initLayout()
        setOnTouchListener(this)
    }

    private fun initLayout() {
        val params = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        params.gravity = (BOTTOM or END)
        params.setMargins(0, 0, 24, 24)
        layoutParams = params
        requestLayout()
    }

    private var initialX: Float = 0.0F
    private var initialTouchX: Float = 0.0F
    private var initialY: Float = 0.0F
    private var initialTouchY: Float = 0.0F

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = v.x
                initialY = v.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                true
            }

            MotionEvent.ACTION_MOVE -> {
                v.x = initialX + (event.rawX - initialTouchX)
                v.y = initialY + (event.rawY - initialTouchY)
                true
            }

            MotionEvent.ACTION_UP -> {
                if (isClick(initialTouchX, event.rawX, initialTouchY, event.rawY)) {
                    v.performClick()
                    val intent = Intent(v.context, HackleUserExplorerActivity::class.java)
                    v.context.startActivity(intent)
                }
                true
            }

            else -> false
        }
    }

    private fun isClick(startX: Float, endX: Float, startY: Float, endY: Float): Boolean {
        val differenceX = abs(startX - endX)
        val differenceY = abs(startY - endY)
        return !(differenceX > 10 || differenceY > 10)
    }
}
