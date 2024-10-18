package io.hackle.android.internal.event.dedup

import android.content.Context
import android.util.Log
import io.hackle.android.HackleConfig
import io.hackle.android.internal.database.repository.AndroidKeyValueRepository
import io.hackle.android.internal.event.dedup.CachedUserEventDedupDeterminer.Key
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateListener
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.time.Clock

internal abstract class CachedUserEventDedupDeterminer<CACHE_KEY : Key, USER_EVENT : UserEvent>(
    context: Context,
    repositoryName: String,
    private val dedupIntervalMillis: Long,
    private val clock: Clock,
) : UserEventDedupDeterminer, AppStateListener {

    private var currentUserIdentifiers: Map<String, String>? = null
    private val appStateManager: AppStateManager = AppStateManager.instance
    private var repository: AndroidKeyValueRepository

    private val repositoryPrefixDedup = "DEDUP_"
    private val repositoryKeyCurrentUser = "CURRENT_USE_PROPERTIES"
    private val repositoryUpdateInterval: Long = 60
    private var repositoryUpdateTime: Long = 0

    init {
        repository = AndroidKeyValueRepository.create(context, repositoryName)
        appStateManager.addListener(this)
        currentUserIdentifiers = loadCurrentUserFromRepository()
        Log.d(repositoryPrefixDedup, "currentUser init: $currentUserIdentifiers")
        updateRepository()
    }

    override fun isDedupTarget(event: UserEvent): Boolean {

        if (dedupIntervalMillis == HackleConfig.USER_EVENT_NO_DEDUP.toLong()) {
            return false
        }

        if (event.user.identifiers != currentUserIdentifiers) {
            Log.d(repositoryPrefixDedup, "diff user: ${event.user.identifiers}, cur: $currentUserIdentifiers")
            repository.clear()
            currentUserIdentifiers = event.user.identifiers
            storeCurrentUserToRepository()
        }

        @Suppress("UNCHECKED_CAST")
        val key = cacheKey(event as USER_EVENT)
        val now = clock.currentMillis()

        val firstMillis = repository.getLong(repositoryPrefixDedup + key, 0)
        if (firstMillis > 0 && now - firstMillis <= dedupIntervalMillis) {
            Log.d(repositoryPrefixDedup, "firstMillis: $firstMillis, dedupTime: ${now - firstMillis}, dedupIntervalMillis: $dedupIntervalMillis")
            return true
        }

        repository.putLong(repositoryPrefixDedup + key, now)
        return false
    }

    private fun storeCurrentUserToRepository() {
        if (currentUserIdentifiers != null) {
            repository.putString(repositoryKeyCurrentUser, currentUserIdentifiers!!.toJson())
        } else {
            repository.remove(repositoryKeyCurrentUser)
        }
    }

    private fun loadCurrentUserFromRepository(): Map<String, String>? {
        val identifiers: String? = repository.getString(repositoryKeyCurrentUser)
        if (identifiers != null) {
            return identifiers.parseJson()
        }

        return null
    }

    abstract fun supports(event: UserEvent): Boolean

    abstract fun cacheKey(event: USER_EVENT): CACHE_KEY

    interface Key {
        override fun equals(other: Any?): Boolean
        override fun hashCode(): Int
    }

    override fun onState(state: AppState, timestamp: Long) {
        Log.d(repositoryPrefixDedup, "onState(state=$state)")
        updateRepository()
    }

    private fun updateRepository() {
        val now = clock.currentMillis()
        if (now - repositoryUpdateTime < repositoryUpdateInterval) {
            Log.d(repositoryPrefixDedup, "updateRepository return")
            return
        }

        repositoryUpdateTime = now

        for ((key, value) in repository.getAll()) {
            Log.d(repositoryPrefixDedup, "updateRepository ($key, $value)")
            if (value is Double && now - value > dedupIntervalMillis) {
                if (key.startsWith(repositoryPrefixDedup)) {
                    Log.d(repositoryPrefixDedup, "remove ($key, $value)")
                    repository.remove(key)
                }
            }
        }
    }
}


