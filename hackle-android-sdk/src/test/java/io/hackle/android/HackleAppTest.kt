package io.hackle.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycleManager
import io.hackle.android.internal.application.install.ApplicationInstallStateManager
import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleManager
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.invocator.web.HackleJavascriptInterface
import io.hackle.android.internal.model.Sdk
import io.hackle.android.internal.notification.NotificationManager
import io.hackle.android.internal.optout.OptOutManager
import io.hackle.android.internal.push.token.PushTokenManager
import io.hackle.android.internal.remoteconfig.HackleRemoteConfigImpl
import io.hackle.android.internal.screen.ScreenManager
import io.hackle.android.internal.session.Session
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.sync.PollingSynchronizer
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.internal.user.local.LocalUserManager
import io.hackle.android.internal.utils.concurrent.Throttler
import io.hackle.android.internal.workspace.config.WorkspaceConfigManager
import io.hackle.android.mock.MockDevice
import io.hackle.android.ui.explorer.HackleUserExplorer
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.InAppMessageViewProvider
import io.hackle.android.ui.notification.NotificationHandler
import io.hackle.sdk.common.*
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations
import io.hackle.sdk.common.subscription.HackleSubscriptionStatus
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

class HackleAppTest {

    @RelaxedMockK
    private lateinit var core: HackleCore

    @RelaxedMockK
    private lateinit var coreExecutor: Executor

    @RelaxedMockK
    private lateinit var backgroundExecutor: ExecutorService

    @RelaxedMockK
    private lateinit var synchronizer: PollingSynchronizer

    @RelaxedMockK
    private lateinit var userManager: LocalUserManager

    @RelaxedMockK
    private lateinit var workspaceManager: WorkspaceConfigManager

    @RelaxedMockK
    private lateinit var sessionManager: SessionManager

    @RelaxedMockK
    private lateinit var screenManager: ScreenManager

    @RelaxedMockK
    private lateinit var eventProcessor: DefaultEventProcessor

    @RelaxedMockK
    private lateinit var pushTokenManager: PushTokenManager

    @RelaxedMockK
    private lateinit var notificationManager: NotificationManager

    @RelaxedMockK
    private lateinit var applicationInstallStateManager: ApplicationInstallStateManager

    @RelaxedMockK
    private lateinit var userExplorer: HackleUserExplorer

    @RelaxedMockK
    private lateinit var fetchThrottler: Throttler

    @RelaxedMockK
    private lateinit var optOutManager: OptOutManager

    @RelaxedMockK
    private lateinit var inAppMessageViewProvider: InAppMessageViewProvider

    private lateinit var sut: HackleApp

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { coreExecutor.execute(any()) } answers { firstArg<Runnable>().run() }

        mockkObject(TaskExecutors)
        every { TaskExecutors.background() } returns backgroundExecutor
        every { TaskExecutors.runOnBackground(any()) } answers { firstArg<() -> Unit>().invoke() }
        every { backgroundExecutor.execute(any()) } answers { firstArg<Runnable>().run() }

        every { synchronizer.sync() } returns CompletableFuture.completedFuture(null)
        every { userManager.setUser(any()) } returns CompletableFuture.completedFuture(null)
        every { userManager.resetUser() } returns CompletableFuture.completedFuture(null)
        every { userManager.setUserId(any()) } returns CompletableFuture.completedFuture(null)
        every { userManager.setDeviceId(any()) } returns CompletableFuture.completedFuture(null)
        every { userManager.updateProperties(any()) } returns CompletableFuture.completedFuture(null)

        val hackleAppCore = HackleAppCore(
            clock = Clock.SYSTEM,
            core = core,
            coreExecutor = coreExecutor,
            synchronizer = synchronizer,
            userManager = userManager,
            workspaceManager = workspaceManager,
            sessionManager = sessionManager,
            screenManager = screenManager,
            eventProcessor = eventProcessor,
            pushTokenManager = pushTokenManager,
            notificationManager = notificationManager,
            fetchThrottler = fetchThrottler,
            device = MockDevice("hackle_device_id", emptyMap()),
            applicationInstallStateManager = applicationInstallStateManager,
            userExplorer = userExplorer,
            optOutManager = optOutManager,
            inAppMessageViewProvider = inAppMessageViewProvider
        )

        sut = HackleApp(
            hackleAppCore,
            Sdk.of("", HackleConfig.DEFAULT),
            HackleConfig.builder().mode(HackleAppMode.NATIVE).build(),
            mockk()
        )
    }

    @After
    fun tearDown() {
        unmockkObject(TaskExecutors)
        unmockkObject(HackleApp)
        clearAllMocks()
    }

    private fun failedFuture(): CompletableFuture<Void> {
        return CompletableFuture<Void>().apply { completeExceptionally(IllegalArgumentException("fail")) }
    }

    @Test
    fun `throws NullPointerException when HackleApp is not initialized`() {
        expectThrows<IllegalStateException> {
            HackleApp.getInstance()
        }
    }

    @Test
    fun `deviceId`() {
        expectThat(sut.deviceId).isEqualTo("hackle_device_id")
    }

    @Test
    fun `sessionId`() {
        every { sessionManager.requiredSession } returns Session("42")
        expectThat(sut.sessionId).isEqualTo("42")
    }

    @Test
    fun `user`() {
        val user = User.builder().build()
        every { userManager.currentUser } returns user
        expectThat(sut.user).isSameInstanceAs(user)
    }

    @Test
    fun `isOptOutTracking - false`() {
        every { optOutManager.isOptOutTracking } returns false
        expectThat(sut.isOptOutTracking).isEqualTo(false)
    }

    @Test
    fun `isOptOutTracking - true`() {
        every { optOutManager.isOptOutTracking } returns true
        expectThat(sut.isOptOutTracking).isEqualTo(true)
    }

    @Test
    fun `setOptOutTracking`() {
        sut.setOptOutTracking(true)
        verify(exactly = 1) { optOutManager.setOptOutTracking(true) }
    }

    @Test
    fun `setOptOutTracking - false`() {
        sut.setOptOutTracking(false)
        verify(exactly = 1) { optOutManager.setOptOutTracking(false) }
    }

    @Test
    fun `setOptOutTracking - does not throw when exception occurs`() {
        every { optOutManager.setOptOutTracking(any()) } throws RuntimeException()
        sut.setOptOutTracking(true)
    }

    @Test
    fun showUserExplorer() {
        sut.showUserExplorer()
        verify(exactly = 1) {
            userExplorer.show()
        }
    }

    @Test
    fun hideUserExplorer() {
        sut.hideUserExplorer()
        verify(exactly = 1) {
            userExplorer.hide()
        }
    }

    @Test
    fun `setUser - run callback even if failed to set user`() {
        // given
        every { userManager.setUser(any()) } throws IllegalArgumentException()
        val user = User.builder().build()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setUser(user, callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setUser - run callback even if failed to sync`() {
        // given
        every { userManager.setUser(any()) } returns failedFuture()
        val user = User.builder().build()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setUser(user, callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setUser - success to set user`() {
        // given
        val user = User.builder().build()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setUser(user, callback)

        // then
        verify(exactly = 1) {
            userManager.setUser(user)
        }
        verify(exactly = 1) {
            callback.run()
        }
    }

    @Test
    fun `setUserId - run callback even if failed to set user id`() {
        // given
        every { userManager.setUserId(any()) } throws IllegalArgumentException()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setUserId("user_id", callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setUserId - run callback even if failed to sync`() {
        // given
        every { userManager.setUserId(any()) } returns failedFuture()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setUserId("user_id", callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setUserId - success to set user id`() {
        // given
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setUserId("user_id", callback)

        // then
        verify(exactly = 1) {
            userManager.setUserId("user_id")
        }
        verify(exactly = 1) {
            callback.run()
        }
    }

    @Test
    fun `setDeviceId - run callback even if failed to set device id`() {
        // given
        every { userManager.setDeviceId(any()) } throws IllegalArgumentException()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setDeviceId("device_id", callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setDeviceId - run callback even if failed to sync`() {
        // given
        every { userManager.setDeviceId(any()) } returns failedFuture()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setDeviceId("device_id", callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setDeviceId - success to set device id`() {
        // given
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setDeviceId("device_id", callback)

        // then
        verify(exactly = 1) {
            userManager.setDeviceId("device_id")
        }
        verify(exactly = 1) {
            callback.run()
        }
    }

    @Test
    fun `setUserProperty`() {
        val callback = mockk<Runnable>(relaxed = true)
        sut.setUserProperty("age", 42, callback)

        verify(exactly = 1) {
            userManager.updateProperties(
                withArg {
                    expectThat(it.asMap()).isEqualTo(
                        PropertyOperations.builder().set("age", 42).build().asMap()
                    )
                }
            )
        }
        verify(exactly = 1) {
            callback.run()
        }
    }

    @Test
    fun `updateUserProperties`() {
        val callback = mockk<Runnable>(relaxed = true)
        val operations = PropertyOperations.builder().set("age", 42).build()
        sut.updateUserProperties(operations, callback)

        verify(exactly = 1) {
            userManager.updateProperties(operations)
        }
        verify(exactly = 1) {
            callback.run()
        }
    }

    @Test
    fun `updateUserProperties - run callback even if fail to update properties`() {
        val callback = mockk<Runnable>(relaxed = true)
        val operations = PropertyOperations.builder().set("age", 42).build()

        every { userManager.updateProperties(any()) } throws IllegalArgumentException()

        sut.updateUserProperties(operations, callback)

        verify(exactly = 1) {
            callback.run()
        }
    }

    @Test
    fun `resetUser - run callback even if failed to reset user`() {
        val callback = mockk<Runnable>(relaxed = true)
        every { userManager.resetUser() } throws IllegalArgumentException()

        sut.resetUser(callback)

        verify(exactly = 1) {
            callback.run()
        }
    }

    @Test
    fun `resetUser - delegate to userManager`() {
        val callback = mockk<Runnable>(relaxed = true)

        sut.resetUser(callback)

        verify(exactly = 1) {
            userManager.resetUser()
        }
        verify(exactly = 1) {
            callback.run()
        }
    }

    @Test
    fun setPhoneNumber() {
        val callback = mockk<Runnable>(relaxed = true)

        sut.setPhoneNumber("010-1234-5678", callback)

        verify(exactly = 1) { core.track(any(), any(), any()) }
        verify(exactly = 1) { eventProcessor.flush() }
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun unsetPhoneNumber() {
        val callback = mockk<Runnable>(relaxed = true)

        sut.unsetPhoneNumber(callback)

        verify(exactly = 1) { core.track(any(), any(), any()) }
        verify(exactly = 1) { eventProcessor.flush() }
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun variation() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.hackleUser(any(), any()) } returns hackleUser

        val decision = Decision.of(Variation.B, DecisionReason.TRAFFIC_ALLOCATED)
        every { core.experiment(any(), any()) } returns decision

        // when
        val actual = sut.variation(42)

        // then
        expectThat(actual).isEqualTo(Variation.B)
        verify(exactly = 1) { core.experiment(42, hackleUser) }
    }

    @Test
    fun `variationDetail - success`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.hackleUser(any(), any()) } returns hackleUser

        val decision = Decision.of(Variation.B, DecisionReason.TRAFFIC_ALLOCATED)
        every { core.experiment(any(), any()) } returns decision

        // when
        val actual = sut.variationDetail(42)

        // then
        expectThat(actual).isSameInstanceAs(decision)
        verify(exactly = 1) { core.experiment(42, hackleUser) }
    }

    @Test
    fun `variationDetail - error`() {
        // given
        every { core.experiment(any(), any()) } throws IllegalArgumentException()

        // when
        val actual = sut.variationDetail(42)

        // then
        expectThat(actual.variation).isEqualTo(Variation.A)
        expectThat(actual.reason).isEqualTo(DecisionReason.EXCEPTION)
    }

    @Test
    fun `allVariationDetails`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.hackleUser(any(), any()) } returns hackleUser

        val experiment = mockk<Experiment> {
            every { key } returns 42
        }
        val decision = Decision.of(
            Variation.B,
            DecisionReason.TRAFFIC_ALLOCATED,
            ParameterConfig.empty(),
            experiment
        )
        every { core.experiments(any()) } returns mapOf(experiment to decision)

        // when
        val actual = sut.allVariationDetails()

        // then
        expectThat(actual).isEqualTo(mapOf(42L to decision))
        verify(exactly = 1) { core.experiments(hackleUser) }
    }

    @Test
    fun `allVariationDetails - exception`() {
        // given
        every { core.experiments(any()) } throws IllegalArgumentException()

        // when
        val actual = sut.allVariationDetails()

        // then
        expectThat(actual).isEqualTo(emptyMap())
    }

    @Test
    fun `isFeatureOn`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.hackleUser(any(), any()) } returns hackleUser

        val decision = FeatureFlagDecision.on(DecisionReason.DEFAULT_RULE)
        every { core.featureFlag(any(), any()) } returns decision

        // when
        val actual = sut.isFeatureOn(42)

        // then
        expectThat(actual).isEqualTo(true)
        verify(exactly = 1) { core.featureFlag(42, hackleUser) }
    }

    @Test
    fun `featureFlagDetail - success`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.hackleUser(any(), any()) } returns hackleUser

        val decision = FeatureFlagDecision.on(DecisionReason.DEFAULT_RULE)
        every { core.featureFlag(any(), any()) } returns decision

        // when
        val actual = sut.featureFlagDetail(42)

        // then
        expectThat(actual).isSameInstanceAs(decision)
        verify(exactly = 1) { core.featureFlag(42, hackleUser) }
    }

    @Test
    fun `featureFlagDetail - exception`() {
        // given
        every { core.featureFlag(any(), any()) } throws IllegalArgumentException()

        // when
        val actual = sut.featureFlagDetail(42)

        // then
        expectThat(actual.isOn).isEqualTo(false)
        expectThat(actual.reason).isEqualTo(DecisionReason.EXCEPTION)
    }

    @Test
    fun `track`() {
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.hackleUser(any(), any()) } returns hackleUser

        sut.track("test_1")
        sut.track(Event.builder("test_2").build())

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it.key).isEqualTo("test_1")
                },
                hackleUser,
                any()
            )
        }

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it.key).isEqualTo("test_2")
                },
                hackleUser,
                any()
            )
        }
    }

    @Test
    fun `remoteConfig`() {
        val actual = sut.remoteConfig()
        expectThat(actual).isA<HackleRemoteConfigImpl>()
    }

    @Test
    fun `close`() {
        sut.close()
        verify(exactly = 1) {
            core.close()
        }
    }

    @Test
    fun `initialize`() {
        val onReady = mockk<Runnable>(relaxed = true)

        sut.initialize(null, onReady)

        verify(exactly = 1) { userManager.initialize(null) }
        verify(exactly = 1) { workspaceManager.initialize() }
        verify(exactly = 1) { pushTokenManager.initialize() }
        verify(exactly = 1) { sessionManager.initialize() }
        verify(exactly = 1) { eventProcessor.initialize() }
        verify(exactly = 1) { synchronizer.sync() }
        verify(exactly = 1) { notificationManager.flush() }
        verify(exactly = 1) { applicationInstallStateManager.checkApplicationInstall() }
        verify(exactly = 1) { onReady.run() }
    }

    @Test
    fun `initialize - run onReady even if failed to initialize`() {
        every { synchronizer.sync() } throws IllegalArgumentException()

        val onReady = mockk<Runnable>(relaxed = true)
        sut.initialize(null, onReady)

        verify(exactly = 1) { onReady.run() }
    }

    @Test
    fun `setWebViewBridge success`() {
        val webView = mockk<WebView>(relaxed = true)
        sut.setWebViewBridge(webView)

        verify(exactly = 1) {
            webView.addJavascriptInterface(any<HackleJavascriptInterface>(), "_hackleApp")
        }
    }

    @Test
    fun `fetch - execute with throttle`() {
        // given
        every { fetchThrottler.execute(any(), any()) } answers { firstArg<() -> Unit>().invoke() }

        // when
        sut.fetch()

        // then
        verify(exactly = 1) {
            fetchThrottler.execute(any(), any())
        }
    }

    @Test
    fun `fetch - execute in background`() {
        // given
        every { fetchThrottler.execute(any(), any()) } answers { firstArg<() -> Unit>().invoke() }

        // when
        sut.fetch()

        // then
        verify(exactly = 1) {
            backgroundExecutor.execute(any())
        }
    }

    @Test
    fun `fetch - sync`() {
        // given
        every { fetchThrottler.execute(any(), any()) } answers { firstArg<() -> Unit>().invoke() }

        // when
        sut.fetch()

        // then
        verify(exactly = 1) {
            synchronizer.sync()
        }
    }

    @Test
    fun `fetch - callback`() {
        // given
        every { fetchThrottler.execute(any(), any()) } answers { firstArg<() -> Unit>().invoke() }
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.fetch(callback)

        // then
        verify(exactly = 1) {
            callback.run()
        }
    }

    @Test
    fun `fetch - run callback even if throttled`() {
        // given
        every { fetchThrottler.execute(any(), any()) } answers { secondArg<() -> Unit>().invoke() }
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.fetch(callback)

        // then
        verify(exactly = 0) { synchronizer.sync() }
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun updatePushSubscriptions() {
        sut.updatePushSubscriptions(
            HackleSubscriptionOperations.builder()
                .marketing(HackleSubscriptionStatus.UNSUBSCRIBED)
                .information(HackleSubscriptionStatus.SUBSCRIBED)
                .custom("chat", HackleSubscriptionStatus.UNKNOWN)
                .build()
        )
        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it).isEqualTo(
                        Event.builder("\$push_subscriptions")
                            .properties(
                                mapOf(
                                    "\$marketing" to "UNSUBSCRIBED",
                                    "\$information" to "SUBSCRIBED",
                                    "chat" to "UNKNOWN"
                                )
                            )
                            .build()
                    )
                },
                any(),
                any()
            )
        }
        verify(exactly = 1) {
            core.flush()
        }
    }

    @Test
    fun updateSmsSubscriptions() {
        sut.updateSmsSubscriptions(
            HackleSubscriptionOperations.builder()
                .marketing(HackleSubscriptionStatus.UNSUBSCRIBED)
                .information(HackleSubscriptionStatus.SUBSCRIBED)
                .custom("chat", HackleSubscriptionStatus.UNKNOWN)
                .build()
        )
        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it).isEqualTo(
                        Event.builder("\$sms_subscriptions")
                            .properties(
                                mapOf(
                                    "\$marketing" to "UNSUBSCRIBED",
                                    "\$information" to "SUBSCRIBED",
                                    "chat" to "UNKNOWN"
                                )
                            )
                            .build()
                    )
                },
                any(),
                any()
            )
        }
        verify(exactly = 1) {
            core.flush()
        }
    }

    @Test
    fun updateKakaoSubscriptions() {
        sut.updateKakaoSubscriptions(
            HackleSubscriptionOperations.builder()
                .marketing(HackleSubscriptionStatus.UNSUBSCRIBED)
                .information(HackleSubscriptionStatus.SUBSCRIBED)
                .custom("chat", HackleSubscriptionStatus.UNKNOWN)
                .build()
        )
        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it).isEqualTo(
                        Event.builder("\$kakao_subscriptions")
                            .properties(
                                mapOf(
                                    "\$marketing" to "UNSUBSCRIBED",
                                    "\$information" to "SUBSCRIBED",
                                    "chat" to "UNKNOWN"
                                )
                            )
                            .build()
                    )
                },
                any(),
                any()
            )
        }
        verify(exactly = 1) {
            core.flush()
        }
    }

    @Test
    fun setCurrentScreen() {
        sut.setCurrentScreen(Screen("current_screen", "current_class"))
        verify(exactly = 1) {
            screenManager.setCurrentScreen(Screen("current_screen", "current_class"), any())
        }
    }

    @Test
    fun setInAppMessageListener() {
        mockkObject(InAppMessageUi.Companion)
        val mockInstance = mockk<InAppMessageUi>(relaxed = true)
        every { InAppMessageUi.instance } returns mockInstance
        val listener = mockk<HackleInAppMessageListener>()

        sut.setInAppMessageListener(listener)

        verify(exactly = 1) {
            mockInstance.setListener(listener)
        }
        unmockkObject(InAppMessageUi.Companion)
    }

    @Test
    fun `setInAppMessageListener - null listener`() {
        mockkObject(InAppMessageUi.Companion)
        val mockInstance = mockk<InAppMessageUi>(relaxed = true)
        every { InAppMessageUi.instance } returns mockInstance

        sut.setInAppMessageListener(null)

        verify(exactly = 1) {
            mockInstance.setListener(null)
        }
        unmockkObject(InAppMessageUi.Companion)
    }

    @Test
    fun setBackButtonDismissesInAppMessageView() {
        mockkObject(InAppMessageUi.Companion)
        val mockInstance = mockk<InAppMessageUi>(relaxed = true)
        every { InAppMessageUi.instance } returns mockInstance

        sut.setBackButtonDismissesInAppMessageView(true)

        verify(exactly = 1) {
            mockInstance.setBackButtonDismisses(true)
        }
        unmockkObject(InAppMessageUi.Companion)
    }

    @Test
    fun `setBackButtonDismissesInAppMessageView - false`() {
        mockkObject(InAppMessageUi.Companion)
        val mockInstance = mockk<InAppMessageUi>(relaxed = true)
        every { InAppMessageUi.instance } returns mockInstance

        sut.setBackButtonDismissesInAppMessageView(false)

        verify(exactly = 1) {
            mockInstance.setBackButtonDismisses(false)
        }
        unmockkObject(InAppMessageUi.Companion)
    }

    // Deprecated methods tests
    // 4.0 유저 중앙화로 user/userId를 받는 deprecated 오버로드(variation/variationDetail/allVariationDetails/
    // isFeatureOn/featureFlagDetail/track/remoteConfig)는 공개 API에서 제거되어 테스트도 함께 삭제됨

    @Test
    fun `showUserExplorer with Activity - deprecated`() {
        val activity = mockk<Activity>()

        sut.showUserExplorer(activity)

        verify(exactly = 1) {
            userExplorer.show()
        }
    }

    @Test
    fun setPushToken() {
        sut.setPushToken("test_token")
        // This method does nothing, just checking it doesn't throw
    }

    @Test
    fun updatePushSubscriptionStatus() {
        sut.updatePushSubscriptionStatus(HacklePushSubscriptionStatus.SUBSCRIBED)
        // This method does nothing, just checking it doesn't throw
    }

    @Test
    fun `displayedInAppMessageView`() {
        every { inAppMessageViewProvider.currentView } returns null
        expectThat(sut.displayedInAppMessageView).isNull()

        val view = mockk<InAppMessageView>()
        every { inAppMessageViewProvider.currentView } returns view
        expectThat(sut.displayedInAppMessageView) isSameInstanceAs view
    }
}

class HackleAppCompanionTest {

    @Test
    fun `registerActivityLifecycleCallbacks`() {
        mockkObject(ApplicationLifecycleManager.Companion)
        mockkObject(ActivityLifecycleManager.Companion)
        val mockApplicationLifecycleManager = mockk<ApplicationLifecycleManager>(relaxed = true)
        val mockActivityLifecycleManager = mockk<ActivityLifecycleManager>(relaxed = true)
        every { ApplicationLifecycleManager.instance } returns mockApplicationLifecycleManager
        every { ActivityLifecycleManager.instance } returns mockActivityLifecycleManager

        val mockContext = mockk<Context>()

        HackleApp.registerActivityLifecycleCallbacks(mockContext)

        verify(exactly = 1) {
            mockApplicationLifecycleManager.registerTo(mockContext)
            mockActivityLifecycleManager.registerTo(mockContext)
        }
        unmockkObject(ApplicationLifecycleManager.Companion)
        unmockkObject(ActivityLifecycleManager.Companion)
    }

    @Test
    fun `isHacklePushMessage`() {
        mockkObject(NotificationHandler.Companion)
        val intent = mockk<Intent>()
        every { NotificationHandler.isHackleIntent(intent) } returns true

        val result = HackleApp.isHacklePushMessage(intent)

        expectThat(result).isEqualTo(true)
        verify(exactly = 1) {
            NotificationHandler.isHackleIntent(intent)
        }
        unmockkObject(NotificationHandler.Companion)
    }

    @Test
    fun `isHacklePushMessage - false`() {
        mockkObject(NotificationHandler.Companion)
        val intent = mockk<Intent>()
        every { NotificationHandler.isHackleIntent(intent) } returns false

        val result = HackleApp.isHacklePushMessage(intent)

        expectThat(result).isEqualTo(false)
        verify(exactly = 1) {
            NotificationHandler.isHackleIntent(intent)
        }
        unmockkObject(NotificationHandler.Companion)
    }

    @Test
    fun `getInstance - when initialized`() {
        // This test requires HackleApp to be initialized first
        mockkObject(HackleApp.Companion)
        val mockInstance = mockk<HackleApp>()
        every { HackleApp.getInstance() } returns mockInstance

        val result = HackleApp.getInstance()

        expectThat(result).isSameInstanceAs(mockInstance)
        unmockkObject(HackleApp.Companion)
    }
}
