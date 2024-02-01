package io.hackle.android

import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.model.Sdk
import io.hackle.android.internal.notification.NotificationManager
import io.hackle.android.internal.remoteconfig.HackleRemoteConfigImpl
import io.hackle.android.internal.session.Session
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.sync.PollingSynchronizer
import io.hackle.android.internal.user.UserManager
import io.hackle.android.mock.MockDevice
import io.hackle.android.ui.explorer.HackleUserExplorer
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.ParameterConfig
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.common.Variation
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

class HackleAppTest {

    @RelaxedMockK
    private lateinit var core: HackleCore

    @RelaxedMockK
    private lateinit var eventExecutor: Executor

    @RelaxedMockK
    private lateinit var backgroundExecutor: ExecutorService

    @RelaxedMockK
    private lateinit var synchronizer: PollingSynchronizer

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var sessionManager: SessionManager

    @RelaxedMockK
    private lateinit var eventProcessor: DefaultEventProcessor

    @RelaxedMockK
    private lateinit var notificationManager: NotificationManager

    @RelaxedMockK
    private lateinit var userExplorer: HackleUserExplorer

    private lateinit var sut: HackleApp

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { eventExecutor.execute(any()) } answers { firstArg<Runnable>().run() }
        every { backgroundExecutor.submit(any()) } answers {
            firstArg<Runnable>().run()
            CompletableFuture.completedFuture(null)
        }

        sut = HackleApp(
            Clock.SYSTEM,
            core,
            eventExecutor,
            backgroundExecutor,
            synchronizer,
            userManager,
            sessionManager,
            eventProcessor,
            notificationManager,
            MockDevice("hackle_device_id", emptyMap()),
            userExplorer,
            Sdk.of("", HackleConfig.DEFAULT)
        )
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
    fun `showUserExplorer`() {
        sut.showUserExplorer()
        verify(exactly = 1) {
            userExplorer.show()
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
    fun `setUser - run callback event if failed to submit sync`() {
        // given
        every { backgroundExecutor.submit(any()) } throws RejectedExecutionException()
        val user = User.builder().build()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setUser(user, callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setUser - run callback event if failed to sync`() {
        // given
        every { synchronizer.sync(any(), any()) } throws IllegalArgumentException()
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
        every { userManager.setUser(any()) } throws IllegalArgumentException()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setUserId("user_id", callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setUserId - run callback event if failed to submit sync`() {
        // given
        every { backgroundExecutor.submit(any()) } throws RejectedExecutionException()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setUserId("user_id", callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setUserId - run callback event if failed to sync`() {
        // given
        every { synchronizer.sync(any(), any()) } throws IllegalArgumentException()
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
    fun `setDeviceId - run callback even if failed to set device id`() {
        // given
        every { userManager.setUser(any()) } throws IllegalArgumentException()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setDeviceId("device_id", callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setDeviceId - run callback event if failed to submit sync`() {
        // given
        every { backgroundExecutor.submit(any()) } throws RejectedExecutionException()
        val callback = mockk<Runnable>(relaxed = true)

        // when
        sut.setDeviceId("device_id", callback)

        // then
        verify(exactly = 1) { callback.run() }
    }

    @Test
    fun `setDeviceId - run callback event if failed to sync`() {
        // given
        every { synchronizer.sync(any(), any()) } throws IllegalArgumentException()
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
            core.track(
                withArg {
                    expectThat(it).isEqualTo(
                        Event.builder("\$properties")
                            .property("\$set", mapOf("age" to 42))
                            .build()
                    )
                },
                any(),
                any()
            )
        }

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
            core.track(
                withArg {
                    expectThat(it).isEqualTo(
                        Event.builder("\$properties")
                            .property("\$set", mapOf("age" to 42))
                            .build()
                    )
                },
                any(),
                any()
            )
        }

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
    fun `resetUser - clear properties`() {
        sut.resetUser()

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it).isEqualTo(
                        Event.builder("\$properties")
                            .property("\$clearAll", mapOf("clearAll" to "-"))
                            .build()
                    )
                },
                any(),
                any()
            )
        }
    }

    @Test
    fun `variation`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.resolve(any()) } returns hackleUser

        val decision = Decision.of(Variation.B, DecisionReason.TRAFFIC_ALLOCATED)
        every { core.experiment(any(), any(), any()) } returns decision

        // when
        val actual = sut.variation(42)

        // then
        expectThat(actual).isEqualTo(Variation.B)
        verify(exactly = 1) { userManager.resolve(null) }
    }

    @Test
    fun `variationDetail - success`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.resolve(any()) } returns hackleUser

        val decision = Decision.of(Variation.B, DecisionReason.TRAFFIC_ALLOCATED)
        every { core.experiment(any(), any(), any()) } returns decision

        // when
        val actual = sut.variationDetail(42)

        // then
        expectThat(actual).isSameInstanceAs(decision)
        verify(exactly = 1) { userManager.resolve(null) }
    }

    @Test
    fun `variationDetail - error`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.resolve(any()) } returns hackleUser

        every { core.experiment(any(), any(), any()) } throws IllegalArgumentException()

        // when
        val actual = sut.variationDetail(42)

        // then
        expectThat(actual.variation).isEqualTo(Variation.A)
        expectThat(actual.reason).isEqualTo(DecisionReason.EXCEPTION)
        verify(exactly = 1) { userManager.resolve(null) }
    }

    @Test
    fun `allVariationDetails`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.resolve(any()) } returns hackleUser

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
        verify(exactly = 1) { userManager.resolve(null) }
    }


    @Test
    fun `allVariationDetails - exception`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.resolve(any()) } returns hackleUser

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
        every { userManager.resolve(any()) } returns hackleUser

        val decision = FeatureFlagDecision.on(DecisionReason.DEFAULT_RULE)
        every { core.featureFlag(any(), any()) } returns decision

        // when
        val actual = sut.isFeatureOn(42)

        // then
        expectThat(actual).isEqualTo(true)
        verify(exactly = 1) { userManager.resolve(null) }
    }

    @Test
    fun `featureFlagDetail - success`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.resolve(any()) } returns hackleUser

        val decision = FeatureFlagDecision.on(DecisionReason.DEFAULT_RULE)
        every { core.featureFlag(any(), any()) } returns decision

        // when
        val actual = sut.featureFlagDetail(42)

        // then
        expectThat(actual).isSameInstanceAs(decision)
        verify(exactly = 1) { userManager.resolve(null) }
    }

    @Test
    fun `featureFlagDetail - exception`() {
        // given
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.resolve(any()) } returns hackleUser

        every { core.featureFlag(any(), any()) } throws IllegalArgumentException()

        // when
        val actual = sut.featureFlagDetail(42)

        // then
        expectThat(actual.isOn).isEqualTo(false)
        expectThat(actual.reason).isEqualTo(DecisionReason.EXCEPTION)
        verify(exactly = 1) { userManager.resolve(null) }
    }

    @Test
    fun `track`() {
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "42").build()
        every { userManager.resolve(any()) } returns hackleUser

        sut.track("test_1")
        sut.track(Event.builder("test_2").build())

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it.key).isEqualTo("test_1")
                },
                any(),
                any()
            )
        }

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it.key).isEqualTo("test_2")
                },
                any(),
                any()
            )
        }
        verify(exactly = 2) { userManager.resolve(null) }
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
        verify(exactly = 1) { sessionManager.initialize() }
        verify(exactly = 1) { eventProcessor.initialize() }
        verify(exactly = 1) { synchronizer.sync(null) }
        verify(exactly = 1) { onReady.run() }
    }

    @Test
    fun `initialize - run onReady even if failed to initialize`() {
        every { synchronizer.sync() } throws IllegalArgumentException()

        val onReady = mockk<Runnable>(relaxed = true)
        sut.initialize(null, onReady)

        verify(exactly = 1) { onReady.run() }
    }
}