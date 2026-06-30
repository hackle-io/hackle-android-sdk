package io.hackle.android.internal.workspace.config

import io.hackle.android.internal.http.HEADER_IF_MODIFIED_SINCE
import io.hackle.android.internal.http.HEADER_LAST_MODIFIED
import io.hackle.android.internal.http.isNotModified
import io.hackle.android.internal.http.parse
import io.hackle.android.internal.model.Sdk
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.internal.task.future
import io.hackle.android.internal.task.map
import io.hackle.sdk.core.internal.log.Logger
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.CompletableFuture

internal class HttpWorkspaceConfigFetcher(
    sdk: Sdk,
    sdkUri: String,
    private val httpClient: OkHttpClient,
) {
    private val url = url(sdk, sdkUri).toHttpUrl()

    fun fetchIfModified(lastModified: String?): CompletableFuture<WorkspaceConfigRecord?> {
        val request = createRequest(lastModified)
        return TaskExecutors.future { execute(request) }
            .map { response -> response.use { handleResponse(it) } }
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

    private fun handleResponse(response: Response): WorkspaceConfigRecord? {
        if (response.isNotModified) {
            log.debug { "Workspace is not modified." }
            return null
        }
        check(response.isSuccessful) { "Http status code: ${response.code}" }
        val lastModified = response.header(HEADER_LAST_MODIFIED)
        val responseBody = checkNotNull(response.body) { "Response body is null" }
        val dto = responseBody.parse<WorkspaceConfigDto>()

        log.debug { "Workspace fetched." }
        return WorkspaceConfigRecord.of(dto, lastModified)
    }

    companion object {
        private val log = Logger<HttpWorkspaceConfigFetcher>()

        private fun url(sdk: Sdk, sdkUri: String): String {
            return "$sdkUri/api/v2/workspaces/${sdk.key}/config"
        }
    }
}
