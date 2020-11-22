package io.hackle.android.internal.http

import okhttp3.Interceptor
import okhttp3.Response

internal class SdkHeaderInterceptor(
    private val sdkKey: String,
    private val sdkName: String,
    private val sdkVersion: String
) : Interceptor {

    private val userAgent = "$sdkName/$sdkVersion"

    override fun intercept(chain: Interceptor.Chain): Response {

        val newRequest = chain.request().newBuilder()
            .addHeader(SDK_KEY_HEADER, sdkKey)
            .addHeader(SDK_NAME_HEADER, sdkName)
            .addHeader(SDK_VERSION_HEADER, sdkVersion)
            .addHeader(USER_AGENT_HEADER, userAgent)
            .build()

        return chain.proceed(newRequest)
    }

    companion object {
        private const val SDK_KEY_HEADER = "X-HACKLE-SDK-KEY"
        private const val SDK_NAME_HEADER = "X-HACKLE-SDK-NAME"
        private const val SDK_VERSION_HEADER = "X-HACKLE-SDK-VERSION"
        private const val USER_AGENT_HEADER = "User-Agent"
    }
}