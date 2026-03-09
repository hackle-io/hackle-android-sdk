package io.hackle.sdk.common

/**
 * Configures session-related policies.
 */
class HackleSessionPolicy private constructor(builder: Builder) {

    /** Condition for preserving the session on identifier change. */
    val persistCondition: HackleSessionPersistCondition = builder.persistCondition

    /** Session timeout configuration. */
    val timeoutCondition: HackleSessionTimeoutCondition = builder.timeoutCondition

    override fun toString(): String = "HackleSessionPolicy(persistCondition=$persistCondition, timeout=$timeoutCondition)"

    class Builder {
        internal var persistCondition: HackleSessionPersistCondition = HackleSessionPersistCondition.ALWAYS_NEW_SESSION
        internal var timeoutCondition: HackleSessionTimeoutCondition = HackleSessionTimeoutCondition.DEFAULT

        fun persistCondition(persistCondition: HackleSessionPersistCondition) = apply {
            this.persistCondition = persistCondition
        }

        fun timeoutCondition(timeoutCondition: HackleSessionTimeoutCondition) = apply {
            this.timeoutCondition = timeoutCondition
        }

        fun build(): HackleSessionPolicy = HackleSessionPolicy(this)
    }

    fun toBuilder(): Builder = Builder().apply {
        this.persistCondition = this@HackleSessionPolicy.persistCondition
        this.timeoutCondition = this@HackleSessionPolicy.timeoutCondition
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmField
        val DEFAULT: HackleSessionPolicy = builder().build()
    }
}
