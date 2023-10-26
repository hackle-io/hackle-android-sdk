package io.hackle.android.internal.bridge.model

import io.hackle.android.internal.utils.toJson

class BridgeResponse private constructor(
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

        fun success(): BridgeResponse {
            return BridgeResponse(success = true, message = "OK")
        }

        fun success(data: Boolean? = null): BridgeResponse {
            if (data == null) {
                return success()
            }
            return BridgeResponse(
                success = true,
                message = "OK",
                data = data
            )
        }

        fun success(data: String? = null): BridgeResponse {
            if (data == null) {
                return success()
            }
            return BridgeResponse(
                success = true,
                message = "OK",
                data = data
            )
        }

        fun success(data: Map<String, Any>? = null): BridgeResponse {
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
            return BridgeResponse(success = false, message = throwable.message ?: "")
        }
    }
}