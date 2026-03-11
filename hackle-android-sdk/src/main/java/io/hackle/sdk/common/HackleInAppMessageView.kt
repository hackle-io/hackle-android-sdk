package io.hackle.sdk.common

/**
 * The view presenting the InAppMessage.
 */
interface HackleInAppMessageView {

    /**
     * The [HackleInAppMessage] displayed by this view.
     */
    val inAppMessage: HackleInAppMessage

    /**
     * Closes the InAppMessage view.
     */
    fun close()
}
