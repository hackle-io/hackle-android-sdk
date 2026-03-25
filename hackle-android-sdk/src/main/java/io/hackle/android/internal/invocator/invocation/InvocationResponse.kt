package io.hackle.android.internal.invocator.invocation

import io.hackle.android.internal.utils.json.toJson

internal class InvocationResponse<out T> private constructor(
    val isSuccess: Boolean,
    val message: String,
    val data: T?,
) {

    fun toJsonString(): String {
        val map = mapOf(
            "success" to isSuccess,
            "message" to message,
            "data" to data
        )
        return map.toJson()
    }

    companion object {

        private val SUCCESS = InvocationResponse<Any>(isSuccess = true, message = "OK", data = null)

        fun <T> success(): InvocationResponse<T> {
            @Suppress("UNCHECKED_CAST")
            return SUCCESS as InvocationResponse<T>
        }

        fun <T> success(data: T): InvocationResponse<T> {
            return InvocationResponse(isSuccess = true, message = "OK", data = data)
        }

        fun failed(e: Throwable): InvocationResponse<Any> {
            return InvocationResponse(isSuccess = false, message = e.message ?: "FAIL", data = null)
        }
    }
}
