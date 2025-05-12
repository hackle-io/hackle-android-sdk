package io.hackle.android.internal.inappmessage.storage

import android.content.Context
import io.hackle.android.internal.database.repository.AndroidKeyValueRepository
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.core.evaluation.target.InAppMessageImpression
import io.hackle.sdk.core.evaluation.target.InAppMessageImpressionStorage
import io.hackle.sdk.core.model.InAppMessage


internal class AndroidInAppMessageImpressionStorage(
    private val keyValueRepository: KeyValueRepository
) : InAppMessageImpressionStorage {

    override fun get(inAppMessage: InAppMessage): List<InAppMessageImpression> {
        val impressions =
            keyValueRepository.getString(inAppMessage.id.toString()) ?: return emptyList()
        return impressions.parseJson()
    }

    override fun set(inAppMessage: InAppMessage, impressions: List<InAppMessageImpression>) {
        keyValueRepository.putString(inAppMessage.id.toString(), impressions.toJson())
    }

    companion object {
        fun create(context: Context, name: String): AndroidInAppMessageImpressionStorage {
            return AndroidInAppMessageImpressionStorage(
                AndroidKeyValueRepository.create(
                    context,
                    name
                )
            )
        }
    }
}
