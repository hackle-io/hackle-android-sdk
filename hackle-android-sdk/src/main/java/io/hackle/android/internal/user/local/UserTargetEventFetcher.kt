package io.hackle.android.internal.user.local

import io.hackle.android.internal.http.parse
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.android.internal.task.Futures
import io.hackle.android.internal.task.map
import io.hackle.android.internal.user.resolvedIdentifiers
import io.hackle.sdk.common.User
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

internal class UserTargetEventFetcher(
    sdkUri: String,
    private val executor: Executor,
    private val httpClient: OkHttpClient,
) {
    private val url = url(sdkUri).toHttpUrl()

    fun fetch(user: User): CompletableFuture<UserTargetEvents> {
        val request = createRequest(user)
        return Futures.async(executor) { execute(request) }
            .map { response -> response.use { handleResponse(it) } }
    }

    private fun createRequest(user: User): Request {
        val userHeader = UserTargetRequestDto(user.resolvedIdentifiers.asMap()).encodeBase64Url()
        return Request.Builder()
            .get()
            .url(url)
            .header("X-HACKLE-USER", userHeader)
            .build()
    }

    private fun execute(request: Request): Response {
        return ApiCallMetrics.record("get.user-targets") {
            httpClient.newCall(request).execute()
        }
    }

    private fun handleResponse(response: Response): UserTargetEvents {
        check(response.isSuccessful) { "Http status code: ${response.code}" }
        val responseBody = checkNotNull(response.body) { "Response body is null" }
        val dto = responseBody.parse<UserTargetResponseDto>()
        return UserTargetEvents.from(dto)
    }

    companion object {
        private fun url(sdkUri: String): String {
            return "$sdkUri/api/v1/user-targets"
        }
    }
}

