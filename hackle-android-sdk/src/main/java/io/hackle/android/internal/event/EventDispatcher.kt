package io.hackle.android.internal.event

import io.hackle.android.internal.database.workspace.EventEntity
import io.hackle.android.internal.database.workspace.EventEntity.Status.PENDING
import io.hackle.android.internal.database.repository.EventRepository
import io.hackle.android.internal.monitoring.metric.ApiCallMetrics
import io.hackle.sdk.core.internal.log.Logger
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.util.concurrent.Executor

internal class EventDispatcher(
    baseEventUri: String,
    private val eventExecutor: Executor,
    private val eventRepository: EventRepository,
    private val httpExecutor: Executor,
    private val httpClient: OkHttpClient,
    private val eventBackoffController: UserEventBackoffController,
) {
    private val dispatchEndpoint = HttpUrl.get(baseEventUri + EVENT_DISPATCH_PATH)

    fun dispatch(events: List<EventEntity>) {
        try {
            httpExecutor.execute(EventDispatchTask(events))
        } catch (e: Exception) {
            log.error { "Failed to submit EventDispatchTask: $e" }
            updateEventStatusToPending(events)
        }
    }

    private fun delete(events: List<EventEntity>) {
        try {
            eventExecutor.execute(DeleteEventTask(events))
        } catch (e: Exception) {
            log.error { "Failed to submit DeleteEventTask: $e" }
        }
    }

    private fun updateEventStatusToPending(events: List<EventEntity>) {
        try {
            eventExecutor.execute(UpdateEventToPendingTask(events))
        } catch (e: Exception) {
            log.error { "Failed to submit UpdateEventTask: $e" }
        }
    }

    inner class EventDispatchTask(private val events: List<EventEntity>) : Runnable {
        override fun run() {
            try {
                dispatch()
            } catch (e: Exception) {
                log.error { "Failed to dispatch events: $e" }
                updateEventStatusToPending(events)
            }
        }

        private fun dispatch() {
            val requestBody = RequestBody.create(CONTENT_TYPE, events.toBody())
            val request = Request.Builder()
                .url(dispatchEndpoint)
                .post(requestBody)
                .build()
            var isSuccess = false
            try {
                val response = ApiCallMetrics.record("post.events") {
                    httpClient.newCall(request).execute()
                }
                response.use {
                    isSuccess = handleResponse(it)
                }
            } catch (e: Exception) {
                throw e
            } finally {
                eventBackoffController.checkResponse(isSuccess)
            }
        }

        private fun handleResponse(response: Response): Boolean {
            when (response.code()) {
                in 200..299 -> delete(events)
                in 400..499 -> delete(events)
                else -> throw IllegalStateException("Http status code: ${response.code()}")
            }
            return response.isSuccessful
        }
    }

    inner class DeleteEventTask(private val events: List<EventEntity>) : Runnable {
        override fun run() {
            try {
                eventRepository.delete(events)
            } catch (e: Exception) {
                log.error { "Failed to delete events: $e" }
            }
        }
    }

    inner class UpdateEventToPendingTask(private val events: List<EventEntity>) : Runnable {
        override fun run() {
            try {
                eventRepository.update(events, PENDING)
            } catch (e: Exception) {
                log.error { "Failed to update events: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<EventDispatcher>()
        private val CONTENT_TYPE = MediaType.get("application/json; charset=utf-8")
        private const val EVENT_DISPATCH_PATH = "/api/v2/events"
    }
}
