package io.hackle.android.ui.notification

internal enum class ChannelType {
    HACKLE_DEFAULT,
    HACKLE_HIGH,
    CUSTOM;
    
    companion object {
        private val ALL = ChannelType.values().associateBy { it.name }
        fun from(name: String): ChannelType {
            return requireNotNull(ALL[name]) { "name[$name]" }
        }
    }
}