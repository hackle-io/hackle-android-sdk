package io.hackle.android.internal.http

import io.hackle.sdk.core.internal.log.Logger
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

internal class LoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = maskSdkKey(request.url)
        log.debug { "--> ${request.method} $url" }
        return try {
            val response = chain.proceed(request)
            log.debug { "<-- ${request.method} $url status: ${response.code}" }
            response
        } catch (e: Exception) {
            log.debug { "<-- ${request.method} $url status: $UNKNOWN_STATUS_CODE, error: $e" }
            throw e
        }
    }

    private fun maskSdkKey(url: HttpUrl): String {
        val segments = url.pathSegments
        val index = segments.indexOf(WORKSPACES_PATH_SEGMENT)
        if (index < 0 || index + 1 >= segments.size) {
            return url.toString()
        }
        return url.newBuilder()
            .setPathSegment(index + 1, SDK_KEY_MASK)
            .toString()
    }

    companion object {
        private const val WORKSPACES_PATH_SEGMENT = "workspaces"
        private const val SDK_KEY_MASK = "****"
        private const val UNKNOWN_STATUS_CODE = -1
        private val log = Logger<LoggingInterceptor>()
    }
}
