package io.hackle.android.internal.http

import android.net.Uri
import io.hackle.android.internal.utils.parseJson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.ResponseBody

internal inline fun <reified T> ResponseBody.parse(): T = string().parseJson()

internal operator fun Uri.plus(path: String): Uri = buildUpon().appendPath(path).build()
internal val Response.isNotSuccessful get() = !isSuccessful

internal fun Call.executeAsync(callback: Callback) = enqueue(callback)
