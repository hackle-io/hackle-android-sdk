package io.hackle.android.internal.workspace

import io.hackle.android.internal.http.parse
import io.hackle.sdk.core.workspace.Workspace
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal class HttpWorkspaceFetcher(
    baseSdkUri: String,
    private val httpClient: OkHttpClient
) {

    private val endpoint = (baseSdkUri + WORKSPACE_FETCH_PATH).toHttpUrl()

    fun fetch(): Workspace {
        val request = Request.Builder()
            .url(endpoint)
            .build()
        return httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Http status code: ${response.code}" }
            val responseBody = checkNotNull(response.body) { "Response body is null" }
            val dto = responseBody.parse<WorkspaceDto>()
            WorkspaceImpl.from(dto)
        }
    }

    companion object {
        private const val WORKSPACE_FETCH_PATH = "/api/v1/workspaces"
    }
}