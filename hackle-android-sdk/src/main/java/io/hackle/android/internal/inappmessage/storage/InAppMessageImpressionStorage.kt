package io.hackle.android.internal.inappmessage.storage

import android.content.Context
import io.hackle.android.internal.database.AndroidKeyValueRepository
import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.utils.parseJson
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.core.model.InAppMessage

/**
 * This class is serialized and deserialized to JSON.
 * Please be careful of field changes.
 */
internal data class InAppMessageImpression(
    val identifiers: Map<String, String>,
    val timestamp: Long
)


internal class InAppMessageImpressionStorage(
    private val keyValueRepository: KeyValueRepository
) {

    fun get(inAppMessage: InAppMessage): List<InAppMessageImpression> {
        val impressions = keyValueRepository.getString(inAppMessage.id.toString()) ?: return emptyList()
        return impressions.parseJson()
    }

    fun set(inAppMessage: InAppMessage, impressions: List<InAppMessageImpression>) {
        keyValueRepository.putString(inAppMessage.id.toString(), impressions.toJson())
    }

    companion object {
        fun create(context: Context, name: String): InAppMessageImpressionStorage {
            return InAppMessageImpressionStorage(AndroidKeyValueRepository.create(context, name))
        }
    }
}
