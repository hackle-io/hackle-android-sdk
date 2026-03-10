package io.hackle.android.ui.inappmessage.event

/**
 * The type of handling to perform when an [InAppMessageEvent] occurs.
 */
internal enum class InAppMessageEventHandleType {

    /**
     * Tracks the event by sending it to the server.
     */
    TRACK,

    /**
     * Executes the behavioral response to the event (e.g., open link, close view).
     */
    ACTION
}
