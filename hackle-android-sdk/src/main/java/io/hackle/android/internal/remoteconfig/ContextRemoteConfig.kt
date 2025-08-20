package io.hackle.android.internal.remoteconfig

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.ValueType

internal class ContextRemoteConfig(
    private val remoteConfigProcessor: RemoteConfigProcessor,
    private val user: User?,
    private val hackleAppContext: HackleAppContext
) : HackleRemoteConfig {
    override fun getString(key: String, defaultValue: String): String {
        return remoteConfigProcessor.get(key, ValueType.STRING, defaultValue, user, hackleAppContext).value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return remoteConfigProcessor.get<Number>(key, ValueType.NUMBER, defaultValue, user, hackleAppContext).value.toInt()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return remoteConfigProcessor.get<Number>(key, ValueType.NUMBER, defaultValue, user, hackleAppContext).value.toLong()
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return remoteConfigProcessor.get<Number>(key, ValueType.NUMBER, defaultValue, user, hackleAppContext).value.toDouble()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return remoteConfigProcessor.get(key, ValueType.BOOLEAN, defaultValue, user, hackleAppContext).value
    }
}
