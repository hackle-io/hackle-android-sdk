package io.hackle.android.internal

import io.hackle.android.internal.application.install.ApplicationInstallStateManager
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.monitoring.metric.DecisionMetrics
import io.hackle.android.internal.notification.NotificationManager
import io.hackle.android.internal.optout.OptOutManager
import io.hackle.android.internal.pii.PIIProperty
import io.hackle.android.internal.pii.toSecuredEvent
import io.hackle.android.internal.platform.device.Device
import io.hackle.android.internal.push.token.PushTokenManager
import io.hackle.android.internal.screen.ScreenManager
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.sync.PollingSynchronizer
import io.hackle.android.internal.task.Futures
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.internal.task.onCompleteAsync
import io.hackle.android.internal.task.recover
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.utils.concurrent.Throttler
import io.hackle.android.internal.workspace.WorkspaceManager
import io.hackle.android.ui.explorer.HackleUserExplorer
import io.hackle.android.ui.explorer.base.HackleUserExplorerService
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.InAppMessageViewProvider
import io.hackle.sdk.common.*
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason.EXCEPTION
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.common.decision.RemoteConfigDecision
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations
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
    private val coreExecutor: Executor,
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
    private val optOutManager: OptOutManager,
    private val inAppMessageViewProvider: InAppMessageViewProvider,
) : Closeable {

    val deviceId: String get() = device.id
    val sessionId: String get() = sessionManager.requiredSession.id
    val user: User get() = userManager.currentUser
    val currentInAppMessageView: InAppMessageView? get() = inAppMessageViewProvider.currentView
    val isOptOutTracking: Boolean get() = optOutManager.isOptOutTracking
    val userExplorerService: HackleUserExplorerService get() = userExplorer.explorerService

    internal fun initialize(user: User?, onReady: Runnable) = apply {
        userManager.initialize(user)
        coreExecutor.execute {
            try {
                workspaceManager.initialize()
                pushTokenManager.initialize()
                sessionManager.initialize()
                eventProcessor.initialize()
                synchronizer.sync().get()
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

    fun getInAppMessageView(viewId: String): InAppMessageView? {
        return inAppMessageViewProvider.getView(viewId)
    }

    fun showUserExplorer() {
        userExplorer.show()
    }

    fun hideUserExplorer() {
        userExplorer.hide()
    }

    // User

    fun setUser(user: User, callback: Runnable?) {
        Futures.wrap { userManager.setUser(user) }
            .recover { log.error { "Unexpected exception while setUser: $it" } }
            .onCompleteAsync(TaskExecutors.background()) { callback?.run() }
    }

    fun resetUser(callback: Runnable?) {
        Futures.wrap { userManager.resetUser() }
            .recover { log.error { "Unexpected exception while reset user: $it" } }
            .onCompleteAsync(TaskExecutors.background()) { callback?.run() }
    }


    fun setUserId(userId: String?, callback: Runnable?) {
        Futures.wrap { userManager.setUserId(userId) }
            .recover { log.error { "Unexpected exception while set userId: $it" } }
            .onCompleteAsync(TaskExecutors.background()) { callback?.run() }
    }

    fun setDeviceId(deviceId: String, callback: Runnable?) {
        Futures.wrap { userManager.setDeviceId(deviceId) }
            .recover { log.error { "Unexpected exception while set deviceId: $it" } }
            .onCompleteAsync(TaskExecutors.background()) { callback?.run() }
    }

    fun updateUserProperties(operations: PropertyOperations, callback: Runnable?) {
        Futures.wrap { userManager.updateProperties(operations) }
            .recover { log.error { "Unexpected exception while update user properties: $it" } }
            .onCompleteAsync(TaskExecutors.background()) { callback?.run() }
    }

    fun updatePushSubscriptions(operations: HackleSubscriptionOperations, hackleAppContext: HackleAppContext) {
        try {
            val event = operations.toEvent("\$push_subscriptions")
            track(event, hackleAppContext)
            core.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while update push subscription status: $e" }
        }
    }

    fun updateSmsSubscriptions(operations: HackleSubscriptionOperations, hackleAppContext: HackleAppContext) {
        try {
            val event = operations.toEvent("\$sms_subscriptions")
            track(event, hackleAppContext)
            core.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while update sms subscription status: $e" }
        }
    }

    fun updateKakaoSubscriptions(operations: HackleSubscriptionOperations, hackleAppContext: HackleAppContext) {
        try {
            val event = operations.toEvent("\$kakao_subscriptions")
            track(event, hackleAppContext)
            core.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while update kakao subscription status: $e" }
        }
    }

    fun setPhoneNumber(
        phoneNumber: String,
        hackleAppContext: HackleAppContext,
        callback: Runnable?,
    ) {
        try {
            val event = PropertyOperations.builder()
                .set(PIIProperty.PHONE_NUMBER.key, phoneNumber)
                .build()
                .toSecuredEvent()
            track(event, hackleAppContext)
            eventProcessor.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while set phoneNumber: $e" }
        } finally {
            TaskExecutors.runOnBackground { callback?.run() }
        }
    }

    fun unsetPhoneNumber(hackleAppContext: HackleAppContext, callback: Runnable?) {
        try {
            val event = PropertyOperations.builder()
                .unset(PIIProperty.PHONE_NUMBER.key)
                .build()
                .toSecuredEvent()
            track(event, hackleAppContext)
            eventProcessor.flush()
        } catch (e: Exception) {
            log.error { "Unexpected exception while unset phoneNumber: $e" }
        } finally {
            TaskExecutors.runOnBackground { callback?.run() }
        }
    }

    fun variationDetail(
        experimentKey: Long,
        hackleAppContext: HackleAppContext,
    ): Decision {
        val sample = Timer.start()
        return try {
            val hackleUser = userManager.hackleUser(appContext = hackleAppContext)
            core.experiment(experimentKey, hackleUser)
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding variation for experiment[$experimentKey]: $t" }
            Decision.of(Variation.CONTROL, EXCEPTION)
        }.also {
            DecisionMetrics.experiment(sample, experimentKey, it)
        }
    }

    fun allVariationDetails(hackleAppContext: HackleAppContext): Map<Long, Decision> {
        return try {
            val hackleUser = userManager.hackleUser(appContext = hackleAppContext)
            core.experiments(hackleUser)
                .mapKeysTo(hashMapOf()) { (experiment, _) -> experiment.key }
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding variations for all experiments: $t" }
            hashMapOf()
        }
    }

    fun featureFlagDetail(
        featureKey: Long,
        hackleAppContext: HackleAppContext,
    ): FeatureFlagDecision {
        val sample = Timer.start()
        return try {
            val hackleUser = userManager.hackleUser(appContext = hackleAppContext)
            core.featureFlag(featureKey, hackleUser)
        } catch (t: Throwable) {
            log.error { "Unexpected exception while deciding feature flag for feature[$featureKey]: $t" }
            FeatureFlagDecision.off(EXCEPTION)
        }.also {
            DecisionMetrics.featureFlag(sample, featureKey, it)
        }
    }

    fun track(event: Event, hackleAppContext: HackleAppContext) {
        try {
            val hackleUser = userManager.hackleUser(appContext = hackleAppContext)
            core.track(event, hackleUser, clock.currentMillis())
        } catch (t: Throwable) {
            log.error { "Unexpected exception while tracking event[${event.key}]: $t" }
        }
    }

    fun <T : Any> remoteConfig(
        key: String,
        requiredType: ValueType,
        defaultValue: T,
        hackleAppContext: HackleAppContext,
    ): RemoteConfigDecision<T> {
        val sample = Timer.start()
        return try {
            val hackleUser = userManager.hackleUser(appContext = hackleAppContext)
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
                synchronizer.sync()
                    .onCompleteAsync(TaskExecutors.background()) { callback?.run() }
            },
            reject = {
                log.debug { "Too many quick fetch requests." }
                TaskExecutors.runOnBackground { callback?.run() }
            }
        )
    }

    fun setCurrentScreen(screen: Screen) {
        screenManager.setCurrentScreen(screen, clock.currentMillis())
    }

    fun setOptOutTracking(optOut: Boolean) {
        try {
            optOutManager.setOptOutTracking(optOut)
        } catch (e: Exception) {
            log.error { "Unexpected exception while setting opt-out tracking: $e" }
        }
    }

    override fun close() {
        core.tryClose()
    }

    companion object {
        private val log = Logger<HackleAppCore>()
    }
}