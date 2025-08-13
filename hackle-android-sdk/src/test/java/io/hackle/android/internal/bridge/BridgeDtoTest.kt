package io.hackle.android.internal.bridge.model

import io.hackle.android.internal.properties.operate
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.ParameterConfig
import io.hackle.sdk.common.PropertyOperation
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.common.Variation
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations
import io.hackle.sdk.common.subscription.HackleSubscriptionStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoConversionTest {

    @Test
    fun `UserDto를 User 모델로 변환한다`() {
        // given
        val dto = UserDto(
            id = "test_id",
            userId = "test_user_id",
            deviceId = "test_device_id",
            identifiers = mapOf("custom_id" to "custom_value"),
            properties = mapOf("key" to "value", "age" to 30)
        )

        // when
        val user = User.from(dto)

        // then
        assertEquals("test_id", user.id)
        assertEquals("test_user_id", user.userId)
        assertEquals("test_device_id", user.deviceId)
        assertEquals(mapOf("custom_id" to "custom_value"), user.identifiers)
        assertEquals(mapOf("key" to "value", "age" to 30), user.properties)
    }

    @Test
    fun `User 모델을 UserDto로 변환한다`() {
        // given
        val user = User.builder()
            .id("test_id")
            .userId("test_user_id")
            .deviceId("test_device_id")
            .identifiers(mapOf("custom_id" to "custom_value"))
            .properties(mapOf("key" to "value", "age" to 30))
            .build()

        // when
        val dto = user.toDto()

        // then
        assertEquals("test_id", dto.id)
        assertEquals("test_user_id", dto.userId)
        assertEquals("test_device_id", dto.deviceId)
        assertEquals(mapOf("custom_id" to "custom_value"), dto.identifiers)
        assertEquals(mapOf("key" to "value", "age" to 30), dto.properties)
    }

    @Test
    fun `Decision 모델을 DecisionDto로 변환한다`() {
        // given
        val decision = mockk<Decision> {
            every { variation } returns Variation.A
            every { reason } returns DecisionReason.DEFAULT_RULE
            every { config } returns ParameterConfig.empty()
        }

        // when
        val dto = decision.toDto()

        // then
        assertEquals("A", dto.variation)
        assertEquals("DEFAULT_RULE", dto.reason)
    }

    @Test
    fun `FeatureFlagDecision 모델을 FeatureFlagDecisionDto로 변환한다`() {
        // given
        val decision = mockk<FeatureFlagDecision> {
            every { isOn } returns true
            every { reason } returns DecisionReason.DEFAULT_RULE
            every { config } returns ParameterConfig.empty()
        }

        // when
        val dto = decision.toDto()

        // then
        assertTrue(dto.isOn)
        assertEquals("DEFAULT_RULE", dto.reason)
    }

    @Test
    fun `EventDto를 Event 모델로 변환한다`() {
        // given
        val dto = EventDto(
            key = "test_event",
            value = 42.0,
            properties = mapOf("category" to "test")
        )

        // when
        val event = Event.from(dto)

        // then
        assertEquals("test_event", event.key)
        assertEquals(42.0, event.value)
        assertEquals(mapOf("category" to "test"), event.properties)
    }

    @Test
    fun `EventDto의 value가 null일 때 Event 모델로 변환한다`() {
        // given
        val dto = EventDto(
            key = "test_event",
            value = null,
            properties = null
        )

        // when
        val event = Event.from(dto)

        // then
        assertEquals("test_event", event.key)
        assertNull(event.value)
        assertTrue(event.properties.isEmpty())
    }

    @Test
    fun `PropertyOperationsDto를 PropertyOperations 모델로 변환한다`() {
        // given
        val dto: PropertyOperationsDto = mapOf(
            "\$set" to mapOf("a" to 1, "b" to "hello"),
            "\$setOnce" to mapOf("c" to true),
            "\$unset" to mapOf("d" to 0),
            "\$increment" to mapOf("e" to 10),
            "\$append" to mapOf("f" to "new"),
            "\$appendOnce" to mapOf("g" to "new_once"),
            "\$prepend" to mapOf("h" to "old"),
            "\$prependOnce" to mapOf("i" to "old_once"),
            "\$remove" to mapOf("j" to "item"),
            "\$clearAll" to mapOf("clearAll" to "-")
        )

        // when
        val operations = PropertyOperations.from(dto)
        val expected = PropertyOperations.builder()
            .set("a", 1).set("b", "hello")
            .setOnce("c", true)
            .unset("d")
            .increment("e", 10)
            .append("f", "new")
            .appendOnce("g", "new_once")
            .prepend("h", "old")
            .prependOnce("i", "old_once")
            .remove("j", "item")
            .clearAll()
            .build()

        // then
        expected.asMap().forEach { (operation, value) ->
            assertEquals(value, operations.asMap()[operation])
        }
    }

    @Test
    fun `잘못된 operation이 포함된 PropertyOperationsDto는 해당 operation을 무시한다`() {
        // given
        val dto: PropertyOperationsDto = mapOf(
            "\$set" to mapOf("a" to 1),
            "invalid_op" to mapOf("b" to 2)
        )

        // when
        val operations = PropertyOperations.from(dto)
        val expected = PropertyOperations.builder().set("a", 1).build()

        // then
        expected.asMap().forEach { (operation, value) ->
            assertEquals(value, operations.asMap()[operation])
        }
    }


    @Test
    fun `HackleSubscriptionOperationsDto를 HackleSubscriptionOperations 모델로 변환한다`() {
        // given
        val dto = mapOf(
            "\$information" to "SUBSCRIBED",
            "\$marketing" to "UNSUBSCRIBED",
            "chat" to "UNKNOWN"
        )

        // when
        val operations = HackleSubscriptionOperations.from(dto)
        val expected = HackleSubscriptionOperations.builder()
            .information( HackleSubscriptionStatus.SUBSCRIBED)
            .marketing( HackleSubscriptionStatus.UNSUBSCRIBED)
            .custom("chat", HackleSubscriptionStatus.UNKNOWN)
            .build()

        // then
        expected.asMap().forEach { (key, status) ->
            assertEquals(status,  operations.asMap()[key])
        }
        
    }
}

class UserDtoTest {
    @Test
    fun `Map으로부터 UserDto를 생성한다`() {
        // given
        val map = mapOf(
            "id" to "test_id",
            "userId" to "test_user_id",
            "deviceId" to "test_device_id",
            "identifiers" to mapOf("idfa" to "test_idfa"),
            "properties" to mapOf("age" to 30)
        )

        // when
        val dto = UserDto.from(map)

        // then
        assertEquals("test_id", dto.id)
        assertEquals("test_user_id", dto.userId)
        assertEquals("test_device_id", dto.deviceId)
        assertEquals(mapOf("idfa" to "test_idfa"), dto.identifiers)
        assertEquals(mapOf("age" to 30), dto.properties)
    }

    @Test
    fun `Map에 일부 키가 누락된 경우 기본값으로 UserDto를 생성한다`() {
        // given
        val map = mapOf(
            "userId" to "test_user_id"
        )

        // when
        val dto = UserDto.from(map)

        // then
        assertNull(dto.id)
        assertEquals("test_user_id", dto.userId)
        assertNull(dto.deviceId)
        assertTrue(dto.identifiers.isEmpty())
        assertTrue(dto.properties.isEmpty())
    }
}

class EventDtoTest {
    @Test
    fun `Map으로부터 EventDto를 생성한다`() {
        // given
        val map = mapOf(
            "key" to "purchase",
            "value" to 10.5,
            "properties" to mapOf("product_id" to "P123")
        )

        // when
        val dto = EventDto.from(map)

        // then
        assertEquals("purchase", dto.key)
        assertEquals(10.5, dto.value)
        assertEquals(mapOf("product_id" to "P123"), dto.properties)
    }

    @Test
    fun `Map에 value와 properties가 없는 경우에도 EventDto를 생성한다`() {
        // given
        val map = mapOf("key" to "login")

        // when
        val dto = EventDto.from(map)

        // then
        assertEquals("login", dto.key)
        assertNull(dto.value)
        assertNull(dto.properties)
    }
}
