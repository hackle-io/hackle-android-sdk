package io.hackle.android.internal.workspace

import io.hackle.android.internal.http.HEADER_IF_MODIFIED_SINCE
import io.hackle.android.internal.http.HEADER_LAST_MODIFIED
import io.hackle.android.internal.http.isNotModified
import io.hackle.android.internal.http.parse
import io.hackle.android.internal.model.Sdk
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.sdk.core.internal.log.Logger
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

    fun fetchIfModified(lastModified: String? = null): WorkspaceConfig? {
        val request = createRequest(lastModified)
        val response = execute(request)
        return response.use { handleResponse(it) }
    }

    private fun createRequest(lastModified: String?): Request {
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

    private fun handleResponse(response: Response): WorkspaceConfig? {
        if (response.isNotModified) {
            log.debug { "Workspace is not modified." }
            return null
        }
        check(response.isSuccessful) { "Http status code: ${response.code()}" }
        val lastModified = response.header(HEADER_LAST_MODIFIED)
        val responseBody = checkNotNull(response.body()) { "Response body is null" }
        val dto = responseBody.parse<WorkspaceConfigDto>()

        log.debug { "Workspace fetched." }
        return WorkspaceConfig(
            lastModified = lastModified,
            config = dto,
        )
    }

    companion object {
        private val log = Logger<HttpWorkspaceFetcher>()

        private fun url(sdk: Sdk, sdkUri: String): String {
            return "$sdkUri/api/v2/workspaces/${sdk.key}/config"
        }
    }
}
