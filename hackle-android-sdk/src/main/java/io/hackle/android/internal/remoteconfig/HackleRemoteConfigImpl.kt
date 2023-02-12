package io.hackle.android.internal.remoteconfig

import io.hackle.android.internal.monitoring.metric.DecisionMetrics
import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.DecisionReason.EXCEPTION
import io.hackle.sdk.common.decision.RemoteConfigDecision
import io.hackle.sdk.core.client.HackleInternalClient
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Timer
import io.hackle.sdk.core.model.ValueType

internal class HackleRemoteConfigImpl(
    private val user: User?,
    private val client: HackleInternalClient,
    private val userManager: UserManager,
    private val hackleUserResolver: HackleUserResolver,
) : HackleRemoteConfig {

    override fun getString(key: String, defaultValue: String): String {
        return get(key, ValueType.STRING, defaultValue).value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return get<Number>(key, ValueType.NUMBER, defaultValue).value.toInt()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return get<Number>(key, ValueType.NUMBER, defaultValue).value.toLong()
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return get<Number>(key, ValueType.NUMBER, defaultValue).value.toDouble()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return get(key, ValueType.BOOLEAN, defaultValue).value
    }

    private fun <T : Any> get(
        key: String,
        requiredType: ValueType,
        defaultValue: T,
    ): RemoteConfigDecision<T> {
        val sample = Timer.start()
        return try {
            val user = this.user?.let { userManager.setUser(it) } ?: userManager.currentUser
            val hackleUser = hackleUserResolver.resolve(user)
            client.remoteConfig(key, hackleUser, requiredType, defaultValue)
        } catch (e: Exception) {
            log.error { "Unexpected exception while deciding remote config parameter[$key]. Returning default value." }
            RemoteConfigDecision.of(defaultValue, EXCEPTION)
        }.also {
            DecisionMetrics.remoteConfig(sample, key, it)
        }
    }

    companion object {
        private val log = Logger<HackleRemoteConfigImpl>()
    }
}
