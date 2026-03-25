package io.hackle.android.ui.inappmessage.view

internal interface InAppMessageViewProvider {
    val currentView: InAppMessageView?
    fun getView(id: String): InAppMessageView?
}
