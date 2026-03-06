package io.hackle.sdk.common

/**
 * Configures session-related policies.
 */
class HackleSessionPolicy private constructor(builder: Builder) {

    /** Condition for preserving the session on identifier change. `null` means always start a new session. */
    val persistCondition: HackleSessionPersistCondition? = builder.persistCondition

    /** Session timeout configuration. */
    val timeout: HackleSessionTimeout = builder.timeout

    override fun toString(): String = "HackleSessionPolicy(persistCondition=$persistCondition, timeout=$timeout)"

    class Builder {
        internal var persistCondition: HackleSessionPersistCondition? = null
        internal var timeout: HackleSessionTimeout = HackleSessionTimeout.DEFAULT

        fun persistCondition(condition: HackleSessionPersistCondition) = apply {
            this.persistCondition = condition
        }

        fun timeout(timeout: HackleSessionTimeout) = apply {
            this.timeout = timeout
        }

        fun build(): HackleSessionPolicy = HackleSessionPolicy(this)
    }

    fun toBuilder(): Builder = Builder().apply {
        this.persistCondition = this@HackleSessionPolicy.persistCondition
        this.timeout = this@HackleSessionPolicy.timeout
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmField
        val DEFAULT: HackleSessionPolicy = builder().build()
    }
}
