package io.hackle.android

import android.content.Context
import io.hackle.android.internal.workspace.WorkspaceCacheHandler
import io.hackle.sdk.Event
import io.hackle.sdk.User
import io.hackle.sdk.Variation
import io.hackle.sdk.Variation.Companion.CONTROL
import io.hackle.sdk.core.client.HackleClient
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.utils.tryClose
import java.io.Closeable

/**
 * Entry point of Hackle Sdk.
 */
class HackleApp internal constructor(
    private val hackleClient: HackleClient,
    private val workspaceCacheHandler: WorkspaceCacheHandler,
) : Closeable {

    /**
     * Decide the variation to expose to the user for experiment.
     *
     * This method return the [defaultVariation] if:
     * - SDK is not ready
     * - The experiment key is invalid
     * - The experiment has not started yet
     * - The user is not allocated to the experiment
     * - The decided variation has been dropped
     *
     * @param experimentKey    the unique key of the experiment.
     * @param user             the user to participate in the experiment. MUST NOT be null.
     * @param defaultVariation the default variation of the experiment. MUST NOT be null.
     *
     * @return the decided variation for the user, or [defaultVariation]
     */
    @JvmOverloads
    fun variation(
        experimentKey: Long,
        user: User,
        defaultVariation: Variation = CONTROL
    ): Variation {
        return runCatching { hackleClient.variation(experimentKey, user, defaultVariation) }
            .getOrElse {
                log.error { "Unexpected exception while deciding variation for experiment[$experimentKey]. Returning default variation[$defaultVariation]: $it" }
                defaultVariation
            }
    }

    /**
     * Records the event that occurred by the user.
     *
     * @param eventKey the unique key of the event that occurred. MUST NOT be null.
     * @param user     the user that occurred the event. MUST NOT be null.
     */
    fun track(eventKey: String, user: User) {
        track(Event.of(eventKey), user)
    }

    /**
     * Records the event that occurred by the user.
     *
     * @param event the event that occurred. MUST NOT be null.
     * @param user  the user that occurred the event. MUST NOT be null.
     */
    fun track(event: Event, user: User) {
        runCatching { hackleClient.track(event, user) }
            .onFailure { log.error { "Unexpected exception while tracking event[${event.key}]: $it" } }
    }

    override fun close() {
        hackleClient.tryClose()
    }

    internal fun initialize(onReady: () -> Unit) = apply {
        workspaceCacheHandler.fetchAndCache(onReady)
    }

    companion object {

        private val log = Logger<HackleApp>()

        private val LOCK = Any()
        private var INSTANCE: HackleApp? = null


        /**
         * Returns a singleton instance of [HackleApp]
         *
         * @throws IllegalStateException if the HackleApp was not initialized.
         */
        @JvmStatic
        fun getInstance(): HackleApp {
            synchronized(LOCK) {
                return checkNotNull(INSTANCE) { "HackleApp is not initialized. Make sure to call HackleApp.initializeApp() first" }
            }
        }

        /**
         * Initialized the HackleApp instance.
         *
         * @param context [Context]
         * @param sdkKey the SDK key of your Hackle environment.
         * @param onReady callback that is called when HackleApp is ready to use.
         */
        @JvmOverloads
        @JvmStatic
        fun initializeApp(
            context: Context,
            sdkKey: String,
            onReady: Runnable = Runnable { }
        ): HackleApp {
            synchronized(LOCK) {
                return INSTANCE
                    ?: HackleApps
                        .create(sdkKey)
                        .initialize { onReady.run() }
                        .also { INSTANCE = it }
            }
        }
    }
}
