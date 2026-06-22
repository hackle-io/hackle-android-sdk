package io.hackle.android.ui.inappmessage.view.html

internal class InAppMessageHtmlReadyHandler(
    private val isClosed: () -> Boolean,
    private val requestFocus: () -> Unit,
    private val ready: () -> Unit,
) {
    fun onBridgeEvaluated() {
        if (isClosed()) {
            return
        }
        requestFocus()
        ready()
    }
}
