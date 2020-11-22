package io.hackle.android.internal.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder

private val GSON: Gson = GsonBuilder().create()

internal inline fun <reified T> String.parseJson(): T = GSON.fromJson(this, T::class.java)
internal fun Any.toJson(): String = GSON.toJson(this)
