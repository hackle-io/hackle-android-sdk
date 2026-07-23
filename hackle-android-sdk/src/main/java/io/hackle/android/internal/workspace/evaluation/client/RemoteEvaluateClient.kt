package io.hackle.android.internal.workspace.evaluation.client

import io.hackle.android.internal.http.isNoContent
import io.hackle.android.internal.http.parse
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.android.internal.task.Futures
import io.hackle.android.internal.task.map
import io.hackle.android.internal.utils.json.toJson
import io.hackle.android.internal.workspace.evaluation.model.EntityEvaluateRequestDto
import io.hackle.android.internal.workspace.evaluation.model.EntityEvaluateResponseDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateRequestDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateResponseDto
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

internal class RemoteEvaluateClient(
    sdkUri: String,
    private val executor: Executor,
    private val httpClient: OkHttpClient,
) {

    private val workspaceEndpoint = (sdkUri + WORKSPACE_EVALUATE_PATH).toHttpUrl()
    private val entityEndpoint = (sdkUri + TARGET_EVALUATE_PATH).toHttpUrl()

    fun evaluateIfModified(requestDto: WorkspaceEvaluateRequestDto): CompletableFuture<WorkspaceEvaluateResponseDto?> {
        val request = createRequest(workspaceEndpoint, requestDto.toJson())
        return Futures.async(executor) { execute(request, "workspace.remote.evaluate") }
            .map { response -> response.use { handleWorkspaceResponse(it) } }
    }

    fun evaluateEntities(requestDto: EntityEvaluateRequestDto): CompletableFuture<EntityEvaluateResponseDto> {
        val request = createRequest(entityEndpoint, requestDto.toJson())
        return Futures.async(executor) { execute(request, "entity.remote.evaluate") }
            .map { response -> response.use { handleResponse<EntityEvaluateResponseDto>(it) } }
    }

    private fun createRequest(url: HttpUrl, body: String): Request {
        return Request.Builder()
            .url(url)
            .post(body.toRequestBody(CONTENT_TYPE))
            .build()
    }

    private fun execute(request: Request, operation: String): Response {
        return ApiCallMetrics.record(operation) {
            httpClient.newCall(request).execute()
        }
    }

    private fun handleWorkspaceResponse(response: Response): WorkspaceEvaluateResponseDto? {
        if (response.isNoContent) {
            return null
        }
        return handleResponse<WorkspaceEvaluateResponseDto>(response)
    }

    private inline fun <reified T> handleResponse(response: Response): T {
        check(response.isSuccessful) { "Http status code: ${response.code}" }
        val responseBody = checkNotNull(response.body) { "Response body is null" }
        return responseBody.parse<T>()
    }

    companion object {
        private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val WORKSPACE_EVALUATE_PATH = "/api/v1/workspace-evaluate"
        private const val TARGET_EVALUATE_PATH = "/api/v1/entity-evaluate"
    }
}
