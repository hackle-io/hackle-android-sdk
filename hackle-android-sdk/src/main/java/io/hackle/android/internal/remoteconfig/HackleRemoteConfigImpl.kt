package io.hackle.android.internal.remoteconfig

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.core.model.ValueType

internal class HackleRemoteConfigImpl(
    private val hackleAppCore: HackleAppCore,
) : HackleRemoteConfig {

    override fun getString(key: String, defaultValue: String): String {
        val decision = hackleAppCore.remoteConfig(key, ValueType.STRING, defaultValue, HackleAppContext.DEFAULT)
        return decision.value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        val decision = hackleAppCore.remoteConfig<Number>(key, ValueType.NUMBER, defaultValue, HackleAppContext.DEFAULT)
        return decision.value.toInt()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        val decision = hackleAppCore.remoteConfig<Number>(key, ValueType.NUMBER, defaultValue, HackleAppContext.DEFAULT)
        return decision.value.toLong()
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        val decision = hackleAppCore.remoteConfig<Number>(key, ValueType.NUMBER, defaultValue, HackleAppContext.DEFAULT)
        return decision.value.toDouble()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val decision = hackleAppCore.remoteConfig(key, ValueType.BOOLEAN, defaultValue, HackleAppContext.DEFAULT)
        return decision.value
    }
}
