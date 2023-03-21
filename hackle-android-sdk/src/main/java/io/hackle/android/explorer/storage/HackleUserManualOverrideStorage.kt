package io.hackle.android.explorer.storage

import android.content.Context
import io.hackle.android.internal.database.AndroidKeyValueRepository
import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.sdk.core.evaluation.target.ManualOverrideStorage
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.model.Variation
import io.hackle.sdk.core.user.HackleUser

internal class HackleUserManualOverrideStorage(
    private val keyValueRepository: KeyValueRepository,
) : ManualOverrideStorage {

    override fun get(experiment: Experiment, user: HackleUser): Variation? {
        val variationId = get(experiment) ?: return null
        return experiment.getVariationOrNull(variationId)
    }

    fun getAll(): Map<Long, Long> {
        val results = hashMapOf<Long, Long>()
        for (e in keyValueRepository.getAll()) {
            val experimentId = e.key.toLongOrNull() ?: continue
            val variationId = e.value as? Long ?: continue
            results[experimentId] = variationId
        }
        return results
    }

    fun get(experiment: Experiment): Long? {
        val variationId = keyValueRepository.getLong(experiment.id.toString(), -1)
        return if (variationId > 0) variationId else null
    }

    fun set(experiment: Experiment, variationId: Long) {
        keyValueRepository.putLong(experiment.id.toString(), variationId)
    }

    fun remove(experiment: Experiment) {
        keyValueRepository.remove(experiment.id.toString())
    }

    fun clear() {
        keyValueRepository.clear()
    }

    companion object {
        fun create(context: Context, name: String): HackleUserManualOverrideStorage {
            return HackleUserManualOverrideStorage(
                AndroidKeyValueRepository.create(context, name)
            )
        }
    }
}
