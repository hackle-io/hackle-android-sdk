package io.hackle.android.ui.notification

enum class NotificationClickAction(val text: String) {

    APP_OPEN("APP_OPEN"),
    DEEP_LINK("DEEP_LINK");

    companion object {
        private val ALL = NotificationClickAction.values().associateBy { it.text }
        fun from(name: String): NotificationClickAction {
            return requireNotNull(ALL[name]) { "name[$name]" }
        }
    }
}