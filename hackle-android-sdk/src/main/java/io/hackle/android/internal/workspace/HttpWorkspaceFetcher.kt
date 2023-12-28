package io.hackle.android.internal.workspace

import io.hackle.android.internal.http.HEADER_IF_MODIFIED_SINCE
import io.hackle.android.internal.http.HEADER_LAST_MODIFIED
import io.hackle.android.internal.http.isNotModified
import io.hackle.android.internal.http.parse
import io.hackle.android.internal.model.Sdk
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.sdk.core.workspace.Workspace
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class HttpWorkspaceFetcher(
    sdk: Sdk,
    sdkUri: String,
    private val httpClient: OkHttpClient,
) {
    private val url = HttpUrl.get(url(sdk, sdkUri))
    private var lastModified: String? = null

    fun fetchIfModified(): Workspace? {
        val request = createRequest()
        val response = execute(request)
        return response.use { handleResponse(it) }
    }

    private fun createRequest(): Request {
        return Request.Builder()
            .url(url)
            .apply { lastModified?.let { header(HEADER_IF_MODIFIED_SINCE, it) } }
            .build()
    }

    private fun execute(request: Request): Response {
        return ApiCallMetrics.record("get.workspace") {
            httpClient.newCall(request).execute()
        }
    }

    private fun handleResponse(response: Response): Workspace? {
        if (response.isNotModified) {
            return null
        }
        check(response.isSuccessful) { "Http status code: ${response.code()}" }
        lastModified = response.header(HEADER_LAST_MODIFIED)
        val responseBody = checkNotNull(response.body()) { "Response body is null" }
        val dto = responseBody.parse<WorkspaceConfigDto>()
        return WorkspaceImpl.from(dto)
    }

    companion object {
        private fun url(sdk: Sdk, sdkUri: String): String {
            return "$sdkUri/api/v2/workspaces/${sdk.key}/config"
        }
    }
}
