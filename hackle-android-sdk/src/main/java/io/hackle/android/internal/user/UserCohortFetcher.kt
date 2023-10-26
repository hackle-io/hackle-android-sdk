package io.hackle.android.internal.user

import android.util.Base64
import android.util.Base64.NO_WRAP
import android.util.Base64.URL_SAFE
import io.hackle.android.internal.http.parse
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.common.User
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.text.Charsets.UTF_8

internal class UserCohortFetcher(
    sdkUri: String,
    private val httpClient: OkHttpClient,
) {
    private val url = HttpUrl.get(url(sdkUri))

    fun fetch(user: User): UserCohorts {
        val request = createRequest(user)
        val response = execute(request)
        return response.use { handleResponse(it) }
    }

    private fun createRequest(user: User): Request {
        val body = UserCohortsRequestDto(user.resolvedIdentifiers.asMap()).encodeBase64Url()
        return Request.Builder()
            .get()
            .url(url)
            .header("X-HACKLE-USER", body)
            .build()
    }

    private fun execute(request: Request): Response {
        return ApiCallMetrics.record("get.cohorts") {
            httpClient.newCall(request).execute()
        }
    }

    private fun handleResponse(response: Response): UserCohorts {
        check(response.isSuccessful) { "Http status code: ${response.code()}" }
        val responseBody = checkNotNull(response.body()) { "Response body is null" }
        val dto = responseBody.parse<UserCohortsResponseDto>()
        return UserCohorts.Companion.from(dto)
    }

    companion object {
        private fun url(sdkUri: String): String {
            return "$sdkUri/api/v1/cohorts"
        }
    }
}

internal class UserCohortsRequestDto(
    val identifiers: Map<String, String>
)

internal fun UserCohortsRequestDto.encodeBase64Url(): String {
    return Base64.encodeToString(toJson().toByteArray(UTF_8), URL_SAFE or NO_WRAP)
}

internal class UserCohortsResponseDto(
    val cohorts: List<UserCohortDto>
)

internal class UserCohortDto(
    val identifier: IdentifierDto,
    val cohorts: List<Long>
)

internal class IdentifierDto(
    val type: String,
    val value: String
)

