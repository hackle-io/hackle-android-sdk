package io.hackle.android.internal.http

import io.hackle.android.internal.utils.json.parseJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.net.HttpURLConnection.HTTP_NO_CONTENT

internal fun Response.isStatusCode(code: Int): Boolean {
    val networkResponse = networkResponse ?: return false
    return networkResponse.code == code
}

internal val Response.isNoContent: Boolean get() = isStatusCode(HTTP_NO_CONTENT)
internal val Response.isNotModified: Boolean get() = isStatusCode(HTTP_NOT_MODIFIED)

internal inline fun <reified T> ResponseBody.parse(): T = string().parseJson()

internal const val HEADER_LAST_MODIFIED = "Last-Modified"
internal const val HEADER_IF_MODIFIED_SINCE = "If-Modified-Since"

internal val CONTENT_TYPE_APPLICATION_JSON = "application/json; charset=utf-8".toMediaType()
