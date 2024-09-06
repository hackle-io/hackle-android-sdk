package io.hackle.sdk.common

interface HackleInAppMessageListener {

    /**
     * Called before the InAppMessage is opened.
     * (before the view is added to the layout)
     */
    fun beforeInAppMessageOpen(inAppMessage: HackleInAppMessage)

    /**
     * Called after the InAppMessage is opened.
     * (after the view is added to the layout and animation has completed)
     */
    fun afterInAppMessageOpen(inAppMessage: HackleInAppMessage)

    /**
     * Called before the InAppMessage is closed.
     * (before the view is removed from the layout)
     */
    fun beforeInAppMessageClose(inAppMessage: HackleInAppMessage)

    /**
     * Called after the InAppMessage is closed.
     * (before the view is removed from the layout and animation has completed)
     */
    fun afterInAppMessageClose(inAppMessage: HackleInAppMessage)

    /**
     * Called when a clickable element is clicked in an InAppMessage.
     *
     * @param inAppMessage The InAppMessage being presented
     * @param view The view presenting the InAppMessage
     * @param action An action performed by the user by clicking InAppMessage.
     *
     * @return boolean indicating whether the click action was custom handled.
     *         If true, Hackle SDK only track a click event and do nothing else.
     *         If false, track click event and handle the click action.
     */
    fun onInAppMessageClick(
        inAppMessage: HackleInAppMessage,
        view: HackleInAppMessageView,
        action: HackleInAppMessageAction
    ): Boolean
}
