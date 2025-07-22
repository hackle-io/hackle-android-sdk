package io.hackle.android.internal.http

import io.hackle.android.internal.utils.json.parseJson
import okhttp3.MediaType
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
internal const val HEADER_IF_MODIFIED_SINCE = "If-Modified-Since"

internal val CONTENT_TYPE_APPLICATION_JSON = MediaType.get("application/json; charset=utf-8")
