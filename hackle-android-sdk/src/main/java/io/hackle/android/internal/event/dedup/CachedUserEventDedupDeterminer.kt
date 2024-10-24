package io.hackle.android.internal.event.dedup

import io.hackle.android.HackleConfig
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.event.dedup.CachedUserEventDedupDeterminer.Key
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.time.Clock

internal abstract class CachedUserEventDedupDeterminer<CACHE_KEY : Key, USER_EVENT : UserEvent>(
    private val repository: KeyValueRepository,
    private val dedupIntervalMillis: Long,
    private val clock: Clock,
) : UserEventDedupDeterminer {

    private var cache = HashMap<String, Long>()
    private var currentUserIdentifiers: Map<String, String>? = null
    private val repositoryKeyDedupCache = "DEDUP_CACHE"
    private val repositoryKeyCurrentUser = "USER_IDENTIFIERS"

    init {
        loadFromRepository()
    }

    override fun isDedupTarget(event: UserEvent): Boolean {

        if (dedupIntervalMillis == HackleConfig.USER_EVENT_NO_DEDUP.toLong()) {
            return false
        }

        if (event.user.identifiers != currentUserIdentifiers) {
            cache = hashMapOf()
            currentUserIdentifiers = event.user.identifiers
        }

        @Suppress("UNCHECKED_CAST")
        val key = cacheKey(event as USER_EVENT)
        val now = clock.currentMillis()

        val firstMillis = cache[key]
        if (firstMillis != null && now - firstMillis <= dedupIntervalMillis) {
            return true
        }

        cache[key] = now
        return false
    }

    abstract fun supports(event: UserEvent): Boolean

    abstract fun cacheKey(event: USER_EVENT): String

    interface Key {
        override fun equals(other: Any?): Boolean
        override fun hashCode(): Int
    }

    fun saveToRepository() {
        saveCurrentUserToRepository()
        saveCacheToRepository()
    }

    fun loadFromRepository() {
        loadCurrentUserFromRepository()
        loadCacheFromRepository()
    }

    private fun saveCurrentUserToRepository() {
        val identifiers = currentUserIdentifiers
        if (identifiers != null) {
            repository.putString(repositoryKeyCurrentUser, identifiers.toJson())
            return
        }

        repository.remove(repositoryKeyCurrentUser)
    }

    private fun loadCurrentUserFromRepository() {
        val identifiers: String? = repository.getString(repositoryKeyCurrentUser)
        if (identifiers != null) {
            currentUserIdentifiers = identifiers.parseJson()
            return
        }

        currentUserIdentifiers = null
    }

    private fun saveCacheToRepository() {
        repository.putString(repositoryKeyDedupCache, cache.toJson())
    }

    private fun loadCacheFromRepository() {
        val savedCacheStr: String? = repository.getString(repositoryKeyDedupCache)
        if (savedCacheStr != null) {
            cache = savedCacheStr.parseJson()
        }
        updateCacheForIntervalExpiry()
    }

    private fun updateCacheForIntervalExpiry() {
        val now = clock.currentMillis()
        this.cache = this.cache.filterTo(HashMap()) { (_, timestamp) -> now - timestamp <= dedupIntervalMillis }
    }
}


