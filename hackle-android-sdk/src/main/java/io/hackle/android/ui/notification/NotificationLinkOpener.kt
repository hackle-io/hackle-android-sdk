package io.hackle.android.ui.notification

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.hackle.android.ui.notification.NotificationTrampolineActivity.Companion.NOTIFICATION_INTENT_FLAGS
import io.hackle.sdk.core.internal.log.Logger

/**
 * Opens push notification landing urls and the launcher activity.
 */
internal object NotificationLinkOpener {

    private val log = Logger<NotificationLinkOpener>()

    /**
     * Opens the [uri] with ACTION_VIEW.
     * Falls back to the launcher activity with the [uri] as data if nothing can handle it.
     */
    fun openLink(context: Context, uri: Uri, extras: Bundle) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.putExtras(extras)
        intent.addFlags(NOTIFICATION_INTENT_FLAGS)
        try {
            context.startActivity(intent)
            log.debug { "Redirected to: $uri" }
        } catch (e: ActivityNotFoundException) {
            log.debug { "Failed to land anywhere: $uri" }
            startLauncherActivity(context, extras, uri)
        }
    }

    fun startLauncherActivity(
        context: Context,
        extras: Bundle,
        data: Uri? = null,
        flags: Int = NOTIFICATION_INTENT_FLAGS,
    ) {
        val launcherIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launcherIntent == null) {
            log.debug { "Cannot find launcher activity." }
            return
        }
        launcherIntent.setData(data)
        launcherIntent.addFlags(flags)
        launcherIntent.putExtras(extras)
        context.startActivity(launcherIntent)

        log.debug { "Started launcher activity." }
    }
}
