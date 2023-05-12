package io.hackle.android.inappmessage.storage

import android.content.Context
import io.hackle.android.internal.database.AndroidKeyValueRepository
import io.hackle.android.internal.database.KeyValueRepository

internal class HackleInAppMessageStorage internal constructor(
    private val keyValueRepository: KeyValueRepository
): io.hackle.sdk.core.evaluation.evaluator.inappmessage.storage.HackleInAppMessageStorage {


    override fun getAll(): Map<Long, Long> {
        val results = hashMapOf<Long, Long>()
        for (e in keyValueRepository.getAll()) {
            val inAppMessageKey = e.key.toLongOrNull() ?: continue
            val invisibleTime = e.value as? Long ?: continue
            results[inAppMessageKey] = invisibleTime
        }
        return results
    }

    override fun getInvisibleUntil(inAppMessageKey: Long): Long {
        return keyValueRepository.getLong(inAppMessageKey.toString(), -1L)
    }

    override fun setInvisibleUntil(inAppMessageKey: Long, until: Long) {
        keyValueRepository.putLong(inAppMessageKey.toString(), System.currentTimeMillis() + until)
    }


    override fun remove(inAppMessageKey: Long) {
        keyValueRepository.remove(inAppMessageKey.toString())
    }

    override fun clear() {
        keyValueRepository.clear()
    }

    companion object {

        //TODO 테스트 용으로 1분으로 설정
        const val NEXT_24_HOUR_MILLISECONDS = 60000L
//        const val NEXT_24_HOUR_MILLISECONDS = 86400000L

        private var INSTANCE: HackleInAppMessageStorage? = null

        private val LOCK = Any()


        fun create(context: Context, name: String): HackleInAppMessageStorage {
            return INSTANCE ?: HackleInAppMessageStorage(
                AndroidKeyValueRepository.create(context, name)
            ).also { INSTANCE = it }
        }

        @JvmStatic
        fun getInstance(): HackleInAppMessageStorage {
            return synchronized(LOCK) {
                checkNotNull(INSTANCE) { "InAppMessageStorage not initialized" }
            }
        }
    }
}