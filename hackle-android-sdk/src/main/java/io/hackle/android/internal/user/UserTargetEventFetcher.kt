package io.hackle.android.internal.user

import io.hackle.android.internal.http.parse
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.sdk.common.User
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * UserTargetEventFetcher
 * @property sdkUri SDK URI
 * @property httpClient OkHttpClient
 */
internal class UserTargetEventFetcher(
    sdkUri: String,
    private val httpClient: OkHttpClient,
) {
    private val url = HttpUrl.get(url(sdkUri))

    /**
     * 사용자의 타겟팅 정보를 가져온다.
     * @param user 사용자 정보
     */
    fun fetch(user: User): UserTargetEvents {
        val request = createRequest(user)
        val response = execute(request)
        return response.use { handleResponse(it) }
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
        check(response.isSuccessful) { "Http status code: ${response.code()}" }
        val responseBody = checkNotNull(response.body()) { "Response body is null" }
        val dto = responseBody.parse<UserTargetResponseDto>()
        return UserTargetEvents.Companion.from(dto)
    }

    companion object {
        private fun url(sdkUri: String): String {
            return "$sdkUri/api/v1/user-targets"
        }
    }
}

