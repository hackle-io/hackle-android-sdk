package io.hackle.android.internal.bridge.model

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import io.hackle.android.internal.utils.parseJson

internal class BridgeInvocation(string: String) {

    enum class Command {
        @SerializedName("getSessionId")
        GET_SESSION_ID,
        @SerializedName("getUser")
        GET_USER,
        @SerializedName("setUser")
        SET_USER,
        @SerializedName("setUserId")
        SET_USER_ID,
        @SerializedName("setDeviceId")
        SET_DEVICE_ID,
        @SerializedName("setUserProperty")
        SET_USER_PROPERTY,
        @SerializedName("updateUserProperties")
        UPDATE_USER_PROPERTY,
        @SerializedName("resetUser")
        RESET_USER,
        @SerializedName("variation")
        VARIATION,
        @SerializedName("variationDetail")
        VARIATION_DETAIL,
        @SerializedName("isFeatureOn")
        IS_FEATURE_ON,
        @SerializedName("featureFlagDetail")
        FEATURE_FLAG_DETAIL,
        @SerializedName("track")
        TRACK,
        @SerializedName("remoteConfig")
        REMOTE_CONFIG,
        @SerializedName("showUserExplorer")
        SHOW_USER_EXPLORER;
    }

    val command: Command
    private val parameters: JsonObject?

    init {
        val parser = JsonParser.parseString(string)
        if (!parser.asJsonObject.has(KEY_HACKLE)) {
            throw IllegalArgumentException("'$KEY_HACKLE' key must be provided.")
        }

        val root = parser.asJsonObject.getAsJsonObject(KEY_HACKLE)
        if (!root.has(KEY_COMMAND)) {
            throw IllegalArgumentException("'$KEY_COMMAND' key must be provided")
        }

        try {
            command = root[KEY_COMMAND].parseJson()
        } catch (_: Throwable) {
            throw IllegalArgumentException("Unsupported command string received.")
        }

        parameters = root[KEY_PARAMETERS]?.asJsonObject
    }

    fun hasParameter(key: String): Boolean {
        return parameters != null && parameters.has(key)
    }

    inline fun <reified T> getParameter(key: String): T? {
        if (parameters == null || !parameters.has(key)) {
            return null
        }
        return try { parameters.get(key).parseJson<T>() }
        catch (_: Throwable) { null }
    }

    inline fun <reified T> getParameterNotNull(key: String): T {
        return checkNotNull(getParameter<T>(key))
    }

    companion object {
        private const val KEY_HACKLE = "_hackle"
        private const val KEY_COMMAND = "command"
        private const val KEY_PARAMETERS = "parameters"
    }
}