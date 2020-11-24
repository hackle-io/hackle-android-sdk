package io.hackle.android.internal.http

import io.hackle.android.internal.utils.parseJson
import okhttp3.ResponseBody

internal inline fun <reified T> ResponseBody.parse(): T = string().parseJson()
