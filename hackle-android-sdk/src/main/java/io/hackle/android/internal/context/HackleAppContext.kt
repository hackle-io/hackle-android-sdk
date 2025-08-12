package io.hackle.android.internal.context

internal data class HackleAppContext(
    val browserProperties: Map<String, Any>?
) {
    companion object{
        internal var default = HackleAppContext(null)
        
        fun create(browserProperties: Map<String, Any>): HackleAppContext {
            return HackleAppContext(browserProperties)
        }
    }
}
