package io.hackle.android

import android.app.Activity
import android.content.Context
import io.hackle.android.explorer.base.HackleUserExplorer
import io.hackle.android.explorer.view.HackleUserExplorerView
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.monitoring.metric.DecisionMetrics
import io.hackle.android.internal.remoteconfig.HackleRemoteConfigImpl
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.workspace.WorkspaceCacheHandler
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.User
import io.hackle.sdk.common.Variation
import io.hackle.sdk.common.Variation.Companion.CONTROL
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.client.HackleInternalClient
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.internal.metrics.Timer
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.internal.utils.tryClose
import java.io.Closeable
import java.util.concurrent.Executor

/**
 * Entry point of Hackle Sdk.
 */
class HackleApp internal constructor(
    private val clock: Clock,
    private val client: HackleInternalClient,
    private val eventExecutor: Executor,
    private val workspaceCacheHandler: WorkspaceCacheHandler,
    private val hackleUserResolver: HackleUserResolver,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val eventProcessor: DefaultEventProcessor,
    private val device: Device,
    internal val userExplorer: HackleUserExplorer,
) : Closeable {

    private val userExplorerView: HackleUserExplorerView by lazy { HackleUserExplorerView() }

    /**
     * The user's Device Id.
     */
    val deviceId: String get() = device.id

    /**
     * Current Session Id. If session is unavailable, returns "0.ffffffff"
     */
    val sessionId: String get() = sessionManager.requiredSession.id

    val user: User get() = userManager.currentUser

    fun showUserExplorer(activity: Activity) {
        userExplorerView.attachTo(activity)
        Metrics.counter("user.explorer.show").increment()
    }

    fun setUser(user: User) {
        try {
            userManager.setUser(user)
        } catch (e: Exception) {
            log.error { "Unexpected exception while set user: $e" }
        }
    }

    fun setUserId(userId: String?) {
        try {
            userManager.setUserId(userId)
        } catch (e: Exception) {
            log.error { "Unexpected exception while set userId: $e" }
        }
    }

    fun setDeviceId(deviceId: String) {
        try {
            userManager.setDeviceId(deviceId)
        } catch (e: Exception) {
            log.error { "Unexpected exception while set deviceId: $e" }
        }
    }

    fun setUserProperty(key: String, value: Any?) {
        try {
            userManager.setUserProperty(key, value)
        } catch (e: Exception) {
            log.error { "Unexpected exception while set userProperty: $e" }
        }
    }

    fun resetUser() {
        try {
            userManager.resetUser()
        } catch (e: Exception) {
            log.error { "Unexpected exception while reset user: $e" }
        }
    }

    /**
     * Decide the variation to expose to the user for experiment.
     *
     * @param experimentKey    the unique key of the experiment.
     * @param defaultVariation the default variation of the experiment. MUST NOT be null.
     *
     * @return the decided variation for the user, or [defaultVariation]
     */
    @JvmOverloads
    fun variation(experimentKey: Long, defaultVariation: Variation = CONTROL): Variation {
        return variationDetail(experimentKey, defaultVariation).variation
    }

    /**
     * Decide the variation to expose to the user for experiment, and returns an object that
     * describes the way the variation was decided.
     *
     * @param experimentKey    the unique key for the experiment.
     * @param defaultVariation the default variation of the experiment. MUST NOT be null.
     *
     * @return a [Decision] object
     */
    @JvmOverloads
    fun variationDetail(experimentKey: Long, defaultVariation: Variation = CONTROL): Decision {
        return variationDetailInternal(experimentKey, null, defaultVariation)
    }

    private fun variationDetailInternal(
        experimentKey: Long,
        user: User?,
        defaultVariation: Variation,
    ): Decision {
        val sample = Timer.start()
        return try {
            val currentUser = userManager.resolve(user)
            val hackleUser = hackleUserResolver.resolve(currentUser)
            client.experiment(experimentKey, hackleUser, defaultVariation)
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding variation for experiment[$experimentKey]. Returning default variation[$defaultVariation]: $t" }
            Decision.of(defaultVariation, DecisionReason.EXCEPTION)
        }.also {
            DecisionMetrics.experiment(sample, experimentKey, it)
        }
    }

    /**
     * Decide the variations for all experiments, and returns a map of decision results.
     *
     * @return key   - experimentKey
     *         value - decision result
     */
    fun allVariationDetails(): Map<Long, Decision> {
        return allVariationDetailsInternal(null)
    }

    private fun allVariationDetailsInternal(user: User?): Map<Long, Decision> {
        return try {
            val currentUser = userManager.resolve(user)
            val hackleUser = hackleUserResolver.resolve(currentUser)
            client.experiments(hackleUser)
                .mapKeysTo(hashMapOf()) { (experiment, _) -> experiment.key }
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding variations for all experiments: $t" }
            hashMapOf()
        }
    }

    /**
     * Decide whether the feature is turned on to the user.
     *
     * @param featureKey the unique key for the feature.
     *
     * @return True if the feature is on.
     *         False if the feature is off.
     *
     * @since 2.0.0
     */
    fun isFeatureOn(featureKey: Long): Boolean {
        return featureFlagDetail(featureKey).isOn
    }

    /**
     * Decide whether the feature is turned on to the user, and returns an object that
     * describes the way the flag was decided.
     *
     * @param featureKey the unique key for the feature.
     *
     * @return a [FeatureFlagDecision] object
     */
    fun featureFlagDetail(featureKey: Long): FeatureFlagDecision {
        return featureFlagDetailInternal(featureKey, null)
    }

    private fun featureFlagDetailInternal(featureKey: Long, user: User?): FeatureFlagDecision {
        val sample = Timer.start()
        return try {
            val currentUser = userManager.resolve(user)
            val hackleUser = hackleUserResolver.resolve(currentUser)
            client.featureFlag(featureKey, hackleUser)
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding feature flag for feature[$featureKey]: $t" }
            FeatureFlagDecision.off(DecisionReason.EXCEPTION)
        }.also {
            DecisionMetrics.featureFlag(sample, featureKey, it)
        }
    }

    /**
     * Records the event that occurred by the user.
     *
     * @param eventKey the unique key of the event that occurred. MUST NOT be null.
     */
    fun track(eventKey: String) {
        track(Event.of(eventKey))
    }

    /**
     * Records the event that occurred by the user.
     *
     * @param event  the event that occurred. MUST NOT be null.
     */
    fun track(event: Event) {
        trackInternal(event, null)
    }

    private fun trackInternal(event: Event, user: User?) {
        try {
            val currentUser = userManager.resolve(user)
            val hackleUser = hackleUserResolver.resolve(currentUser)
            client.track(event, hackleUser, clock.currentMillis())
        } catch (t: Throwable) {
            log.error { "Unexpected exception while tracking event[${event.key}]: $t" }
        }
    }

    /**
     * Returns a instance of Hackle Remote Config.
     */
    fun remoteConfig(): HackleRemoteConfig {
        return HackleRemoteConfigImpl(null, client, userManager, hackleUserResolver)
    }

    override fun close() {
        client.tryClose()
    }

    internal fun initialize(user: User?, onReady: Runnable) = apply {
        userManager.initialize(user)
        eventExecutor.execute {
            try {
                sessionManager.initialize()
                eventProcessor.initialize()
                workspaceCacheHandler.fetchAndCache()
                log.debug { "HackleApp initialized" }
            } catch (e: Throwable) {
                log.error { "Failed to initialize HackleApp: $e" }
            } finally {
                onReady.run()
            }
        }
    }

    // Deprecated

    @Deprecated("Use variation(experimentKey) with setUser(user) instead.")
    @JvmOverloads
    fun variation(
        experimentKey: Long,
        userId: String,
        defaultVariation: Variation = CONTROL,
    ): Variation {
        return variationDetailInternal(experimentKey, User.of(userId), defaultVariation).variation
    }

    @Deprecated("Use variation(experimentKey) with setUser(user) instead.")
    @JvmOverloads
    fun variation(
        experimentKey: Long,
        user: User,
        defaultVariation: Variation = CONTROL,
    ): Variation {
        return variationDetailInternal(experimentKey, user, defaultVariation).variation
    }

    @Deprecated("Use variationDetail(experimentKey) with setUser(user) instead.")
    @JvmOverloads
    fun variationDetail(
        experimentKey: Long,
        userId: String,
        defaultVariation: Variation = CONTROL,
    ): Decision {
        return variationDetailInternal(experimentKey, User.of(userId), defaultVariation)
    }

    @Deprecated("Use variationDetail(experimentKey) with setUser(user) instead.")
    @JvmOverloads
    fun variationDetail(
        experimentKey: Long,
        user: User,
        defaultVariation: Variation = CONTROL,
    ): Decision {
        return variationDetailInternal(experimentKey, user, defaultVariation)
    }

    @Deprecated("Use allVariationDetails() with setUser(user) instead.")
    fun allVariationDetails(user: User): Map<Long, Decision> {
        return allVariationDetailsInternal(user)
    }

    @Deprecated("Use featureFlagDetail(featureKey) with setUser(user) instead.")
    fun featureFlagDetail(featureKey: Long, userId: String): FeatureFlagDecision {
        return featureFlagDetailInternal(featureKey, User.of(userId))
    }

    @Deprecated("Use featureFlagDetail(featureKey) with setUser(user) instead.")
    fun featureFlagDetail(featureKey: Long, user: User): FeatureFlagDecision {
        return featureFlagDetailInternal(featureKey, user)
    }

    @Deprecated("Use isFeatureOn(featureKey) with setUser(user) instead.")
    fun isFeatureOn(featureKey: Long, userId: String): Boolean {
        return featureFlagDetailInternal(featureKey, User.of(userId)).isOn
    }

    @Deprecated("Use isFeatureOn(featureKey) with setUser(user) instead.")
    fun isFeatureOn(featureKey: Long, user: User): Boolean {
        return featureFlagDetailInternal(featureKey, user).isOn
    }

    @Deprecated("Use track(eventKey) with setUser(user) instead.")
    fun track(eventKey: String, userId: String) {
        trackInternal(Event.of(eventKey), User.of(userId))
    }

    @Deprecated("Use track(eventKey) with setUser(user) instead.")
    fun track(event: Event, userId: String) {
        trackInternal(event, User.of(userId))
    }

    @Deprecated("Use track(eventKey) with setUser(user) instead.")
    fun track(eventKey: String, user: User) {
        trackInternal(Event.of(eventKey), user)
    }

    @Deprecated("Use track(eventKey) with setUser(user) instead.")
    fun track(event: Event, user: User) {
        trackInternal(event, user)
    }

    @Deprecated("Use remoteConfig() with setUser(user) instead.")
    fun remoteConfig(user: User): HackleRemoteConfig {
        return HackleRemoteConfigImpl(user, client, userManager, hackleUserResolver)
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
            return synchronized(LOCK) {
                checkNotNull(INSTANCE) { "HackleApp is not initialized. Make sure to call HackleApp.initializeApp() first" }
            }
        }

        @JvmOverloads
        @JvmStatic
        fun initializeApp(
            context: Context,
            sdkKey: String,
            user: User?,
            config: HackleConfig = HackleConfig.DEFAULT,
            onReady: Runnable = Runnable { },
        ): HackleApp {
            return initializeAppInternal(context, sdkKey, user, config, onReady)
        }

        /**
         * Initialized the HackleApp instance.
         *
         * @param context [Context]
         * @param sdkKey the SDK key of your Hackle environment.
         * @param config the HackleConfig that contains the desired configuration.
         * @param onReady callback that is called when HackleApp is ready to use.
         */
        @JvmOverloads
        @JvmStatic
        fun initializeApp(
            context: Context,
            sdkKey: String,
            config: HackleConfig = HackleConfig.DEFAULT,
            onReady: Runnable = Runnable { },
        ): HackleApp {
            return initializeAppInternal(context, sdkKey, null, config, onReady)
        }

        /**
         * Initialized the HackleApp instance.
         *
         * @param context [Context]
         * @param sdkKey the SDK key of your Hackle environment.
         * @param onReady callback that is called when HackleApp is ready to use.
         */
        @JvmStatic
        fun initializeApp(
            context: Context,
            sdkKey: String,
            onReady: Runnable,
        ): HackleApp {
            return initializeAppInternal(context, sdkKey, null, HackleConfig.DEFAULT, onReady)
        }

        private fun initializeAppInternal(
            context: Context,
            sdkKey: String,
            user: User?,
            config: HackleConfig,
            onReady: Runnable,
        ): HackleApp {
            return synchronized(LOCK) {
                INSTANCE?.also { onReady.run() } ?: HackleApps.create(context.applicationContext,
                    sdkKey,
                    config).initialize(user, onReady).also { INSTANCE = it }
            }
        }
    }
}
