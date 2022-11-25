package io.hackle.android.internal.remoteconfig

import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.DecisionReason.EXCEPTION
import io.hackle.sdk.common.decision.DecisionReason.INVALID_INPUT
import io.hackle.sdk.common.decision.RemoteConfigDecision
import io.hackle.sdk.core.client.HackleInternalClient
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.ValueType

internal class HackleRemoteConfigImpl(
    private val user: User,
    private val client: HackleInternalClient,
    private val userResolver: HackleUserResolver,
) : HackleRemoteConfig {

    override fun getString(key: String, defaultValue: String): String {
        return get(user, key, ValueType.STRING, defaultValue).value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return get<Number>(user, key, ValueType.NUMBER, defaultValue).value.toInt()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return get<Number>(user, key, ValueType.NUMBER, defaultValue).value.toLong()
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return get<Number>(user, key, ValueType.NUMBER, defaultValue).value.toDouble()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return get(user, key, ValueType.BOOLEAN, defaultValue).value
    }

    private fun <T : Any> get(
        user: User,
        key: String,
        requiredType: ValueType,
        defaultValue: T,
    ): RemoteConfigDecision<T> {
        return try {
            val hackleUser = userResolver.resolveOrNull(user)
                ?: return RemoteConfigDecision.of(defaultValue, INVALID_INPUT)
            client.remoteConfig(key, hackleUser, requiredType, defaultValue)
        } catch (e: Exception) {
            log.error { "Unexpected exception while deciding remote config parameter[$key]. Returning default value." }
            RemoteConfigDecision.of(defaultValue, EXCEPTION)
        }
    }

    companion object {
        private val log = Logger<HackleRemoteConfigImpl>()
    }
}
