package io.hackle.android.explorer.view.button

import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import io.hackle.android.explorer.activity.HackleUserExplorerActivity
import kotlin.math.abs

internal class HackleUserExplorerButtonTouchHandler(
    private val windowManager: WindowManager,
    private val layoutParams: LayoutParams,
) : View.OnTouchListener {

    private var initialX: Int = 0
    private var initialTouchX: Float = 0.0F

    private var initialY: Int = 0
    private var initialTouchY: Float = 0.0F

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                windowManager.updateViewLayout(v, layoutParams)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val endX = event.rawX
                val endY = event.rawY
                if (isClick(initialTouchX, endX, initialTouchY, endY)) {
                    v.performClick()
                    val intent = Intent(v.context, HackleUserExplorerActivity::class.java)
                    v.context.startActivity(intent)
                }
                return true
            }
            else -> false
        }
    }

    private fun isClick(startX: Float, endX: Float, startY: Float, endY: Float): Boolean {
        val differenceX = abs(startX - endX)
        val differenceY = abs(startY - endY)
        return !(differenceX > 5 || differenceY > 5)
    }
}
