package io.hackle.android.internal.event

import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ExecutorService

internal class EventDispatcher(
    baseEventUri: String,
    private val executor: ExecutorService,
    private val httpClient: OkHttpClient
) {

    private val dispatchEndpoint = (baseEventUri + EVENT_DISPATCH_PATH).toHttpUrl()

    fun dispatch(events: List<UserEvent>) {
        try {
            executor.submit(EventDispatchTask(events))
        } catch (e: Exception) {
            log.error { "Failed to dispatch event: $e" }
        }
    }

    private inner class EventDispatchTask(private val events: List<UserEvent>) : Runnable {
        override fun run() {
            try {
                val requestBody = events.toPayload().toJson().toRequestBody(CONTENT_TYPE)
                val request = Request.Builder()
                    .url(dispatchEndpoint)
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "Http status code: ${response.code}" }
                }
            } catch (e: Exception) {
                log.error { "Failed to dispatch event: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<EventDispatcher>()
        private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val EVENT_DISPATCH_PATH = "/api/v1/events"
    }
}