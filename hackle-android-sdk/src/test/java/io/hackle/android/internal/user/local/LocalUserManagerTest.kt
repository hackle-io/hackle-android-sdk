package io.hackle.android.internal.user.local

import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.user.UserListener
import io.hackle.android.internal.user.rawEvents
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.hackle.android.mock.MockUserRepository
import io.hackle.sdk.common.PropertyOperation
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.model.Cohort
import io.hackle.sdk.core.model.Identifier
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.TargetEvent
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*
import java.util.concurrent.CompletableFuture

class LocalUserManagerTest {

    private lateinit var repository: MockUserRepository
    private lateinit var cohortFetcher: UserCohortFetcher
    private lateinit var targetEventFetcher: UserTargetEventFetcher
    private lateinit var sut: LocalUserManager

    private lateinit var listener: UserListener

    @Before
    fun before() {
        repository = MockUserRepository()
        cohortFetcher = mockk()
        targetEventFetcher = mockk()
        sut = LocalUserManager(
            Clock.SYSTEM,
            MockDevice("hackle_device_id", emptyMap()),
            MockPackageInfo(PackageVersionInfo("1.0.0", 1L)),
            repository,
            cohortFetcher,
            targetEventFetcher
        )

        listener = mockk(relaxed = true)
        sut.addListener(listener)

        every { cohortFetcher.fetch(any()) } returns CompletableFuture.completedFuture(UserCohorts.empty())
        every { targetEventFetcher.fetch(any()) } returns CompletableFuture.completedFuture(UserTargetEvents.empty())
    }

    @Test
    fun `initialize - with default user`() {
        sut.initialize(null)
        val user = sut.currentUser
        expectThat(user) isEqualTo User.builder()
            .id("hackle_device_id")
            .deviceId("hackle_device_id")
            .build()
    }

    @Test
    fun `initialize - with saved user`() {
        repository.set(
            User.builder()
                .deviceId("saved_device_id")
                .userId("saved_user_id")
                .build()
        )
        sut.initialize(null)
        val user = sut.currentUser
        expectThat(user) isEqualTo User.builder()
            .id("hackle_device_id")
            .deviceId("saved_device_id")
            .userId("saved_user_id")
            .build()
    }

    @Test
    fun `initialize - init saved user`() {
        repository.set(
            User.builder()
                .deviceId("saved_device_id")
                .userId("saved_user_id")
                .build()
        )
        sut.initialize(
            User.builder()
                .deviceId("init_device_id")
                .userId("init_user_id")
                .build()
        )
        val user = sut.currentUser
        expectThat(user) isEqualTo User.builder()
            .id("hackle_device_id")
            .deviceId("init_device_id")
            .userId("init_user_id")
            .build()
    }


    @Test
    fun `hackleUser - currentUser`() {
        sut.initialize(
            User.builder()
                .id("init_id")
                .deviceId("init_device_id")
                .userId("init_user_id")
                .build()
        )

        val actual = sut.hackleUser()
        expectThat(actual).isEqualTo(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "init_id")
                .identifier(IdentifierType.DEVICE, "init_device_id")
                .identifier(IdentifierType.USER, "init_user_id")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .build()
        )
    }

    @Test
    fun `hackleUser - inputUser`() {
        sut.initialize(null)

        val actual = sut.hackleUser(User.builder().id("input_id").build())
        expectThat(actual).isEqualTo(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "input_id")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .identifier(IdentifierType.DEVICE, "hackle_device_id")
                .build()
        )
    }

    @Test
    fun `hackleUser - inputUser로 유저 상태를 변경하지 않는다`() {
        sut.initialize(null)
        val currentUser = sut.currentUser

        sut.hackleUser(User.builder().id("input_id").userId("input_user_id").build())

        expectThat(sut.currentUser) isEqualTo currentUser
        verify { listener wasNot Called }
    }

    @Test
    fun `hackleUser - merge with current context`() {
        // given
        val userCohorts = UserCohorts.builder()
            .put(
                UserCohort(
                    Identifier("\$id", "id"),
                    listOf(Cohort(42))
                )
            )
            .build()
        val userTargetEvents = UserTargetEvents.builder()
            .build()
        every { cohortFetcher.fetch(any()) } returns CompletableFuture.completedFuture(userCohorts)
        every { targetEventFetcher.fetch(any()) } returns CompletableFuture.completedFuture(userTargetEvents)

        // when
        sut.initialize(User.builder().id("id").property("a", "a").build())
        sut.sync().get()
        val hackleUser =
            sut.hackleUser(User.builder().id("id").userId("user_id").property("b", "b").build())

        // then
        expectThat(hackleUser).isEqualTo(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "id")
                .identifier(IdentifierType.DEVICE, "hackle_device_id")
                .identifier(IdentifierType.USER, "user_id")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .property("b", "b")
                .cohort(Cohort(42))
                .build()
        )
    }

    @Test
    fun `hackleUser - full`() {
        val hackleUser = sut.hackleUser(
            User.builder()
                .id("id")
                .deviceId("device_id")
                .userId("user_id")
                .identifier("custom", "custom_id")
                .property("age", 42)
                .build()
        )
        expectThat(hackleUser).isEqualTo(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "id")
                .identifier(IdentifierType.DEVICE, "device_id")
                .identifier(IdentifierType.USER, "user_id")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .identifier("custom", "custom_id")
                .property("age", 42)
                .build()
        )
    }

    @Test
    fun `hackleUser - fill default id`() {
        val hackleUser = sut.hackleUser(User.builder().build())
        expectThat(hackleUser).isEqualTo(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "hackle_device_id")
                .identifier(IdentifierType.DEVICE, "hackle_device_id")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .build()
        )
    }

    @Test
    fun `hackleUser - hackle properties`() {
        val sut = LocalUserManager(
            Clock.SYSTEM,
            MockDevice("hackle_device_id", mapOf("age" to 42)),
            MockPackageInfo(PackageVersionInfo("1.0.0", 0L)),
            repository,
            cohortFetcher,
            targetEventFetcher
        )
        val hackleUser = sut.hackleUser(User.builder().build())
        expectThat(hackleUser.hackleProperties.size).isGreaterThan(0)
    }

    @Test
    fun `sync - update userTarget`() {
        val userCohorts = UserCohorts.builder()
            .put(UserCohort(Identifier("\$id", "hackle_device_id"), listOf(Cohort(42))))
            .build()
        val userTargetEvents = UserTargetEvents.builder()
            .put(TargetEvent("purchase", listOf(TargetEvent.Stat(1738368000000, 1)), null))
            .build()
        every { cohortFetcher.fetch(any()) } returns CompletableFuture.completedFuture(userCohorts)
        every { targetEventFetcher.fetch(any()) } returns CompletableFuture.completedFuture(userTargetEvents)

        sut.initialize(null)
        expectThat(sut.hackleUser().cohorts).hasSize(0)
        expectThat(sut.hackleUser().targetEvents).hasSize(0)

        sut.sync().get()
        expectThat(sut.hackleUser().cohorts).isEqualTo(listOf(Cohort(42)))
        expectThat(sut.hackleUser().targetEvents).isEqualTo(
            listOf(TargetEvent("purchase", listOf(TargetEvent.Stat(1738368000000, 1)), null))
        )
    }

    @Test
    fun `sync - when error on fetch userTarget then do not update userTarget`() {
        val failedFuture = CompletableFuture<UserTargetEvents>()
        failedFuture.completeExceptionally(IllegalArgumentException("fail"))
        every { targetEventFetcher.fetch(any()) } returns failedFuture

        sut.initialize(null)
        expectThat(sut.hackleUser().cohorts).hasSize(0)
        expectThat(sut.hackleUser().targetEvents).hasSize(0)

        sut.sync().get()
        expectThat(sut.hackleUser().cohorts).hasSize(0)
        expectThat(sut.hackleUser().targetEvents).hasSize(0)
    }

    @Test
    fun `syncIfNeeded - when no new identifier then do not sync cohort and target event`() {
        sut.initialize(
            User.builder()
                .deviceId("device_id")
                .identifier("custom", "custom_id")
                .build()
        )

        // 동일한 유저 - cohort not sync and target event not sync
        sut.setUser(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .identifier("custom", "custom_id")
                .build()
        ).get()

        // custom identifier 제거 - 새로운 식별자가 없으므로 cohort not sync and target event not sync
        sut.setUser(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        ).get()

        // properties만 변경 - cohort not sync and target event not sync
        sut.updateProperties(PropertyOperations.builder().set("a", 1).build()).get()

        verify { cohortFetcher wasNot Called }
        verify { targetEventFetcher wasNot Called }
    }

    @Test
    fun `syncIfNeeded - when has new identifier then sync cohort and target event`() {
        sut.initialize(null)

        // 새로운 custom identifier - cohort sync, userId/deviceId 동일하므로 target event not sync
        sut.setUser(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .identifier("custom", "custom_id")
                .build()
        ).get()
        verify(exactly = 1) { cohortFetcher.fetch(any()) }
        verify(exactly = 0) { targetEventFetcher.fetch(any()) }

        // userId 변경 - cohort sync and target event sync
        sut.setUserId("user_id").get()
        verify(exactly = 2) { cohortFetcher.fetch(any()) }
        verify(exactly = 1) { targetEventFetcher.fetch(any()) }

        // deviceId 변경 - cohort sync and target event sync
        sut.setDeviceId("device_id_2").get()
        verify(exactly = 3) { cohortFetcher.fetch(any()) }
        verify(exactly = 2) { targetEventFetcher.fetch(any()) }
    }

    @Test
    fun `syncIfNeeded - userId가 제거되면 새로운 식별자가 없으므로 targetEvent만 동기화한다`() {
        sut.initialize(User.builder().userId("user_id").build())

        sut.setUserId(null).get()

        verify { cohortFetcher wasNot Called }
        verify(exactly = 1) { targetEventFetcher.fetch(any()) }
    }

    @Test
    fun `when sync target event, overwrite`() {
        val targetEvent = TargetEvent(
            "purchase",
            listOf(
                TargetEvent.Stat(1737361789000, 10),
                TargetEvent.Stat(1737361790000, 20),
                TargetEvent.Stat(1737361793000, 30)
            ),
            TargetEvent.Property(
                "product_name",
                Target.Key.Type.EVENT_PROPERTY,
                "shampoo"
            )
        )
        val targetEvent2 = TargetEvent(
            "login",
            listOf(
                TargetEvent.Stat(1737361789000, 1),
                TargetEvent.Stat(1737361790000, 2),
                TargetEvent.Stat(1737361793000, 3)
            ),
            TargetEvent.Property(
                "grade",
                Target.Key.Type.EVENT_PROPERTY,
                "silver"
            )
        )
        val userTargetEvents = UserTargetEvents.builder()
            .put(targetEvent)
            .put(targetEvent2)
            .build()
        every { targetEventFetcher.fetch(any()) } returns CompletableFuture.completedFuture(userTargetEvents)
        sut.initialize(null)
        sut.sync().get()

        expectThat(sut.hackleUser().targetEvents).isEqualTo(userTargetEvents.rawEvents())

        val newTargetEvents = UserTargetEvents.builder()
            .put(targetEvent)
            .build()
        every { targetEventFetcher.fetch(any()) } returns CompletableFuture.completedFuture(newTargetEvents)
        sut.sync().get()

        expectThat(sut.hackleUser().targetEvents).isNotEqualTo(userTargetEvents.rawEvents())
        expectThat(sut.hackleUser().targetEvents).isEqualTo(newTargetEvents.rawEvents())
    }

    @Test
    fun `setUser - decorate hackleDeviceId`() {
        sut.setUser(User.builder().build()).get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .build()
        )
    }

    @Test
    fun `setUser - defaultUser to deviceId`() {
        sut.initialize(null)
        val initUser = User.builder()
            .id("hackle_device_id")
            .deviceId("hackle_device_id")
            .build()
        expectThat(sut.currentUser).isEqualTo(initUser)

        sut.setUser(User.builder().deviceId("device_id").build()).get()

        val currentUser = User.builder()
            .id("hackle_device_id")
            .deviceId("device_id")
            .build()
        expectThat(sut.currentUser).isEqualTo(currentUser)
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("hackle_device_id")
                    .build(),
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .build(),
                any()
            )
        }
    }

    @Test
    fun `setUser - defaultUser to deviceId, userId`() {
        sut.initialize(null)
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .build()
        )
        sut.setUser(
            User.builder()
                .deviceId("device_id")
                .userId("user_id")
                .build()
        ).get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("hackle_device_id")
                    .build(),
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .userId("user_id")
                    .build(),
                any()
            )
        }
    }

    @Test
    fun `setUser - deviceId to deviceId(diff)`() {
        sut.initialize(
            User.builder()
                .deviceId("device_id")
                .build()
        )
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
        sut.setUser(
            User.builder()
                .deviceId("device_id_2")
                .build()
        ).get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id_2")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .build(),
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id_2")
                    .build(),
                any()
            )
        }
    }

    @Test
    fun `setUser - deviceId to deviceId, userId(new)`() {
        sut.initialize(
            User.builder()
                .deviceId("device_id")
                .build()
        )
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
        sut.setUser(
            User.builder()
                .deviceId("device_id")
                .userId("user_id")
                .build()
        ).get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .build(),
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .userId("user_id")
                    .build(),
                any()
            )
        }
    }

    @Test
    fun `setUser - deviceId to deviceId(diff), userId(new)`() {
        sut.initialize(
            User.builder()
                .deviceId("device_id")
                .build()
        )
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )

        sut.setUser(
            User.builder()
                .deviceId("device_id_2")
                .userId("user_id")
                .build()
        ).get()

        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id_2")
                .userId("user_id")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .build(),
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id_2")
                    .userId("user_id")
                    .build(),
                any()
            )
        }
    }

    @Test
    fun `setUser - deviceId, userId to deviceId`() {
        sut.initialize(
            User.builder()
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )

        sut.setUser(
            User.builder()
                .deviceId("device_id")
                .build()
        ).get()

        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .userId("user_id")
                    .build(),
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .build(),
                any()
            )
        }
    }

    @Test
    fun `setUser - deviceId, userId to deviceId(diff)`() {
        sut.initialize(
            User.builder()
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )

        sut.setUser(
            User.builder()
                .deviceId("device_id_2")
                .build()
        ).get()

        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id_2")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .userId("user_id")
                    .build(),
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id_2")
                    .build(),
                any()
            )
        }
    }

    @Test
    fun `setUser - deviceId, userId to deviceId(diff), userId`() {
        sut.initialize(
            User.builder()
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )

        sut.setUser(
            User.builder()
                .deviceId("device_id_2")
                .userId("user_id")
                .build()
        ).get()

        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id_2")
                .userId("user_id")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .userId("user_id")
                    .build(),
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id_2")
                    .userId("user_id")
                    .build(),
                any()
            )
        }
    }

    @Test
    fun `setUser - deviceId, userId to deviceId, userId(diff)`() {
        sut.initialize(
            User.builder()
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .userId("user_id")
                .build()
        )

        sut.setUser(
            User.builder()
                .deviceId("device_id")
                .userId("user_id_2")
                .build()
        ).get()

        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .userId("user_id_2")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .userId("user_id")
                    .build(),
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("device_id")
                    .userId("user_id_2")
                    .build(),
                any()
            )
        }
    }

    @Test
    fun `setUser - update cohorts`() {
        val userCohorts = UserCohorts.builder()
            .put(UserCohort(Identifier("\$id", "hackle_device_id"), listOf(Cohort(42))))
            .put(UserCohort(Identifier("\$deviceId", "hackle_device_id"), listOf(Cohort(43))))
            .build()
        val userTargetEvents = UserTargetEvents.builder()
            .build()
        every { cohortFetcher.fetch(any()) } returns CompletableFuture.completedFuture(userCohorts)
        every { targetEventFetcher.fetch(any()) } returns CompletableFuture.completedFuture(userTargetEvents)

        sut.initialize(null)
        sut.sync().get()

        sut.setUser(User.builder().deviceId("device_id").build()).get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
        expectThat(sut.hackleUser()).isEqualTo(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "hackle_device_id")
                .identifier(IdentifierType.DEVICE, "device_id")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .cohort(Cohort(42))
                .build()
        )
    }

    @Test
    fun `setUser - update targetEvent`() {
        val userCohorts = UserCohorts.builder()
            .build()
        val userTargetEvents = UserTargetEvents.builder()
            .put(TargetEvent("purchase", listOf(TargetEvent.Stat(1738368000000, 1)), null))
            .build()
        every { cohortFetcher.fetch(any()) } returns CompletableFuture.completedFuture(userCohorts)
        every { targetEventFetcher.fetch(any()) } returns CompletableFuture.completedFuture(userTargetEvents)

        sut.initialize(null)
        sut.sync().get()

        sut.setUser(User.builder().deviceId("device_id").build()).get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
        expectThat(sut.hackleUser()).isEqualTo(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "hackle_device_id")
                .identifier(IdentifierType.DEVICE, "device_id")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .targetEvent(TargetEvent("purchase", listOf(TargetEvent.Stat(1738368000000, 1)), null))
                .build()
        )
    }

    @Test
    fun `updateProperties - update`() {
        sut.initialize(null)

        val operations = PropertyOperations.builder()
            .set("d", "d")
            .increment("a", 42)
            .append("c", "cc")
            .build()
        sut.updateProperties(operations).get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .property("a", 42)
                .property("c", listOf("cc"))
                .property("d", "d")
                .build()
        )
    }

    @Test
    fun `updateProperties - existed properties`() {
        sut.initialize(
            User.builder()
                .property("a", 42)
                .property("b", "b")
                .property("c", "c")
                .build()
        )

        val operations = PropertyOperations.builder()
            .set("d", "d")
            .increment("a", 42)
            .append("c", "cc")
            .build()
        sut.updateProperties(operations).get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .property("a", 84.0)
                .property("b", "b")
                .property("c", listOf("c", "cc"))
                .property("d", "d")
                .build()
        )
    }

    @Test
    fun `updateProperties - 변경 전 유저로 property 이벤트를 발행한다`() {
        sut.initialize(User.builder().property("a", 1).build())
        val userBeforeUpdate = sut.currentUser

        val operations = PropertyOperations.builder().set("b", 2).build()
        sut.updateProperties(operations).get()

        verify(exactly = 1) {
            listener.onPropertyOperations(userBeforeUpdate, operations, any())
        }
    }

    @Test
    fun `resetUser - 기본 유저로 리셋하고 리셋된 유저로 clearAll 이벤트를 발행한다`() {
        sut.initialize(
            User.builder()
                .userId("user_id")
                .property("a", 1)
                .build()
        )

        sut.resetUser().get()

        // 기본 유저는 deviceId만 갖는다. id는 hackleUser 변환 시 device id로 채워진다.
        val defaultUser = User.builder()
            .deviceId("hackle_device_id")
            .build()
        expectThat(sut.currentUser) isEqualTo defaultUser

        verify(exactly = 1) {
            listener.onPropertyOperations(
                defaultUser,
                withArg { expectThat(it.asMap().keys) isEqualTo setOf(PropertyOperation.CLEAR_ALL) },
                any()
            )
        }
        verify(exactly = 1) {
            listener.onUserUpdated(any(), defaultUser, any())
        }
    }

    @Test
    fun `setUserId - new`() {
        sut.initialize(null)
        sut.setUserId("user_id").get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .userId("user_id")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(any(), any(), any())
        }
    }

    @Test
    fun `setUserId - unset`() {
        sut.initialize(User.builder().userId("user_id").build())
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .userId("user_id")
                .build()
        )

        sut.setUserId(null).get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(any(), any(), any())
        }
    }

    @Test
    fun `setUserId - change`() {
        sut.initialize(User.builder().userId("user_id").build())
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .userId("user_id")
                .build()
        )

        sut.setUserId("user_id_2").get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .userId("user_id_2")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(any(), any(), any())
        }
    }

    @Test
    fun `setUserId - same`() {
        sut.initialize(User.builder().userId("user_id").build())
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .userId("user_id")
                .build()
        )

        sut.setUserId("user_id").get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .userId("user_id")
                .build()
        )
        verify(exactly = 0) {
            listener.onUserUpdated(any(), any(), any())
        }
    }


    @Test
    fun `setDeviceId - new`() {
        sut.initialize(null)
        sut.setDeviceId("device_id").get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(any(), any(), any())
        }
    }

    @Test
    fun `setDeviceId - change`() {
        sut.initialize(User.builder().deviceId("device_id").build())
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )

        sut.setDeviceId("device_id_2").get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id_2")
                .build()
        )
        verify(exactly = 1) {
            listener.onUserUpdated(any(), any(), any())
        }
    }

    @Test
    fun `setDeviceId - same`() {
        sut.initialize(User.builder().deviceId("device_id").build())
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )

        sut.setDeviceId("device_id").get()
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
        verify(exactly = 0) {
            listener.onUserUpdated(any(), any(), any())
        }
    }

    @Test
    fun `onChanged - foreground`() {
        sut.onForeground(42, true)
    }

    @Test
    fun `onChanged - background`() {
        expectThat(repository.get()).isNull()
        sut.onBackground(42)
        expectThat(repository.get()).isNotNull()
    }
}
