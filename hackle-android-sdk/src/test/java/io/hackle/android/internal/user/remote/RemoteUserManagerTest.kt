package io.hackle.android.internal.user.remote

import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.user.UserListener
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationManager
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.hackle.android.mock.MockUserRepository
import io.hackle.sdk.common.PropertyOperation
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.util.concurrent.CompletableFuture

class RemoteUserManagerTest {

    private lateinit var repository: MockUserRepository
    private lateinit var evaluationManager: WorkspaceEvaluationManager
    private lateinit var sut: RemoteUserManager

    private lateinit var listener: UserListener

    @Before
    fun before() {
        repository = MockUserRepository()
        evaluationManager = mockk()
        sut = RemoteUserManager(
            Clock.SYSTEM,
            MockDevice("hackle_device_id", emptyMap()),
            MockPackageInfo(PackageVersionInfo("1.0.0", 1L)),
            repository,
            evaluationManager
        )

        listener = mockk(relaxed = true)
        sut.addListener(listener)

        every { evaluationManager.sync(any()) } returns CompletableFuture.completedFuture(null)
    }

    @Test
    fun `initialize - with default user`() {
        sut.initialize(null)
        expectThat(sut.currentUser) isEqualTo User.builder()
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
        expectThat(sut.currentUser) isEqualTo User.builder()
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
        expectThat(sut.currentUser) isEqualTo User.builder()
            .id("hackle_device_id")
            .deviceId("init_device_id")
            .userId("init_user_id")
            .build()
    }

    @Test
    fun `initialize - 유저의 properties는 클라이언트 상태로 유지하지 않는다`() {
        sut.initialize(
            User.builder()
                .userId("user_id")
                .property("age", 42)
                .build()
        )
        expectThat(sut.currentUser.properties) isEqualTo emptyMap()
    }

    @Test
    fun `sync - 최초 sync는 초기 유저의 properties를 set operation으로 전달한다`() {
        // given
        sut.initialize(
            User.builder()
                .userId("user_id")
                .property("age", 42)
                .build()
        )

        // when
        sut.sync().get()

        // then
        verify(exactly = 1) {
            evaluationManager.sync(withArg {
                expectThat(it) {
                    get { user.identifiers[IdentifierType.USER.key] } isEqualTo "user_id"
                    get { operations.asMap() } isEqualTo mapOf(PropertyOperation.SET to mapOf<String, Any>("age" to 42))
                }
            })
        }
    }

    @Test
    fun `sync - 두번째 sync부터는 현재 유저와 빈 operations로 동기화한다`() {
        // given
        sut.initialize(User.builder().property("age", 42).build())

        // when
        sut.sync().get()
        sut.sync().get()

        // then
        val contexts = mutableListOf<RemoteEvaluateContext>()
        verify(exactly = 2) { evaluationManager.sync(capture(contexts)) }
        expectThat(contexts[0].operations.size) isEqualTo 1
        expectThat(contexts[1].operations.size) isEqualTo 0
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
        val sut = RemoteUserManager(
            Clock.SYSTEM,
            MockDevice("hackle_device_id", mapOf("age" to 42)),
            MockPackageInfo(PackageVersionInfo("1.0.0", 0L)),
            repository,
            evaluationManager
        )
        val hackleUser = sut.hackleUser(User.builder().build())
        expectThat(hackleUser.hackleProperties.size).isGreaterThan(0)
    }

    @Test
    fun `setUser - 식별자가 변경되면 리스너에 알리고 properties를 set operation으로 동기화한다`() {
        // given
        sut.initialize(null)

        // when
        sut.setUser(
            User.builder()
                .deviceId("device_id")
                .userId("user_id")
                .property("age", 42)
                .build()
        ).get()

        // then
        expectThat(sut.currentUser) isEqualTo User.builder()
            .id("hackle_device_id")
            .deviceId("device_id")
            .userId("user_id")
            .build()
        verify(exactly = 1) {
            listener.onUserUpdated(
                User.builder()
                    .id("hackle_device_id")
                    .deviceId("hackle_device_id")
                    .build(),
                any(),
                any()
            )
        }

        verify(exactly = 1) {
            evaluationManager.sync(withArg {
                expectThat(it) {
                    get { user.identifiers[IdentifierType.USER.key] } isEqualTo "user_id"
                    get { user.identifiers[IdentifierType.DEVICE.key] } isEqualTo "device_id"
                    get { operations.asMap() } isEqualTo mapOf(PropertyOperation.SET to mapOf<String, Any>("age" to 42))
                }
            })
        }
    }

    @Test
    fun `setUser - 식별자와 properties 변화가 없으면 동기화하지 않는다`() {
        // given
        sut.initialize(null)

        // when
        sut.setUser(User.builder().build()).get()

        // then
        verify(exactly = 0) { evaluationManager.sync(any()) }
        verify(exactly = 0) { listener.onUserUpdated(any(), any(), any()) }
    }

    @Test
    fun `setUser - 식별자가 같아도 properties가 있으면 동기화한다`() {
        // given
        sut.initialize(null)

        // when
        sut.setUser(User.builder().property("age", 42).build()).get()

        // then
        verify(exactly = 1) { evaluationManager.sync(any()) }
        verify(exactly = 0) { listener.onUserUpdated(any(), any(), any()) }
    }

    @Test
    fun `setUserId - 변경되면 동기화하고 동일하면 동기화하지 않는다`() {
        // given
        sut.initialize(null)

        // when
        sut.setUserId("user_id").get()
        sut.setUserId("user_id").get()

        // then
        expectThat(sut.currentUser.userId) isEqualTo "user_id"
        verify(exactly = 1) { evaluationManager.sync(any()) }
        verify(exactly = 1) { listener.onUserUpdated(any(), any(), any()) }
    }

    @Test
    fun `setDeviceId - 변경되면 동기화하고 동일하면 동기화하지 않는다`() {
        // given
        sut.initialize(null)

        // when
        sut.setDeviceId("device_id").get()
        sut.setDeviceId("device_id").get()

        // then
        expectThat(sut.currentUser.deviceId) isEqualTo "device_id"
        verify(exactly = 1) { evaluationManager.sync(any()) }
        verify(exactly = 1) { listener.onUserUpdated(any(), any(), any()) }
    }

    @Test
    fun `resetUser - 기본 유저로 리셋하고 clearAll operation으로 동기화한다`() {
        // given
        sut.initialize(
            User.builder()
                .userId("user_id")
                .property("age", 42)
                .build()
        )

        // when
        sut.resetUser().get()

        // then
        // 기본 유저는 deviceId만 갖는다. id는 hackleUser 변환 시 device id로 채워진다.
        val defaultUser = User.builder()
            .deviceId("hackle_device_id")
            .build()
        expectThat(sut.currentUser) isEqualTo defaultUser
        verify(exactly = 1) { listener.onUserUpdated(any(), defaultUser, any()) }

        verify(exactly = 1) {
            evaluationManager.sync(withArg {
                expectThat(it.operations.asMap().keys) isEqualTo setOf(PropertyOperation.CLEAR_ALL)
            })
        }
    }

    @Test
    fun `updateProperties - 유저 상태는 바꾸지 않고 operations를 서버에 동기화한다`() {
        // given
        sut.initialize(null)
        val operations = PropertyOperations.builder().set("age", 42).build()

        // when
        sut.updateProperties(operations).get()

        // then
        expectThat(sut.currentUser.properties) isEqualTo emptyMap()

        verify(exactly = 1) {
            evaluationManager.sync(withArg {
                expectThat(it.operations.asMap()) isEqualTo mapOf(
                    PropertyOperation.SET to mapOf<String, Any>("age" to 42)
                )
            })
        }
    }

    @Test
    fun `updateProperties - property 이벤트는 발행하지 않는다`() {
        // given
        sut.initialize(null)

        // when
        sut.updateProperties(PropertyOperations.builder().set("age", 42).build()).get()

        // then
        verify(exactly = 0) { listener.onPropertyOperations(any(), any(), any()) }
    }

    @Test
    fun `updateProperties - 빈 operations면 동기화하지 않는다`() {
        // given
        sut.initialize(null)

        // when
        sut.updateProperties(PropertyOperations.empty()).get()

        // then
        verify(exactly = 0) { evaluationManager.sync(any()) }
    }

    @Test
    fun `sync - 동기화에 실패해도 future는 성공으로 완료된다`() {
        // given
        sut.initialize(null)
        val failedFuture = CompletableFuture<Void>()
        failedFuture.completeExceptionally(IllegalArgumentException("fail"))
        every { evaluationManager.sync(any()) } returns failedFuture

        // when & then
        sut.sync().get()
    }

    @Test
    fun `onChanged - foreground`() {
        sut.onForeground(42, true)
    }

    @Test
    fun `onChanged - background`() {
        sut.initialize(null)
        expectThat(repository.get()).isNull()
        sut.onBackground(42)
        expectThat(repository.get()).isNotNull()
    }
}
