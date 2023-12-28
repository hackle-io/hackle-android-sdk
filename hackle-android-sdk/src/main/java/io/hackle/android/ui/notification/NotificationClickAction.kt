package io.hackle.android.ui.notification

internal enum class NotificationClickAction {

    APP_OPEN,
    DEEP_LINK;

    companion object {
        private val ALL = NotificationClickAction.values().associateBy { it.name }
        fun from(name: String): NotificationClickAction {
            return requireNotNull(ALL[name]) { "name[$name]" }
        }
    }
}