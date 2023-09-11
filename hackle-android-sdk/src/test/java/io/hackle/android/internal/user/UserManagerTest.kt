package io.hackle.android.internal.user

import io.hackle.android.internal.database.MapKeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.utils.toJson
import io.hackle.android.mock.MockDevice
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*

class UserManagerTest {


    @Test
    fun `initialize - User 설정한 경우`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)

        val user = User.of("hello")
        userManager.initialize(user)

        expectThat(userManager.currentUser) isSameInstanceAs user
    }

    @Test
    fun `initialize - from repository`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val user = User.builder("id")
            .userId("userId")
            .deviceId("deviceId")
            .identifier("customId", "customValue")
            .property("string", "value")
            .property("int", 42)
            .property("long", 42L)
            .property("boolean", false)
            .property("null", null)
            .build()
        repository.putString("user", user.toJson())
        val userManager = UserManager(device, repository)

        userManager.initialize(null)

        expectThat(userManager.currentUser) {
            get { identifiers } isEqualTo user.identifiers
            get { properties }.and {
                get("string") isEqualTo "value"
                get("int") isEqualTo 42.0
                get("long") isEqualTo 42.0
                get("boolean") isEqualTo false
            }
        }
    }

    @Test
    fun `initialize - from repository null`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)
        userManager.initialize(null)
        expectThat(userManager.currentUser).isEqualTo(User.builder().deviceId("test_device_id")
            .build())
    }

    @Test
    fun `resolve - user not null`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)

        val user = User.builder().deviceId("42").build()
        val actual = userManager.resolve(user)

        expectThat(actual) isEqualTo user
    }

    @Test
    fun `resolve - currentUser`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)

        val actual = userManager.resolve(null)

        expectThat(actual) isEqualTo User.builder().deviceId("test_device_id").build()
    }

    @Test
    fun `setUser - 기존 사용자와 다른 경우`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)
        val listener = UserListenerStub()
        userManager.addListener(listener)

        val user = User.builder().deviceId("42").build()
        val actual = userManager.setUser(user)

        expectThat(actual) isEqualTo user
        expectThat(listener.history) {
            hasSize(1)
            first().and {
                get { first } isEqualTo User.builder().deviceId("test_device_id").build()
                get { second } isEqualTo user
                get { third } isGreaterThan 0
            }
        }
    }

    @Test
    fun `setUser - 기존 사용자와 다른 경우 2`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)
        val listener = UserListenerStub()
        userManager.addListener(listener)

        val oldUser = User.builder().deviceId("a").property("a", "a").build()
        val newUser = User.builder().deviceId("b").property("b", "b").build()

        userManager.initialize(oldUser)
        val actual = userManager.setUser(newUser)

        expectThat(actual).isEqualTo(newUser)
        expectThat(listener.history) {
            hasSize(1)
            first().and {
                get { first } isEqualTo oldUser
                get { second } isEqualTo newUser
                get { third } isGreaterThan 0
            }
        }
    }

    @Test
    fun `setUser - 기존 사용자와 같은 사용자인 경우`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)
        val listener = UserListenerStub()
        userManager.addListener(listener)

        val oldUser = User.builder().deviceId("a").property("a", "a").build()
        val newUser = User.builder().deviceId("a").property("b", "b").build()

        userManager.initialize(oldUser)
        val actual = userManager.setUser(newUser)

        expectThat(actual).isEqualTo(
            User.builder().deviceId("a").property("a", "a").property("b", "b").build()
        )
        expectThat(listener.history).hasSize(0)
    }

    @Test
    fun `onChange - 현재 유저를 저장한다`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)
        val listener = UserListenerStub()
        userManager.addListener(listener)

        val user = User.builder().deviceId("a").property("a", "a").build()
        userManager.initialize(user)
        userManager.onChanged(AppState.BACKGROUND, 42)

        expectThat(repository.getString("user"))
            .isNotNull()
            .isEqualTo(user.toJson())
    }

    @Test
    fun `resetUser`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)

        val user = User.builder().deviceId("a").property("a", "a").build()
        userManager.initialize(user)

        val actual = userManager.resetUser()
        expectThat(actual) isEqualTo User.builder().deviceId("test_device_id").build()
        expectThat(userManager.currentUser) isEqualTo User.builder().deviceId("test_device_id")
            .build()
    }

    @Test
    fun `updateProperties`() {
        val device = MockDevice("test_device_id", emptyMap())
        val repository = MapKeyValueRepository()
        val userManager = UserManager(device, repository)

        val user = User.builder()
            .userId("user")
            .deviceId("device")
            .property("a", 42)
            .property("b", "b")
            .property("c", "c")
            .build()
        userManager.initialize(user)

        val operations = PropertyOperations.builder()
            .set("d", "d")
            .increment("a", 42)
            .append("c", "cc")
            .build()

        val actual = userManager.updateProperties(operations)
        expectThat(actual) isEqualTo User.builder()
            .userId("user")
            .deviceId("device")
            .property("a", 84.0)
            .property("b", "b")
            .property("c", listOf("c", "cc"))
            .property("d", "d")
            .build()

    }

    private class UserListenerStub : UserListener {

        val history = mutableListOf<Triple<User, User, Long>>()
        override fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long) {
            history += Triple(oldUser, newUser, timestamp)
        }
    }
}
