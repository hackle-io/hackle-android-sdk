package io.hackle.android

import android.content.Context
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.android.internal.workspace.WorkspaceCacheHandler
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.common.Variation
import io.hackle.sdk.common.Variation.Companion.CONTROL
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.client.HackleInternalClient
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.utils.tryClose
import java.io.Closeable

/**
 * Entry point of Hackle Sdk.
 */
class HackleApp internal constructor(
    private val client: HackleInternalClient,
    private val workspaceCacheHandler: WorkspaceCacheHandler,
    private val userResolver: HackleUserResolver,
    private val device: Device,
) : Closeable {

    /**
     * The user's Device Id.
     */
    val deviceId: String get() = device.id

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
     * @param userId           the identifier of user to participate in the experiment. MUST NOT be null.
     * @param defaultVariation the default variation of the experiment. MUST NOT be null.
     *
     * @return the decided variation for the user, or [defaultVariation]
     */
    @JvmOverloads
    fun variation(
        experimentKey: Long,
        userId: String = deviceId,
        defaultVariation: Variation = CONTROL,
    ): Variation {
        return variation(experimentKey, User.of(userId), defaultVariation)
    }

    /**
     * Decide the variation to expose to the user for experiment.
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
        defaultVariation: Variation = CONTROL,
    ): Variation {
        return variationDetail(experimentKey, user, defaultVariation).variation
    }


    /**
     * Decide the variation to expose to the user for experiment, and returns an object that
     * describes the way the variation was decided.
     *
     * @param experimentKey    the unique key of the experiment.
     * @param userId           the identifier of user to participate in the experiment. MUST NOT be null.
     * @param defaultVariation the default variation of the experiment. MUST NOT be null.
     *
     * @return a [Decision] object
     */
    @JvmOverloads
    fun variationDetail(
        experimentKey: Long,
        userId: String = deviceId,
        defaultVariation: Variation = CONTROL,
    ): Decision {
        return variationDetail(experimentKey, User.of(userId), defaultVariation)
    }

    /**
     * Decide the variation to expose to the user for experiment, and returns an object that
     * describes the way the variation was decided.
     *
     * @param experimentKey    the unique key for the experiment.
     * @param user             the user to participate in the experiment. MUST NOT be null.
     * @param defaultVariation the default variation of the experiment. MUST NOT be null.
     *
     * @return a [Decision] object
     */
    @JvmOverloads
    fun variationDetail(
        experimentKey: Long,
        user: User,
        defaultVariation: Variation = CONTROL,
    ): Decision {
        return try {
            val hackleUser = userResolver.resolveOrNull(user)
                ?: return Decision.of(defaultVariation, DecisionReason.INVALID_INPUT)
            client.experiment(experimentKey, hackleUser, defaultVariation)
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding variation for experiment[$experimentKey]. Returning default variation[$defaultVariation]: $t" }
            Decision.of(defaultVariation, DecisionReason.EXCEPTION)
        }
    }

    /**
     * Decide whether the feature is turned on to the user.
     *
     * @param featureKey the unique key for the feature.
     * @param userId     the identifier of user.
     *
     * @return True if the feature is on.
     *         False if the feature is off.
     *
     * @since 2.0.0
     */
    @JvmOverloads
    fun isFeatureOn(featureKey: Long, userId: String = deviceId): Boolean {
        return featureFlagDetail(featureKey, User.of(userId)).isOn
    }

    /**
     * Decide whether the feature is turned on to the user.
     *
     * @param featureKey the unique key for the feature.
     * @param user       the user requesting the feature.
     *
     * @return True if the feature is on.
     *         False if the feature is off.
     *
     * @since 2.0.0
     */
    fun isFeatureOn(featureKey: Long, user: User): Boolean {
        return featureFlagDetail(featureKey, user).isOn
    }

    /**
     * Decide whether the feature is turned on to the user, and returns an object that
     * describes the way the flag was decided.
     *
     * @param featureKey the unique key for the feature.
     * @param userId     the identifier of user.
     *
     * @return a [FeatureFlagDecision] object
     *
     * @since 2.0.0
     */
    @JvmOverloads
    fun featureFlagDetail(featureKey: Long, userId: String = deviceId): FeatureFlagDecision {
        return featureFlagDetail(featureKey, User.of(userId))
    }

    /**
     * Decide whether the feature is turned on to the user, and returns an object that
     * describes the way the flag was decided.
     *
     * @param featureKey the unique key for the feature.
     * @param user       the user requesting the feature.
     *
     * @return a [FeatureFlagDecision] object
     *
     * @since 2.0.0
     */
    fun featureFlagDetail(featureKey: Long, user: User): FeatureFlagDecision {
        return try {
            val hackleUser = userResolver.resolveOrNull(user)
                ?: return FeatureFlagDecision.off(DecisionReason.INVALID_INPUT)
            client.featureFlag(featureKey, hackleUser)
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding feature flag for feature[$featureKey]: $t" }
            FeatureFlagDecision.off(DecisionReason.EXCEPTION)
        }
    }

    /**
     * Records the event that occurred by the user.
     *
     * @param eventKey the unique key of the event that occurred. MUST NOT be null.
     * @param userId   the identifier of user that occurred the event. MUST NOT be null.
     */
    @JvmOverloads
    fun track(eventKey: String, userId: String = deviceId) {
        track(Event.of(eventKey), User.of(userId))
    }

    /**
     * Records the event that occurred by the user.
     *
     * @param event  the event that occurred. MUST NOT be null.
     * @param userId the identifier of user that occurred the event. MUST NOT be null.
     */
    @JvmOverloads
    fun track(event: Event, userId: String = deviceId) {
        track(event, User.of(userId))
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
        try {
            val hackleUser = userResolver.resolveOrNull(user) ?: return
            client.track(event, hackleUser)
        } catch (t: Throwable) {
            log.error { "Unexpected exception while tracking event[${event.key}]: $t" }
        }
    }

    override fun close() {
        client.tryClose()
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
            return synchronized(LOCK) {
                checkNotNull(INSTANCE) { "HackleApp is not initialized. Make sure to call HackleApp.initializeApp() first" }
            }
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
            return synchronized(LOCK) {
                INSTANCE?.also { onReady.run() }
                    ?: HackleApps
                        .create(context.applicationContext, sdkKey, config)
                        .initialize { onReady.run() }
                        .also { INSTANCE = it }
            }
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
            return initializeApp(context, sdkKey, HackleConfig.DEFAULT, onReady)
        }
    }
}
