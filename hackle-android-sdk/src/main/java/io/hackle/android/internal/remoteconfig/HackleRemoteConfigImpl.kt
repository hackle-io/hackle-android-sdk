package io.hackle.android.internal.remoteconfig

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.ValueType

internal class HackleRemoteConfigImpl(
    private val hackleAppCore: HackleAppCore,
    private val user: User?
) : HackleRemoteConfig {

    override fun getString(key: String, defaultValue: String): String {
        return hackleAppCore.remoteConfig(key, ValueType.STRING, defaultValue, user, HackleAppContext.DEFAULT).value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return hackleAppCore.remoteConfig<Number>(
            key,
            ValueType.NUMBER,
            defaultValue,
            user,
            HackleAppContext.DEFAULT
        ).value.toInt()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return hackleAppCore.remoteConfig<Number>(
            key,
            ValueType.NUMBER,
            defaultValue,
            user,
            HackleAppContext.DEFAULT
        ).value.toLong()
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return hackleAppCore.remoteConfig<Number>(
            key,
            ValueType.NUMBER,
            defaultValue,
            user,
            HackleAppContext.DEFAULT
        ).value.toDouble()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return hackleAppCore.remoteConfig(key, ValueType.BOOLEAN, defaultValue, user, HackleAppContext.DEFAULT).value
    }
}
