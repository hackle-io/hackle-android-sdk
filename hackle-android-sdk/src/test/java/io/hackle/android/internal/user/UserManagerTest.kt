package io.hackle.android.internal.user

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.core.Updated
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.platform.model.PackageVersionInfo
import io.hackle.android.internal.utils.json.toJson
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
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

class UserManagerTest {

    private lateinit var repository: KeyValueRepository
    private lateinit var cohortFetcher: UserCohortFetcher
    private lateinit var targetEventFetcher: UserTargetEventFetcher
    private lateinit var sut: UserManager

    private lateinit var listener: UserListener

    @Before
    fun before() {
        repository = MapKeyValueRepository()
        cohortFetcher = mockk()
        targetEventFetcher = mockk()
        sut = UserManager(
            MockDevice("hackle_device_id", emptyMap()),
            MockPackageInfo(PackageVersionInfo("1.0.0", 1L)),
            repository,
            cohortFetcher,
            targetEventFetcher
        )

        listener = mockk(relaxed = true)
        sut.addListener(listener)
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
        repository.putString(
            "user",
            UserManager.UserModel.from(
                User.builder()
                    .deviceId("saved_device_id")
                    .userId("saved_user_id")
                    .build()
            ).toJson()
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
        repository.putString(
            "user",
            UserManager.UserModel.from(
                User.builder()
                    .deviceId("saved_device_id")
                    .userId("saved_user_id")
                    .build()
            ).toJson()
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
    fun `resolve - currentUser`() {
        sut.initialize(
            User.builder()
                .id("init_id")
                .deviceId("init_device_id")
                .userId("init_user_id")
                .build()
        )

        val actual = sut.resolve(null, HackleAppContext.DEFAULT)
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
    fun `resolve - inputUser`() {
        sut.initialize(null)

        val actual = sut.resolve(User.builder().id("input_id").build(), HackleAppContext.DEFAULT)
        expectThat(actual).isEqualTo(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "input_id")
                .identifier(IdentifierType.DEVICE, "hackle_device_id")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .build()
        )
    }

    @Test
    fun `toHackleUser - merge with current context`() {
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
        every { cohortFetcher.fetch(any()) } returns  userCohorts
        every { targetEventFetcher.fetch(any()) } returns  userTargetEvents

        // when
        sut.initialize(User.builder().id("id").property("a", "a").build())
        sut.sync()
        val hackleUser =
            sut.toHackleUser(User.builder().id("id").userId("user_id").property("b", "b").build())

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
    fun `toHackleUser - full`() {
        val hackleUser = sut.toHackleUser(
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
    fun `toHackleUser - fill default id`() {
        val hackleUser = sut.toHackleUser(User.builder().build())
        expectThat(hackleUser).isEqualTo(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "hackle_device_id")
                .identifier(IdentifierType.DEVICE, "hackle_device_id")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .build()
        )
    }

    @Test
    fun `toHackleUser - hackle properties`() {
        val sut = UserManager(
            MockDevice("hackle_device_id", mapOf("age" to 42)),
            MockPackageInfo(PackageVersionInfo("1.0.0", 0L)),
            repository,
            cohortFetcher,
            targetEventFetcher
        )
        val hackleUser = sut.toHackleUser(User.builder().build())
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
        every { cohortFetcher.fetch(any()) } returns userCohorts
        every { targetEventFetcher.fetch(any()) } returns userTargetEvents

        sut.initialize(null)
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).cohorts).hasSize(0)
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).targetEvents).hasSize(0)

        sut.sync()
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).cohorts).isEqualTo(listOf(Cohort(42)))
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).targetEvents).isEqualTo(listOf(TargetEvent("purchase", listOf(TargetEvent.Stat(1738368000000, 1)), null)))
    }

    @Test
    fun `sync - when error on fetch userTarget then do not update userTarget`() {
        every { targetEventFetcher.fetch(any()) } throws IllegalArgumentException("fail")

        sut.initialize(null)
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).cohorts).hasSize(0)
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).targetEvents).hasSize(0)

        sut.sync()
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).cohorts).hasSize(0)
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).targetEvents).hasSize(0)
    }

    @Test
    fun `syncIfNeeded - when no new identifier then do not sync cohort and sync target event`() {
        // cohort not sync and target event not sync
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().build(),
                current = User.builder().build()
            )
        )
        // cohort not sync and target event not sync
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").build(),
                current = User.builder().build()
            )
        )
        // cohort not sync and target event not sync
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").build(),
                current = User.builder().id("id").build()
            )
        )
        // cohort not sync and target sync
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").deviceId("device_id").build(),
                current = User.builder().id("id").build()
            )
        )
        // cohort not sync and target event not sync
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").deviceId("device_id").build(),
                current = User.builder().id("id").deviceId("device_id").build()
            )
        )
        // cohort not sync and target event not sync
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").deviceId("device_id").identifier("custom", "custom_id").build(),
                current = User.builder().id("id").deviceId("device_id").build()
            )
        )
        // cohort not sync and target event not sync
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").deviceId("device_id").identifier("custom", "custom_id").build(),
                current = User.builder().id("id").deviceId("device_id").identifier("custom", "custom_id").build()
            )
        )

        verify { cohortFetcher wasNot Called }
        verify(exactly = 1) {
            targetEventFetcher.fetch(any())
        }
    }

    @Test
    fun `syncIfNeeded - when has new identifier then sync cohort and target event`() {
        // sync cohort and not sync target event
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().build(),
                current = User.builder().id("new_id").build()
            )
        )
        // sync cohort and not sync target event
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").build(),
                current = User.builder().id("new_id").build()
            )
        )
        // sync cohort and sync target event
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").build(),
                current = User.builder().id("id").deviceId("new_device_id").build()
            )
        )
        // sync cohort and sync target event
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").deviceId("device_id").build(),
                current = User.builder().id("id").deviceId("new_device_id").build()
            )
        )
        // sync cohort and not sync target event
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").deviceId("device_id").build(),
                current = User.builder().id("id").deviceId("device_id").identifier("custom", "new_custom_id").build()
            )
        )
        // sync cohort and not sync target event
        sut.syncIfNeeded(
            Updated(
                previous = User.builder().id("id").deviceId("device_id").identifier("custom", "custom_id").build(),
                current = User.builder().id("id").deviceId("device_id").identifier("custom", "new_custom_id").build()
            )
        )

        verify(exactly = 6) {
            cohortFetcher.fetch(any())
        }
        verify(exactly = 2) {
            targetEventFetcher.fetch(any())
        }
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
        every { targetEventFetcher.fetch(any()) } returns userTargetEvents
        sut.initialize(null)
        sut.sync()

        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).targetEvents).isEqualTo(userTargetEvents.rawEvents())

        val newTargetEvents = UserTargetEvents.builder()
            .put(targetEvent)
            .build()
        every { targetEventFetcher.fetch(any()) } returns newTargetEvents
        sut.sync()
        
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).targetEvents).isNotEqualTo(userTargetEvents.rawEvents())
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT).targetEvents).isEqualTo(newTargetEvents.rawEvents())
    }

    @Test
    fun `setUser - decorate hackleDeviceId`() {
        val actual = sut.setUser(User.builder().build())
        expectThat(actual.current).isEqualTo(
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

        val actual = sut.setUser(User.builder().deviceId("device_id").build())

        val currentUser = User.builder()
            .id("hackle_device_id")
            .deviceId("device_id")
            .build()
        expectThat(actual.previous).isEqualTo(initUser)
        expectThat(actual.current).isEqualTo(currentUser)
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
        )
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
        )
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
        )
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
        )

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
        )

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
        )

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
        )

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
        )

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
        every { cohortFetcher.fetch(any()) } returns userCohorts
        every { targetEventFetcher.fetch(any()) } returns userTargetEvents

        sut.initialize(null)
        sut.sync()

        sut.setUser(User.builder().deviceId("device_id").build())
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT)).isEqualTo(
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
        every { cohortFetcher.fetch(any()) } returns userCohorts
        every { targetEventFetcher.fetch(any()) } returns userTargetEvents

        sut.initialize(null)
        sut.sync()

        sut.setUser(User.builder().deviceId("device_id").build())
        expectThat(sut.currentUser).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
        expectThat(sut.resolve(null, HackleAppContext.DEFAULT)).isEqualTo(
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
        val actual = sut.updateProperties(operations)
        expectThat(actual.current).isEqualTo(
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
        val actual = sut.updateProperties(operations)
        expectThat(actual.current).isEqualTo(
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
    fun `setUserId - new`() {
        sut.initialize(null)
        val actual = sut.setUserId("user_id")
        expectThat(actual.current).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .userId("user_id")
                .build()
        )
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

        val actual = sut.setUserId(null)
        expectThat(actual.current).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .build()
        )
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

        val actual = sut.setUserId("user_id_2")
        expectThat(actual.current).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .userId("user_id_2")
                .build()
        )
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

        val actual = sut.setUserId("user_id")
        expectThat(actual.current).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("hackle_device_id")
                .userId("user_id")
                .build()
        )
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
        val actual = sut.setDeviceId("device_id")
        expectThat(actual.current).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
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

        val actual = sut.setDeviceId("device_id_2")
        expectThat(actual.current).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id_2")
                .build()
        )
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

        val actual = sut.setDeviceId("device_id")
        expectThat(actual.current).isEqualTo(
            User.builder()
                .id("hackle_device_id")
                .deviceId("device_id")
                .build()
        )
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
        expectThat(repository.getString("user")).isNull()
        sut.onBackground(42)
        expectThat(repository.getString("user")).isNotNull()
    }
}
