package io.hackle.android.internal.remoteconfig

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.model.ValueType

internal class ContextRemoteConfig(
    user: User?,
    core: HackleCore,
    userManager: UserManager,
    private val hackleAppContext: HackleAppContext
) : RemoteConfigCore(user, core, userManager), HackleRemoteConfig {
    override fun getString(key: String, defaultValue: String): String {
        return get(key, ValueType.STRING, defaultValue, hackleAppContext).value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return get<Number>(key, ValueType.NUMBER, defaultValue, hackleAppContext).value.toInt()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return get<Number>(key, ValueType.NUMBER, defaultValue, hackleAppContext).value.toLong()
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return get<Number>(key, ValueType.NUMBER, defaultValue, hackleAppContext).value.toDouble()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return get(key, ValueType.BOOLEAN, defaultValue, hackleAppContext).value
    }
}
