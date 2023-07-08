package io.hackle.android.ui.inappmessage.storage

import android.content.Context
import io.hackle.android.internal.database.AndroidKeyValueRepository
import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.storage.HackleInAppMessageStorage
import io.hackle.sdk.core.model.InAppMessage

internal class AndroidHackleInAppMessageStorage internal constructor(
    private val keyValueRepository: KeyValueRepository
) : HackleInAppMessageStorage {

    override fun exist(inAppMessage: InAppMessage, nowTimeMillis: Long): Boolean {
        val key = key(inAppMessage)
        val expiredTimeMillis = keyValueRepository.getLong(key, -1L)

        if (expiredTimeMillis < 0) {
            return false
        }

        return if (nowTimeMillis <= expiredTimeMillis) {
            true
        } else {
            keyValueRepository.remove(key)
            false
        }
    }

    override fun put(inAppMessage: InAppMessage, expiredAtMillis: Long) {
        keyValueRepository.putLong(key(inAppMessage), expiredAtMillis)
    }

    fun put(inAppMessageKey: Long, expiredAtMillis: Long) {
        keyValueRepository.putLong(inAppMessageKey.toString(), expiredAtMillis)
    }

    private fun key(inAppMessage: InAppMessage): String {
        return inAppMessage.key.toString()
    }


    companion object {

        const val NEXT_24_HOUR_MILLISECONDS = 24 * 60 * 60 * 1000L

        fun create(context: Context, name: String): AndroidHackleInAppMessageStorage {
            return AndroidHackleInAppMessageStorage(
                AndroidKeyValueRepository.create(
                    context,
                    name
                )
            )
        }
    }
}