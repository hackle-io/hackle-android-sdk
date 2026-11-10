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
 * The SDK does not open links passed to the delegate or start an activity for those links.
 * The SDK keeps a strong reference to the registered delegate until null is passed.
 *
 * [handleUrl] may be called while the app has no activity at all.
 * When the app initializes the SDK in `Application.onCreate` and the user taps a push notification
 * while the app is not running, the SDK calls [handleUrl] with [HackleUrlChannel.PUSH] without
 * starting the launcher activity. In that case, bringing the app to the foreground, preparing the
 * screen, and holding the link until the screen is ready are all the delegate's responsibility.
 * A delegate that only navigates within an existing activity cannot open the link in that case.
 *
 * A push link tapped before the SDK is initialized (e.g. when the app initializes the SDK in an
 * activity) is opened by the SDK right away and is not passed to the delegate.
 * To pass it to the delegate instead, add the following to the `<application>` element of
 * `AndroidManifest.xml`. The SDK then starts the launcher activity, holds the link, and replays it
 * once the SDK is initialized.
 * ```xml
 * <meta-data
 *     android:name="io.hackle.android.defer_push_link_until_initialized"
 *     android:value="true" />
 * ```
 */
interface HackleDelegate {

    /**
     * Called on the main thread when a link should be opened.
     *
     * @param context the [HackleUrlContext] of the link
     */
    fun handleUrl(context: HackleUrlContext)
}
