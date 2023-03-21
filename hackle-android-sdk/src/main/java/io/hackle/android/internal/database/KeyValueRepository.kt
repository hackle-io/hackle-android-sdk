package io.hackle.android.internal.database

import android.content.Context
import android.content.SharedPreferences

internal interface KeyValueRepository {

    fun getAll(): Map<String, Any>

    fun getString(key: String): String?

    fun putString(key: String, value: String)

    fun getString(key: String, mapping: (String) -> String): String {
        return getString(key) ?: mapping(key).also { putString(key, it) }
    }

    fun getLong(key: String, defaultValue: Long): Long

    fun putLong(key: String, value: Long)

    fun remove(key: String)

    fun clear()
}

internal class AndroidKeyValueRepository(
    private val preferences: SharedPreferences,
) : KeyValueRepository {

    override fun getAll(): Map<String, Any> {
        @Suppress("UNCHECKED_CAST")
        return preferences.all as Map<String, Any>
    }

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

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        fun create(
            context: Context,
            name: String,
            mode: Int = Context.MODE_PRIVATE,
        ): AndroidKeyValueRepository {
            val preferences = context.getSharedPreferences(name, mode)
            return AndroidKeyValueRepository(preferences)
        }
    }
}
