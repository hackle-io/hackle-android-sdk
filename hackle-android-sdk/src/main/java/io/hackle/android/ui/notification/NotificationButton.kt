package io.hackle.android.ui.notification

internal data class NotificationButton(
    val title: String,
    val clickAction: NotificationClickAction,
    val link: String?,
    val type: String? = null
)
