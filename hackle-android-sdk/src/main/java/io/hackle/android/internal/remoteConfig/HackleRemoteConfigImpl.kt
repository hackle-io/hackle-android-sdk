package io.hackle.android.internal.remoteConfig

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.monitoring.metric.DecisionMetrics
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.DecisionReason.EXCEPTION
import io.hackle.sdk.common.decision.RemoteConfigDecision
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Timer
import io.hackle.sdk.core.model.ValueType

internal class HackleRemoteConfigImpl(
    private val user: User?,
    private val core: HackleCore,
    private val userManager: UserManager,
    private var hackleAppContext: HackleAppContext = HackleAppContext.DEFAULT
): HackleRemoteConfig {

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
    
    internal fun getString(key: String, defaultValue: String, hackleAppContext: HackleAppContext): String {
        this.hackleAppContext = hackleAppContext
        return getString(key, defaultValue)
    }

    internal fun getInt(key: String, defaultValue: Int, hackleAppContext: HackleAppContext): Int {
        this.hackleAppContext = hackleAppContext
        return getInt(key, defaultValue)
    }

    internal fun getLong(key: String, defaultValue: Long, hackleAppContext: HackleAppContext): Long {
        this.hackleAppContext = hackleAppContext
        return getLong(key, defaultValue)
    }

    internal fun getDouble(key: String, defaultValue: Double, hackleAppContext: HackleAppContext): Double {
        this.hackleAppContext = hackleAppContext
        return getDouble(key, defaultValue)
    }

    internal fun getBoolean(key: String, defaultValue: Boolean, hackleAppContext: HackleAppContext): Boolean {
        this.hackleAppContext = hackleAppContext
        return getBoolean(key, defaultValue)
    }

    internal fun <T : Any> get(
        key: String,
        requiredType: ValueType,
        defaultValue: T
    ): RemoteConfigDecision<T> {
        val sample = Timer.start()
        return try {
            val hackleUser = userManager.resolve(user, hackleAppContext)
            core.remoteConfig(key, hackleUser, requiredType, defaultValue)
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
