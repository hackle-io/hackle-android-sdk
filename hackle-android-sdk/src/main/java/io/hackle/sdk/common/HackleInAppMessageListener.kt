package io.hackle.sdk.common

interface HackleInAppMessageListener {

    /**
     * Called before the InAppMessage is presented.
     */
    fun onWillOpen(inAppMessage: HackleInAppMessage)

    /**
     * Called after an InAppMessage is presented.
     */
    fun onDidOpen(inAppMessage: HackleInAppMessage)

    /**
     * Called before the InAppMessage is closed.
     */
    fun onWillClose(inAppMessage: HackleInAppMessage)

    /**
     * Called after an InAppMessage is closed.
     */
    fun onDidClose(inAppMessage: HackleInAppMessage)

    /**
     * Called when a clickable element is clicked in an InAppMessage.
     *
     * @param view The view presenting the InAppMessage
     * @param inAppMessage The InAppMessage being presented
     * @param action An action performed by the user by clicking InAppMessage.
     *
     * @return boolean indicating whether the click action was custom handled.
     *         If true, Hackle SDK only track a click event and do nothing else.
     *         If false, track click event and handle the click action.
     */
    fun onClick(
        view: HackleInAppMessageView,
        inAppMessage: HackleInAppMessage,
        action: HackleInAppMessageAction
    ): Boolean
}