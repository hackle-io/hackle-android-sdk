package io.hackle.android.ui.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.hackle.sdk.core.internal.log.Logger

/**
 * Opens push notification landing urls, in-app message links, and the launcher activity.
 */
internal object LinkOpener {

    private val log = Logger<LinkOpener>()

    internal val NOTIFICATION_INTENT_FLAGS =
        Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP

    /**
     * Opens the [uri] with ACTION_VIEW.
     * Falls back to the launcher activity with the [uri] as data if nothing can handle it.
     */
    fun openLink(context: Context, uri: Uri, extras: Bundle?) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        extras?.let { intent.putExtras(it) }
        intent.addFlags(NOTIFICATION_INTENT_FLAGS)
        try {
            context.startActivity(intent)
            log.debug { "Redirected to link." }
        } catch (e: ActivityNotFoundException) {
            log.debug { "Failed to land anywhere." }
            startLauncherActivity(context, extras, uri)
        } catch (e: Exception) {
            log.error { "Failed to open link [error=${e.javaClass.simpleName}]" }
        }
    }

    /**
     * Opens the in-app message [uri] with ACTION_VIEW from the given [context].
     * Unlike [openLink], no task flags are added and any failure is only logged.
     */
    fun openInAppMessageLink(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            log.error { "Failed to handle URI [error=${e.javaClass.simpleName}]" }
        }
    }

    fun startLauncherActivity(
        context: Context,
        extras: Bundle?,
        data: Uri? = null,
    ) {
        val launcherIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launcherIntent == null) {
            log.debug { "Cannot find launcher activity." }
            return
        }
        launcherIntent.setData(data)
        launcherIntent.addFlags(NOTIFICATION_INTENT_FLAGS)
        extras?.let { launcherIntent.putExtras(it) }
        try {
            context.startActivity(launcherIntent)
            log.debug { "Started launcher activity." }
        } catch (e: Exception) {
            log.error { "Failed to start launcher activity [error=${e.javaClass.simpleName}]" }
        }
    }
}
