package io.hackle.android.internal.devtools

import com.google.gson.annotations.SerializedName
import io.hackle.android.internal.invocator.model.UserDto
import io.hackle.android.internal.http.CONTENT_TYPE_APPLICATION_JSON
import io.hackle.android.internal.model.Sdk
import io.hackle.android.internal.utils.json.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

internal class DevToolsApi(
    private val sdk: Sdk,
    private val url: String,
    private val httpClient: OkHttpClient
) {

    fun addExperimentOverrides(experimentKey: Long, requestDto: OverrideRequestDto) {
        execute("PATCH", "/v1/experiments/${experimentKey}/overrides", requestDto)
    }

    fun removeExperimentOverrides(experimentKey: Long, requestDto: OverrideRequestDto) {
        execute("DELETE", "/v1/experiments/${experimentKey}/overrides", requestDto)
    }

    fun removeAllExperimentOverrides(requestDto: OverrideRequestDto) {
        execute("DELETE", "/v1/experiments/overrides", requestDto)
    }

    fun addFeatureFlagOverrides(experimentKey: Long, requestDto: OverrideRequestDto) {
        execute("PATCH", "/v1/feature-flags/${experimentKey}/overrides", requestDto)
    }

    fun removeFeatureFlagOverrides(experimentKey: Long, requestDto: OverrideRequestDto) {
        execute("DELETE", "/v1/feature-flags/${experimentKey}/overrides", requestDto)
    }

    fun removeAllFeatureFlagOverrides(requestDto: OverrideRequestDto) {
        execute("DELETE", "/v1/feature-flags/overrides", requestDto)
    }

    private fun execute(method: String, path: String, requestBody: Any) {
        val request = Request.Builder()
            .url("$url$path")
            .method(method, RequestBody.create(CONTENT_TYPE_APPLICATION_JSON, requestBody.toJson()))
            .header("X-HACKLE-API-KEY", sdk.key)
            .build()
        val response = httpClient.newCall(request).execute()
        response.use { require(it.isSuccessful) { "[${it.code()}] $method $path" } }
    }
}

internal class OverrideRequestDto(
    @SerializedName("user")
    val user: UserDto,
    @SerializedName("variation")
    val variation: String?
)
