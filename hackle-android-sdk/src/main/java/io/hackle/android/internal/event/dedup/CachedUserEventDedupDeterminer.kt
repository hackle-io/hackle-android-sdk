package io.hackle.android.internal.event.dedup

import android.content.Context
import android.util.Log
import io.hackle.android.HackleConfig
import io.hackle.android.internal.database.repository.AndroidKeyValueRepository
import io.hackle.android.internal.event.dedup.CachedUserEventDedupDeterminer.Key
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateListener
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.user.HackleUser

internal abstract class CachedUserEventDedupDeterminer<CACHE_KEY : Key, USER_EVENT : UserEvent>(
    private val context: Context,
    private val repositorySuiteName: String,
    private val dedupIntervalMillis: Long,
    private val clock: Clock,
) : UserEventDedupDeterminer, AppStateListener {
    private var cache = hashMapOf<CACHE_KEY, Long>()
    private var currentUser: HackleUser? = null
    private val appStateManager: AppStateManager = AppStateManager.instance
    private lateinit var repository: AndroidKeyValueRepository

    init {
        repository = AndroidKeyValueRepository.create(context, repositorySuiteName)
        appStateManager.addListener(this)
    }

    override fun isDedupTarget(event: UserEvent): Boolean {

        if (dedupIntervalMillis == HackleConfig.USER_EVENT_NO_DEDUP.toLong()) {
            return false
        }

        if (event.user.identifiers != currentUser?.identifiers) {
            currentUser = event.user
            cache = hashMapOf()
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

    abstract fun cacheKey(event: USER_EVENT): CACHE_KEY

    interface Key {
        override fun equals(other: Any?): Boolean
        override fun hashCode(): Int
    }

    override fun onState(state: AppState, timestamp: Long) {
        Log.d("Dedup", "onState(state=$state)")
    }
}


