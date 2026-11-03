package io.hackle.android

import android.net.Uri

/**
 * The channel through which a link was delivered.
 */
enum class HackleUrlChannel {
    /**
     * A link from a push notification.
     */
    PUSH,

    /**
     * A link from an in-app message.
     */
    IN_APP_MESSAGE
}

/**
 * Context of a link that Hackle would otherwise open.
 *
 * @property url the link to open
 * @property channel the [HackleUrlChannel] through which the link was delivered
 */
class HackleUrlContext internal constructor(
    val url: Uri,
    val channel: HackleUrlChannel,
) {
    override fun toString(): String {
        return "HackleUrlContext(url=$url, channel=$channel)"
    }
}

/**
 * Delegate interface for handling links from push notifications and in-app messages.
 *
 * Register it with [HackleConfig.Builder.delegate] or [HackleApp.setDelegate].
 * While a delegate is registered, the SDK does not open links itself.
 * The SDK keeps a strong reference to the registered delegate until null is passed.
 */
interface HackleDelegate {

    /**
     * Called on the main thread when a link should be opened.
     *
     * @param context the [HackleUrlContext] of the link
     */
    fun handleUrl(context: HackleUrlContext)
}
