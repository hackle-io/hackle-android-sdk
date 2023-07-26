package io.hackle.android.internal.inappmessage.storage

import android.content.Context
import io.hackle.android.internal.database.AndroidKeyValueRepository
import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.sdk.core.evaluation.target.InAppMessageHiddenStorage
import io.hackle.sdk.core.model.InAppMessage

internal class AndroidInAppMessageHiddenStorage(
    private val keyValueRepository: KeyValueRepository,
) : InAppMessageHiddenStorage {

    override fun exist(inAppMessage: InAppMessage, now: Long): Boolean {
        val key = key(inAppMessage)
        val expireAt = keyValueRepository.getLong(key, -1)
        if (expireAt < 0) {
            return false
        }
        return if (now <= expireAt) {
            true
        } else {
            keyValueRepository.remove(key)
            false
        }
    }

    override fun put(inAppMessage: InAppMessage, expireAt: Long) {
        return keyValueRepository.putLong(key(inAppMessage), expireAt)
    }

    private fun key(inAppMessage: InAppMessage): String {
        return inAppMessage.key.toString()
    }

    companion object {
        fun create(context: Context, name: String): AndroidInAppMessageHiddenStorage {
            return AndroidInAppMessageHiddenStorage(AndroidKeyValueRepository.create(context, name))
        }
    }
}
