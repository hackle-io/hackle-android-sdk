package io.hackle.android.internal

import io.hackle.android.internal.application.install.ApplicationInstallStateManager
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.core.Updated
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.platform.device.Device
import io.hackle.android.internal.monitoring.metric.DecisionMetrics
import io.hackle.android.internal.notification.NotificationManager
import io.hackle.android.internal.pii.PIIProperty
import io.hackle.android.internal.pii.toSecuredEvent
import io.hackle.android.internal.push.token.PushTokenManager
import io.hackle.sdk.common.Screen
import io.hackle.android.internal.screen.ScreenManager
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.sync.PollingSynchronizer
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.utils.concurrent.Throttler
import io.hackle.android.internal.workspace.WorkspaceManager
import io.hackle.android.ui.explorer.HackleUserExplorer
import io.hackle.android.ui.explorer.base.HackleUserExplorerService
import io.hackle.sdk.common.*
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.DecisionReason.EXCEPTION
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.common.decision.RemoteConfigDecision
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.internal.metrics.Timer
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.internal.utils.tryClose
import io.hackle.sdk.core.model.ValueType
import io.hackle.sdk.core.model.toEvent
import java.io.Closeable
import java.util.concurrent.Executor

internal class HackleAppCore(
    private val clock: Clock,
    private val core: HackleCore,
    private val eventExecutor: Executor,
    private val backgroundExecutor: Executor,
    private val synchronizer: PollingSynchronizer,
    private val userManager: UserManager,
    private val workspaceManager: WorkspaceManager,
    private val sessionManager: SessionManager,
    private val screenManager: ScreenManager,
    private val eventProcessor: DefaultEventProcessor,
    private val pushTokenManager: PushTokenManager,
    private val notificationManager: NotificationManager,
    private val fetchThrottler: Throttler,
    private val device: Device,
    private val applicationInstallStateManager: ApplicationInstallStateManager,
    private val userExplorer: HackleUserExplorer,
) : Closeable {

    val deviceId: String get() = device.id
    val sessionId: String get() = sessionManager.requiredSession.id
    val user: User get() = userManager.currentUser
    val userExplorerService: HackleUserExplorerService get() = userExplorer.explorerService

    internal fun initialize(user: User?, onReady: Runnable) = apply {
        userManager.initialize(user)
        eventExecutor.execute {
            try {
                workspaceManager.initialize()
                pushTokenManager.initialize()
                sessionManager.initialize()
                eventProcessor.initialize()
                applicationInstallStateManager.initialize()
                synchronizer.sync()
                notificationManager.flush()
                applicationInstallStateManager.checkApplicationInstall()
                log.debug { "HackleApp initialized" }
            } catch (e: Throwable) {
                log.error { "Failed to initialize HackleApp: $e" }
            } finally {
                onReady.run()
            }
        }
    }

    fun showUserExplorer() {
        userExplorer.show()
        Metrics.counter("user.explorer.show").increment()
    }

    fun hideUserExplorer() {
        userExplorer.hide()
    }

    fun setUser(user: User, callback: Runnable?) {
        try {
            val updated = userManager.setUser(user)
            syncIfNeeded(updated, callback)
        } catch (e: Exception) {
            log.error { "Unexpected exception while set user: $e" }
            callback?.run()
        }
    }

    fun setUserId(userId: String?, callback: Runnable?) {
        try {
            val updated = userManager.setUserId(userId)
            syncIfNeeded(updated, callback)
        } catch (e: Exception) {
            log.error { "Unexpected exception while set userId: $e" }
            callback?.run()
        }
    }

    fun setDeviceId(deviceId: String, callback: Runnable?) {
        try {
            val updated = userManager.setDeviceId(deviceId)
            syncIfNeeded(updated, callback)
        } catch (e: Exception) {
            log.error { "Unexpected exception while set deviceId: $e" }
            callback?.run()
        }
    }

    fun updateUserProperties(
        operations: PropertyOperations,
        hackleAppContext: HackleAppContext,
        callback: Runnable?
    ) {
        try {
            val event = operations.toEvent()
            track(event, null, hackleAppContext)
            eventProcessor.flush()
            userManager.updateProperties(operations)
        } catch (e: Exception) {
            log.error { "Unexpected exception while update user properties: $e" }
        } finally {
            callback?.run()
        }
    }

    fun updatePushSubscriptions(operations: HackleSubscriptionOperations, hackleAppContext: HackleAppContext) {
        try {
            val event = operations.toEvent("\$push_subscriptions")
            track(event, null, hackleAppContext)
            core.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while update push subscription status: $e" }
        }
    }

    fun updateSmsSubscriptions(operations: HackleSubscriptionOperations, hackleAppContext: HackleAppContext) {
        try {
            val event = operations.toEvent("\$sms_subscriptions")
            track(event, null, hackleAppContext)
            core.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while update sms subscription status: $e" }
        }
    }

    fun updateKakaoSubscriptions(operations: HackleSubscriptionOperations, hackleAppContext: HackleAppContext) {
        try {
            val event = operations.toEvent("\$kakao_subscriptions")
            track(event, null, hackleAppContext)
            core.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while update kakao subscription status: $e" }
        }
    }

    fun resetUser(hackleAppContext: HackleAppContext, callback: Runnable?) {
        try {
            val updated = userManager.resetUser()
            track(PropertyOperations.clearAll().toEvent(), null, hackleAppContext)
            syncIfNeeded(updated, callback)
        } catch (e: Exception) {
            log.error { "Unexpected exception while reset user: $e" }
            callback?.run()
        }
    }

    fun setPhoneNumber(
        phoneNumber: String,
        hackleAppContext: HackleAppContext,
        callback: Runnable?
    ) {
        try {
            val event = PropertyOperations.builder()
                .set(PIIProperty.PHONE_NUMBER.key, phoneNumber)
                .build()
                .toSecuredEvent()
            track(event, null, hackleAppContext)
            eventProcessor.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while set phoneNumber: $e" }
        } finally {
            callback?.run()
        }
    }

    fun unsetPhoneNumber(hackleAppContext: HackleAppContext, callback: Runnable?) {
        try {
            val event = PropertyOperations.builder()
                .unset(PIIProperty.PHONE_NUMBER.key)
                .build()
                .toSecuredEvent()
            track(event, null, hackleAppContext)
            eventProcessor.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while unset phoneNumber: $e" }
        } finally {
            callback?.run()
        }
    }

    fun variationDetail(
        experimentKey: Long,
        user: User?,
        defaultVariation: Variation,
        hackleAppContext: HackleAppContext
    ): Decision {
        val sample = Timer.start()
        return try {
            val hackleUser = userManager.resolve(user, hackleAppContext)
            core.experiment(experimentKey, hackleUser, defaultVariation)
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding variation for experiment[$experimentKey]. Returning default variation[$defaultVariation]: $t" }
            Decision.of(defaultVariation, DecisionReason.EXCEPTION)
        }.also {
            DecisionMetrics.experiment(sample, experimentKey, it)
        }
    }

    fun allVariationDetails(user: User? = null, hackleAppContext: HackleAppContext): Map<Long, Decision> {
        return try {
            val hackleUser = userManager.resolve(user, hackleAppContext)
            core.experiments(hackleUser)
                .mapKeysTo(hashMapOf()) { (experiment, _) -> experiment.key }
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding variations for all experiments: $t" }
            hashMapOf()
        }
    }

    fun featureFlagDetail(
        featureKey: Long,
        user: User?,
        hackleAppContext: HackleAppContext
    ): FeatureFlagDecision {
        val sample = Timer.start()
        return try {
            val hackleUser = userManager.resolve(user, hackleAppContext)
            core.featureFlag(featureKey, hackleUser)
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding feature flag for feature[$featureKey]: $t" }
            FeatureFlagDecision.off(DecisionReason.EXCEPTION)
        }.also {
            DecisionMetrics.featureFlag(sample, featureKey, it)
        }
    }

    fun track(event: Event, user: User?, hackleAppContext: HackleAppContext) {
        try {
            val hackleUser = userManager.resolve(user, hackleAppContext)
            core.track(event, hackleUser, clock.currentMillis())
        } catch (t: Throwable) {
            log.error { "Unexpected exception while tracking event[${event.key}]: $t" }
        }
    }

    fun <T : Any> remoteConfig(
        key: String,
        requiredType: ValueType,
        defaultValue: T,
        user: User?,
        hackleAppContext: HackleAppContext,
    ): RemoteConfigDecision<T> {
        val sample = Timer.start()
        return try {
            val hackleUser = userManager.resolve(user, hackleAppContext)
            core.remoteConfig(key, hackleUser, requiredType, defaultValue)
        } catch (_: Exception) {
            log.error { "Unexpected exception while deciding remote config parameter[$key]. Returning default value." }
            RemoteConfigDecision.of(defaultValue, EXCEPTION)
        }.also {
            DecisionMetrics.remoteConfig(sample, key, it)
        }
    }

    fun fetch(callback: Runnable?) {
        fetchThrottler.execute(
            accept = {
                backgroundExecutor.execute {
                    synchronizer.sync()
                    callback?.run()
                }
            },
            reject = {
                log.debug { "Too many quick fetch requests." }
                callback?.run()
            }
        )
    }

    fun setCurrentScreen(screen: Screen) {
        screenManager.setCurrentScreen(screen, clock.currentMillis())
    }

    private fun syncIfNeeded(userUpdated: Updated<User>, callback: Runnable?) {
        try {
            backgroundExecutor.execute {
                try {
                    userManager.syncIfNeeded(userUpdated)
                } catch (e: Exception) {
                    log.error { "Failed to sync: $e" }
                } finally {
                    callback?.run()
                }
            }
        } catch (e: Exception) {
            log.error { "Failed to submit sync task: $e" }
            callback?.run()
        }
    }

    override fun close() {
        core.tryClose()
    }

    companion object {
        private val log = Logger<HackleAppCore>()
    }
}