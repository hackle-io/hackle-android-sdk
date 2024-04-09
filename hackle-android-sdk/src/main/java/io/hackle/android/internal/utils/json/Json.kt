package io.hackle.android.internal.utils.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

private val GSON: Gson = GsonBuilder().create()

internal inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, gsonTypeRef<T>().type)
internal inline fun <reified T> String.parseJson(): T = GSON.fromJson(this)
internal fun Any.toJson(): String = GSON.toJson(this)

internal inline fun <reified T> gsonTypeRef(): TypeToken<T> = object : TypeToken<T>() {}
