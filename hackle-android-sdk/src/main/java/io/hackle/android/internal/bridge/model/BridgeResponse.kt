package io.hackle.android.internal.bridge.model

import io.hackle.android.internal.utils.toJson

internal class BridgeResponse private constructor(
    val success: Boolean,
    val message: String,
    val data: Any? = null
) {

    fun toJsonString(): String {
        val map = mapOf(
            "success" to success,
            "message" to message,
            "data" to data
        )
        return map.toJson()
    }

    companion object {

        private val SUCCESS = BridgeResponse(success = true, message = "OK")

        fun success(): BridgeResponse {
            return SUCCESS
        }

        fun success(data: Any? = null): BridgeResponse {
            if (data == null) {
                return success()
            }
            return BridgeResponse(
                success = true,
                message = "OK",
                data = data
            )
        }

        fun error(message: String): BridgeResponse {
            return BridgeResponse(success = false, message = message)
        }

        fun error(throwable: Throwable): BridgeResponse {
            return BridgeResponse(success = false, message = throwable.message ?: "FAIL")
        }
    }
}