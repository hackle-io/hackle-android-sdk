package io.hackle.android.internal.utils

import android.content.SharedPreferences

internal inline fun SharedPreferences.computeIfAbsent(key: String, mapping: (String) -> String): String {
    return getString(key, null) ?: mapping(key).also { edit().putString(key, it).apply() }
}