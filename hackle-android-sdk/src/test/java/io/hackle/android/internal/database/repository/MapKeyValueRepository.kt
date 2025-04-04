package io.hackle.android.internal.database.repository

internal class MapKeyValueRepository(
    private val map: MutableMap<String, Any> = mutableMapOf(),
) : KeyValueRepository {
    override fun getAll(): Map<String, Any> {
        return map
    }

    override fun getString(key: String): String? {
        return map[key] as? String
    }

    override fun putString(key: String, value: String) {
        map[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return map[key] as? Long ?: defaultValue
    }

    override fun putLong(key: String, value: Long) {
        map[key] = value
    }

    override fun remove(key: String) {
        map.remove(key)
    }

    override fun clear() {
        map.clear()
    }

}