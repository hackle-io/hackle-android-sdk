package io.hackle.android.ui.explorer.view.listener

import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner

internal abstract class OnSpinnerItemSelectedListener : OnItemSelectedListener, OnTouchListener {

    private var userSelecting: Boolean = false

    abstract fun onItemSelectedByUser(parent: AdapterView<*>?, view: View?, position: Int, id: Long)

    final override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long,
    ) {
        if (!userSelecting) {
            return
        }

        onItemSelectedByUser(parent, view, position, id)
        userSelecting = false
    }

    final override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    final override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        userSelecting = true
        return false
    }
}

internal fun Spinner.setOnSpinnerItemSelectedListener(listener: OnSpinnerItemSelectedListener) {
    onItemSelectedListener = listener
    setOnTouchListener(listener)
}
