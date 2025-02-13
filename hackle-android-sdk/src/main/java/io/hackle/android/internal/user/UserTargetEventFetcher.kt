package io.hackle.android.internal.user

import android.util.Base64
import android.util.Base64.NO_WRAP
import android.util.Base64.URL_SAFE
import io.hackle.android.internal.http.parse
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.TargetEvent
import io.hackle.sdk.core.model.TargetEvent.Property
import io.hackle.sdk.core.model.TargetEvent.Stat
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.text.Charsets.UTF_8

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

internal class UserTargetRequestDto(
    val identifiers: Map<String, String>
)

internal fun UserTargetRequestDto.encodeBase64Url(): String {
    return Base64.encodeToString(toJson().toByteArray(UTF_8), URL_SAFE or NO_WRAP)
}

internal class UserTargetResponseDto(
    val events: List<TargetEventDto>
)

internal class TargetEventDto(
    val eventKey: String,
    val stats: List<StatDto>,
    val property: PropertyDto?
)

internal class StatDto(
    val date: Long,
    val count: Int
)

internal class PropertyDto(
    val key: String,
    val type: Target.Key.Type,
    val value: String
)
