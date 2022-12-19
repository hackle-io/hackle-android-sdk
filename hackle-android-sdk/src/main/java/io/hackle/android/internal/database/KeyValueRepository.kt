package io.hackle.android.internal.database

import android.content.SharedPreferences

internal interface KeyValueRepository {

    fun getString(key: String): String?

    fun putString(key: String, value: String)

    fun getString(key: String, mapping: (String) -> String): String {
        return getString(key) ?: mapping(key).also { putString(key, it) }
    }

    fun getLong(key: String, defaultValue: Long): Long

    fun putLong(key: String, value: Long)
}

internal class AndroidKeyValueRepository(
    private val preferences: SharedPreferences,
) : KeyValueRepository {

    override fun getString(key: String): String? {
        return preferences.getString(key, null)
    }

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return preferences.getLong(key, defaultValue)
    }

    override fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }
}
