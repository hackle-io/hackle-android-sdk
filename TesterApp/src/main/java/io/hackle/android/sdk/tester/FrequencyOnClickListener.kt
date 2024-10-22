package io.hackle.android.sdk.tester

import android.app.Activity
import android.view.View
import io.hackle.android.HackleApp

class FrequencyOnClickListener(
    private val activity: Activity,
    private val millis: Int,
    private val count: Int,
) : View.OnClickListener {

    private val history = MutableList(5) { 0L }
    private var currentIndex = 0

    override fun onClick(v: View) {
        val now = System.currentTimeMillis()
        history[currentIndex] = now
        currentIndex = (currentIndex + 1) % count
        if ((now - history[currentIndex]) <= millis) {
//            HackleApp.getInstance().showUserExplorer(activity)
        }
    }

    companion object {

        @JvmOverloads
        @JvmStatic
        fun bindTo(activity: Activity, view: View, millis: Int = 2000, count: Int = 5) {
            view.setOnClickListener(FrequencyOnClickListener(activity, millis, count))
        }
    }
}