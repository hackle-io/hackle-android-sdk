package io.hackle.android

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.WebView
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.HackleInvocatorImpl
import io.hackle.android.internal.invocator.web.HackleJavascriptInterface
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.internal.lifecycle.LifecycleManager
import io.hackle.android.internal.model.AndroidBuild
import io.hackle.android.internal.model.Sdk
import io.hackle.android.ui.explorer.HackleUserExplorer
import io.hackle.sdk.common.Screen
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.notification.NotificationHandler
import io.hackle.sdk.common.*
import io.hackle.sdk.common.HacklePushSubscriptionStatus
import io.hackle.sdk.common.Variation.Companion.CONTROL
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.internal.log.Logger
import java.io.Closeable

/**
 * Entry point of Hackle Sdk.
 */
class HackleApp internal constructor(
    private val hackleAppCore: HackleAppCore,
    internal val sdk: Sdk,
    internal val mode: HackleAppMode,
) : Closeable {
    /**
     * The user's Device Id.
     */
    val deviceId: String get() = hackleAppCore.deviceId

    /**
     * Current Session Id. If session is unavailable, returns "0.ffffffff"
     */
    val sessionId: String get() = hackleAppCore.sessionId

    /**
     * Current User
     */
    val user: User get() = hackleAppCore.user

    internal val userExplorer: HackleUserExplorer get() = hackleAppCore.userExplorer

    /**
     * Shows the user explorer UI button.
     *
     * This is typically used for debugging purposes to view the current user's state.
     */
    fun showUserExplorer() {
        userExplorer.show()
    }

    /**
     * Hides the user explorer UI button if it is currently visible.
     */
    fun hideUserExplorer() {
        userExplorer.hide()
    }

    /**
     * Sets or replaces the current user.
     *
     * @param user the user to set.
     * @param callback an optional callback to be executed when the operation is complete.
     */
    @JvmOverloads
    fun setUser(user: User, callback: Runnable? = null) {
        hackleAppCore.setUser(user, callback)
    }

    /**
     * Sets the userId for the current user.
     *
     * @param userId the userId to set for the user. Can be null to identify an anonymous user.
     * @param callback an optional callback to be executed when the operation is complete.
     */
    @JvmOverloads
    fun setUserId(userId: String?, callback: Runnable? = null) {
        hackleAppCore.setUserId(userId, callback)
    }

    /**
     * Sets a custom device ID.
     *
     * @param deviceId the custom device ID to set.
     * @param callback an optional callback to be executed when the operation is complete.
     */
    @JvmOverloads
    fun setDeviceId(deviceId: String, callback: Runnable? = null) {
        hackleAppCore.setDeviceId(deviceId, callback)
    }

    /**
     * Sets a single user property.
     *
     * @param key the key of the property.
     * @param value the value of the property.
     * @param callback an optional callback to be executed when the operation is complete.
     */
    @JvmOverloads
    fun setUserProperty(key: String, value: Any?, callback: Runnable? = null) {
        val operations = PropertyOperations.builder()
            .set(key, value)
            .build()
        hackleAppCore.updateUserProperties(operations, HackleAppContext.DEFAULT, callback)
    }

    /**
     * Updates user properties with a set of operations.
     *
     * @param operations a set of operations to apply to user properties.
     * @param callback an optional callback to be executed when the operation is complete.
     */
    @JvmOverloads
    fun updateUserProperties(operations: PropertyOperations, callback: Runnable? = null) {
        hackleAppCore.updateUserProperties(operations, HackleAppContext.DEFAULT, callback)
    }

    /**
     * Updates push notification subscription status.
     *
     * @param operations a set of subscription operations to apply.
     */
    fun updatePushSubscriptions(operations: HackleSubscriptionOperations) {
        hackleAppCore.updatePushSubscriptions(operations, HackleAppContext.DEFAULT)
    }

    /**
     * Updates SMS subscription status.
     *
     * @param operations a set of subscription operations to apply.
     */
    fun updateSmsSubscriptions(operations: HackleSubscriptionOperations) {
        hackleAppCore.updateSmsSubscriptions(operations, HackleAppContext.DEFAULT)
    }

    /**
     * Updates KakaoTalk subscription status.
     *
     * @param operations a set of subscription operations to apply.
     */
    fun updateKakaoSubscriptions(operations: HackleSubscriptionOperations) {
        hackleAppCore.updateKakaoSubscriptions(operations, HackleAppContext.DEFAULT)
    }

    /**
     * Resets the current user.
     *
     * When reset user, the deviceId is set to hackleDeviceId
     * and id, userId, properties are set to null
     *
     * @param callback an optional callback to be executed when the operation is complete.
     */
    @JvmOverloads
    fun resetUser(callback: Runnable? = null) {
        hackleAppCore.resetUser(HackleAppContext.DEFAULT, callback)
    }

    /**
     * Sets the phone number for the current user.
     *
     * @param phoneNumber the phone number to set.
     * @param callback an optional callback to be executed when the operation is complete.
     */
    @JvmOverloads
    fun setPhoneNumber(phoneNumber: String, callback: Runnable? = null) {
        hackleAppCore.setPhoneNumber(phoneNumber, HackleAppContext.DEFAULT, callback)
    }

    /**
     * Removes the phone number from the current user.
     *
     * @param callback an optional callback to be executed when the operation is complete.
     */
    @JvmOverloads
    fun unsetPhoneNumber(callback: Runnable? = null) {
        hackleAppCore.unsetPhoneNumber(HackleAppContext.DEFAULT, callback)
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
     * Decide the variation to expose to the user for experiment and returns an object that
     * describes the way the variation was decided.
     *
     * @param experimentKey    the unique key for the experiment.
     * @param defaultVariation the default variation of the experiment. MUST NOT be null.
     *
     * @return a [Decision] object
     */
    @JvmOverloads
    fun variationDetail(experimentKey: Long, defaultVariation: Variation = CONTROL): Decision {
        return hackleAppCore.variationDetail(experimentKey, null, defaultVariation, HackleAppContext.DEFAULT)
    }

    /**
     * Decide the variations for all experiments and returns a map of decision results.
     *
     * @return key   - experimentKey
     *         value - decision result
     */
    fun allVariationDetails(): Map<Long, Decision> {
        return hackleAppCore.allVariationDetails(null, HackleAppContext.DEFAULT)
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
     * Decide whether the feature is turned on to the user and returns an object that
     * describes the way the flag was decided.
     *
     * @param featureKey the unique key for the feature.
     *
     * @return a [FeatureFlagDecision] object
     */
    fun featureFlagDetail(featureKey: Long): FeatureFlagDecision {
        return hackleAppCore.featureFlagDetail(featureKey, null, HackleAppContext.DEFAULT)
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
        hackleAppCore.track(event, null, HackleAppContext.DEFAULT)
    }


    /**
     * Returns an instance of Hackle Remote Config.
     */
    fun remoteConfig(): HackleRemoteConfig {
        return hackleAppCore.remoteConfig(null)
    }

    /**
     * Injects the supplied Java object into this WebView.
     *
     * @param webView  Target WebView. MUST NOT be null.
     */
    @SuppressLint("UseRequiresApi")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun setWebViewBridge(webView: WebView) {
        if (AndroidBuild.sdkVersion() < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            throw IllegalStateException(
                "HackleApp.setJavascriptInterface should not be called with minSdkVersion < 17 for security reasons: " +
                        "JavaScript can use reflection to manipulate application"
            )
        }
        val invocator = invocator()
        val jsInterface = HackleJavascriptInterface(invocator, this.sdk, this.mode)
        webView.addJavascriptInterface(jsInterface, HackleJavascriptInterface.NAME)
    }

    internal fun invocator(): HackleInvocator {
        return HackleInvocatorImpl(this.hackleAppCore)
    }

    fun setInAppMessageListener(listener: HackleInAppMessageListener?) {
        InAppMessageUi.instance.setListener(listener)
    }

    @JvmOverloads
    fun fetch(callback: Runnable? = null) {
        hackleAppCore.fetch(callback)
    }

    fun setCurrentScreen(screen: Screen) {
        hackleAppCore.setCurrentScreen(screen)
    }

    override fun close() {
        hackleAppCore.close()
    }

    internal fun initialize(user: User?, onReady: Runnable) = apply {
        hackleAppCore.initialize(user, onReady)
    }

    // Deprecated
    //<editor-fold desc="Deprecated function">
    @Deprecated("Use variation(experimentKey) with setUser(user) instead.")
    @JvmOverloads
    fun variation(
        experimentKey: Long,
        userId: String,
        defaultVariation: Variation = CONTROL,
    ): Variation {
        return hackleAppCore.variationDetail(
            experimentKey,
            User.of(userId),
            defaultVariation,
            HackleAppContext.DEFAULT
        ).variation
    }

    @Deprecated("Use variation(experimentKey) with setUser(user) instead.")
    @JvmOverloads
    fun variation(
        experimentKey: Long,
        user: User,
        defaultVariation: Variation = CONTROL,
    ): Variation {
        return hackleAppCore.variationDetail(experimentKey, user, defaultVariation, HackleAppContext.DEFAULT).variation
    }

    @Deprecated("Use variationDetail(experimentKey) with setUser(user) instead.")
    @JvmOverloads
    fun variationDetail(
        experimentKey: Long,
        userId: String,
        defaultVariation: Variation = CONTROL,
    ): Decision {
        return hackleAppCore.variationDetail(experimentKey, User.of(userId), defaultVariation, HackleAppContext.DEFAULT)
    }

    @Deprecated("Use variationDetail(experimentKey) with setUser(user) instead.")
    @JvmOverloads
    fun variationDetail(
        experimentKey: Long,
        user: User,
        defaultVariation: Variation = CONTROL,
    ): Decision {
        return hackleAppCore.variationDetail(experimentKey, user, defaultVariation, HackleAppContext.DEFAULT)
    }

    @Deprecated("Use allVariationDetails() with setUser(user) instead.")
    fun allVariationDetails(user: User): Map<Long, Decision> {
        return hackleAppCore.allVariationDetails(user, HackleAppContext.DEFAULT)
    }

    @Deprecated("Use featureFlagDetail(featureKey) with setUser(user) instead.")
    fun featureFlagDetail(featureKey: Long, userId: String): FeatureFlagDecision {
        return hackleAppCore.featureFlagDetail(featureKey, User.of(userId), HackleAppContext.DEFAULT)
    }

    @Deprecated("Use featureFlagDetail(featureKey) with setUser(user) instead.")
    fun featureFlagDetail(featureKey: Long, user: User): FeatureFlagDecision {
        return hackleAppCore.featureFlagDetail(featureKey, user, HackleAppContext.DEFAULT)
    }

    @Deprecated("Use isFeatureOn(featureKey) with setUser(user) instead.")
    fun isFeatureOn(featureKey: Long, userId: String): Boolean {
        return hackleAppCore.featureFlagDetail(featureKey, User.of(userId), HackleAppContext.DEFAULT).isOn
    }

    @Deprecated("Use isFeatureOn(featureKey) with setUser(user) instead.")
    fun isFeatureOn(featureKey: Long, user: User): Boolean {
        return hackleAppCore.featureFlagDetail(featureKey, user, HackleAppContext.DEFAULT).isOn
    }

    @Deprecated("Use track(eventKey) with setUser(user) instead.")
    fun track(eventKey: String, userId: String) {
        hackleAppCore.track(Event.of(eventKey), User.of(userId), HackleAppContext.DEFAULT)
    }

    @Deprecated("Use track(eventKey) with setUser(user) instead.")
    fun track(event: Event, userId: String) {
        hackleAppCore.track(event, User.of(userId), HackleAppContext.DEFAULT)
    }

    @Deprecated("Use track(eventKey) with setUser(user) instead.")
    fun track(eventKey: String, user: User) {
        hackleAppCore.track(Event.of(eventKey), user, HackleAppContext.DEFAULT)
    }

    @Deprecated("Use track(eventKey) with setUser(user) instead.")
    fun track(event: Event, user: User) {
        hackleAppCore.track(event, user, HackleAppContext.DEFAULT)
    }

    @Deprecated("Use remoteConfig() with setUser(user) instead.")
    fun remoteConfig(user: User): HackleRemoteConfig {
        return hackleAppCore.remoteConfig(user)
    }

    @Deprecated("Use showUserExplorer() instead.", ReplaceWith("showUserExplorer()"))
    fun showUserExplorer(activity: Activity) {
        showUserExplorer()
    }

    @Deprecated("Do not use the method because Hackle SDK will register push token by self. (Will remove v2.38.0)")
    fun setPushToken(token: String) {
        log.debug { "HackleApp::setPushToken(token) will do nothing, please remove usages." }
    }

    @Deprecated("Do not use this method because it does nothing. Use `updatePushSubscriptions(operations)` instead.")
    fun updatePushSubscriptionStatus(status: HacklePushSubscriptionStatus) {
        log.error {
            "updatePushSubscriptionStatus does nothing. Use updatePushSubscriptions(operations) instead."
        }
    }
    //</editor-fold>

    companion object {

        private val log = Logger<HackleApp>()

        private val LOCK = Any()
        private var INSTANCE: HackleApp? = null

        @JvmStatic
        fun registerActivityLifecycleCallbacks(context: Context) {
            LifecycleManager.instance.registerTo(context)
        }

        @JvmStatic
        fun isHacklePushMessage(intent: Intent): Boolean {
            return NotificationHandler.isHackleIntent(intent)
        }

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
                INSTANCE?.also { onReady.run() }
                    ?: HackleApps
                        .create(context.applicationContext, sdkKey, config)
                        .initialize(user, onReady)
                        .also { AppStateManager.instance.publishStateIfNeeded() }
                        .also { INSTANCE = it }
            }
        }
    }
}
