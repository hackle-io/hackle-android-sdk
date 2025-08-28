package io.hackle.android.internal.invocator.model

import io.hackle.android.internal.utils.json.toJson

internal class InvokeResponse private constructor(
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

    companion object Companion {

        private val SUCCESS = InvokeResponse(success = true, message = "OK")

        fun success(): InvokeResponse {
            return SUCCESS
        }

        fun success(data: Any? = null): InvokeResponse {
            if (data == null) {
                return success()
            }
            return InvokeResponse(
                success = true,
                message = "OK",
                data = data
            )
        }

        fun error(message: String): InvokeResponse {
            return InvokeResponse(success = false, message = message)
        }

        fun error(throwable: Throwable): InvokeResponse {
            return InvokeResponse(success = false, message = throwable.message ?: "FAIL")
        }
    }
}