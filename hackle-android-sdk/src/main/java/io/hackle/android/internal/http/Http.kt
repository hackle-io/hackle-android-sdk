package io.hackle.android.internal.http

import io.hackle.android.internal.utils.parseJson
import okhttp3.Response
import okhttp3.ResponseBody
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED

internal val Response.isNotModified: Boolean
    get() {
        val networkResponse = networkResponse() ?: return false
        return networkResponse.code() == HTTP_NOT_MODIFIED
    }

internal inline fun <reified T> ResponseBody.parse(): T = string().parseJson()

internal const val HEADER_LAST_MODIFIED = "Last-Modified"
