package io.hackle.android.internal.optout

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.sdk.core.internal.log.Logger

internal class OptOutManager(
    private val keyValueRepository: KeyValueRepository,
    private val eventProcessor: DefaultEventProcessor,
    configOptOutTracking: Boolean,
) {

    private val lock = Any()

    @Volatile
    var isOptOutTracking: Boolean = false
        private set

    init {
        val savedOptOut = keyValueRepository.getString(OPT_OUT_KEY)?.toBoolean() ?: false
        isOptOutTracking = configOptOutTracking || savedOptOut
        save(isOptOutTracking)
    }

    fun setOptOutTracking(optOut: Boolean) {
        synchronized(lock) {
            if (optOut == isOptOutTracking) {
                return
            }

            if (optOut) {
                // optIn -> optOut 전환: 기존 로컬 이벤트를 best-effort flush한 후 차단
                eventProcessor.flush()
            }

            isOptOutTracking = optOut
            save(optOut)
            log.info { "OptOutTracking changed to $optOut" }
        }
    }

    private fun save(optOut: Boolean) {
        keyValueRepository.putString(OPT_OUT_KEY, optOut.toString())
    }

    companion object {
        private val log = Logger<OptOutManager>()
        private const val OPT_OUT_KEY = "opt_out_tracking"
    }
}
