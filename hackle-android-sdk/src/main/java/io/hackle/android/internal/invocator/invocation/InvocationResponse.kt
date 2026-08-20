package io.hackle.android.internal.invocator.invocation

import io.hackle.android.internal.utils.json.toJson
import java.util.concurrent.CompletableFuture

internal class InvocationResponse<out T> private constructor(
    val isSuccess: Boolean,
    val message: String,
    val data: T?,
    /**
     * 비동기 처리의 완료 신호. user mutation 명령만 값을 가지며 직렬화되지 않는다.
     */
    val completion: CompletableFuture<Void>?,
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

        private val SUCCESS = InvocationResponse<Any>(isSuccess = true, message = "OK", data = null, completion = null)

        fun <T> success(): InvocationResponse<T> {
            @Suppress("UNCHECKED_CAST")
            return SUCCESS as InvocationResponse<T>
        }

        fun <T> success(data: T): InvocationResponse<T> {
            return InvocationResponse(isSuccess = true, message = "OK", data = data, completion = null)
        }

        fun <T> success(completion: CompletableFuture<Void>): InvocationResponse<T> {
            return InvocationResponse(isSuccess = true, message = "OK", data = null, completion = completion)
        }

        fun failed(e: Throwable): InvocationResponse<Any> {
            return InvocationResponse(isSuccess = false, message = e.message ?: "FAIL", data = null, completion = null)
        }
    }
}
