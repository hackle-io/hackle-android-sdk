package io.hackle.android.inappmessage

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import io.hackle.android.HackleActivity
import io.hackle.android.inappmessage.activity.InAppMessageActivity
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.core.decision.InAppMessageDecision
import io.hackle.sdk.core.internal.log.Logger


internal class InAppMessageRenderer : Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null

    fun render(
        inAppMessageDecision: InAppMessageDecision
    ) {
        val activity = currentActivity ?: return

        if (activity is HackleActivity) {
            return
        }

        if (inAppMessageDecision.inAppMessage == null || inAppMessageDecision.message == null) {
            log.error { "The message need to show couldn't be null" }
            return
        }
        val intent = Intent(activity, InAppMessageActivity::class.java)
        val inAppMessage = inAppMessageDecision.inAppMessage!!.toJson()
        val message = inAppMessageDecision.message!!.toJson()

        intent.putExtra("message", message)
        intent.putExtra("inAppMessage", inAppMessage)

        activity.startActivity(intent)
    }


    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity

    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }


    companion object {
        private val log = Logger<InAppMessageRenderer>()
    }

}