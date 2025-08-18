package io.hackle.android.internal.context

internal data class HackleAppContext(
    val browserProperties: Map<String, Any>
) {
    companion object {
        internal var DEFAULT = HackleAppContext(emptyMap())

        fun create(browserProperties: Map<String, Any>): HackleAppContext {
            return HackleAppContext(browserProperties)
        }
    }
}
