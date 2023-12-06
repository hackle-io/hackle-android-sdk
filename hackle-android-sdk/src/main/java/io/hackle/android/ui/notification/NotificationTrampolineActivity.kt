package io.hackle.android.ui.notification

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.hackle.android.ui.notification.Constants.KEY_CLICK_ACTION
import io.hackle.sdk.core.internal.log.Logger

internal class NotificationTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.debug { "Notification trampoline activity open." }

        val extras = intent?.extras
        if (extras == null) {
            log.debug { "Notification trampoline activity received nothing." }
            finish()
            return
        }

        trampoline(intent.data, extras)
        finish()
    }

    private fun trampoline(data: Uri?, bundle: Bundle) {
        var clickAction = NotificationClickAction.APP_OPEN
        try {
            val text = requireNotNull(bundle.getString(KEY_CLICK_ACTION))
            clickAction = NotificationClickAction.from(text)
        } catch (_: Exception) {
            log.debug { "Cannot find click action by default is app open." }
        }

        log.debug { "Notification click action : $clickAction" }

        when (clickAction) {
            NotificationClickAction.APP_OPEN -> {
                startLauncherActivity(bundle)
            }
            NotificationClickAction.DEEP_LINK -> {
                if (data == null) {
                    log.debug { "Landing url is empty." }
                    startLauncherActivity(bundle)
                } else {
                    val trampolineIntent = Intent(Intent.ACTION_VIEW, data)
                    trampolineIntent.putExtras(bundle)
                    trampolineIntent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                    try {
                        startActivity(trampolineIntent)
                    } catch (e: ActivityNotFoundException) {
                        log.debug { "Failed to land anywhere." }
                        startLauncherActivity(bundle)
                    }

                    log.debug { "Redirected url : $data" }
                }
            }
        }
    }

    private fun startLauncherActivity(bundle: Bundle) {
        val launcherIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launcherIntent == null) {
            log.debug { "Cannot find launcher activity." }
            return
        }
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        launcherIntent.putExtras(bundle)
        startActivity(launcherIntent)

        log.debug { "Started launcher activity." }
    }

    companion object {

        private val log = Logger<NotificationTrampolineActivity>()
    }
}