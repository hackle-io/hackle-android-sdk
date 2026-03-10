package io.hackle.android.ui.inappmessage.view

internal interface InAppMessageViewProvider {
    val currentView: InAppMessageView?
    fun get(id: String): InAppMessageView?
}