package io.hackle.android.internal.event

import io.hackle.android.internal.http.executeAsync
import io.hackle.android.internal.http.isNotSuccessful
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

internal class EventDispatcher(
    baseEventUri: String,
    private val httpClient: OkHttpClient
) {

    private val dispatchEndpoint = (baseEventUri + EVENT_DISPATCH_PATH).toHttpUrl()

    fun dispatch(events: List<UserEvent>) {

        val requestBody = events.toPayload().toJson().toRequestBody(CONTENT_TYPE)
        val request = Request.Builder()
            .url(dispatchEndpoint)
            .post(requestBody)
            .build()

        httpClient.newCall(request).executeAsync(LoggingCallback)
    }


    private object LoggingCallback : Callback {
        override fun onFailure(call: Call, e: IOException) {
            log.error { "Failed to dispatch events: $e" }
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isNotSuccessful) {
                log.error { "Failed to dispatch events. Http status code: ${response.code}" }
            }
        }
    }

    companion object {
        private val log = Logger<EventDispatcher>()
        private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()

        private const val EVENT_DISPATCH_PATH = "/api/v1/events"
    }
}