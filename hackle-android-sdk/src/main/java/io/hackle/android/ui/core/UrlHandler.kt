package io.hackle.android.ui.core

import android.content.Context
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleUrlChannel

/**
 * Opens a link delivered through a [HackleUrlChannel].
 */
internal interface UrlHandler {

    /**
     * @param context the context to start activities from
     * @param url the link to open
     * @param channel the channel through which the link was delivered
     * @param extras the push notification extras, if the link came from a push notification
     */
    fun open(context: Context, url: Uri, channel: HackleUrlChannel, extras: Bundle?)
}
