package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.http.parse
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.android.internal.task.Task
import io.hackle.android.internal.utils.json.toJson
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateRequestDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateResponseDto
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

internal class WorkspaceRemoteEvaluateClient(
    sdkUri: String,
    private val httpClient: OkHttpClient,
) {

    private val evaluateEndpoint = (sdkUri + EVALUATE_PATH).toHttpUrl()

    fun evaluate(request: WorkspaceEvaluateRequestDto): Task<WorkspaceEvaluateResponseDto> {
        val requestBody = request.toJson().toRequestBody(CONTENT_TYPE)
        val httpRequest = Request.Builder()
            .url(evaluateEndpoint)
            .post(requestBody)
            .build()
        return Task.async { execute(httpRequest) }
            .map { response -> response.use { handleResponse(it) } }
    }

    private fun execute(request: Request): Response {
        return ApiCallMetrics.record("workspace.remote.evaluate") {
            httpClient.newCall(request).execute()
        }
    }

    private fun handleResponse(response: Response): WorkspaceEvaluateResponseDto {
        check(response.isSuccessful) { "Http status code: ${response.code}" }
        val responseBody = checkNotNull(response.body) { "Response body is null" }
        return responseBody.parse<WorkspaceEvaluateResponseDto>()
    }

    companion object {
        private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val EVALUATE_PATH = "/api/v1/evaluate"
    }
}
