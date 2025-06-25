package io.hackle.android.internal.bridge

import io.hackle.android.HackleApp
import io.hackle.android.internal.bridge.model.BridgeResponse
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.ParameterConfig
import io.hackle.sdk.common.PropertyOperation
import io.hackle.sdk.common.User
import io.hackle.sdk.common.Variation
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@Suppress("DEPRECATION")
class HackleBridgeTest {

    private lateinit var app: HackleApp
    private lateinit var bridge: HackleBridge

    @Before
    fun setup() {
        app = spyk(mockk<HackleApp>())
        bridge = HackleBridge(app)
    }

    @Test
    fun `invoke with session id`() {
        every { app.sessionId } returns "abcd1234"

        val jsonString = createJsonString("getSessionId")
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) { app.sessionId }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data,`is`("abcd1234"))
        }
    }

    @Test
    fun `invoke with get user`() {
        val user = User.builder()
            .id("foo")
            .userId("bar")
            .deviceId("abcd1234")
            .identifiers(mapOf(
                "foo" to "bar"
            ))
            .properties(mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            ))
            .build()
        every { app.user } returns user
        val jsonString = createJsonString("getUser")
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) { app.user }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))

            val map = data as Map<*, *>
            assertThat(map.size, `is`(5))
            assertThat(map["id"], `is`("foo"))
            assertThat(map["userId"], `is`("bar"))
            assertThat(map["deviceId"], `is`("abcd1234"))

            val identifiers = map["identifiers"] as Map<*, *>
            assertThat(identifiers.size, `is`(1))
            assertThat(identifiers["foo"], `is`("bar"))

            val properties = map["properties"] as Map<*, *>
            assertThat(properties.size, `is`(3))
            assertThat((properties["number"] as Number).toDouble(), `is`(123.0))
            assertThat(properties["string"], `is`("text"))

            val array = properties["array"] as ArrayList<*>
            assertThat(array.size, `is`(2))
            assertThat((array[0] as Number).toDouble(), `is`(123.0))
            assertThat(array[1], `is`("123"))
        }
    }

    @Test
    fun `invoke with set user`() {
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf(
                "foobar" to "foofoo",
                "foobar2" to "barbar"
            ),
            "properties" to mapOf(
                "null" to null,
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123", null),
                "map" to mapOf("key" to "value")
            )
        )
        val parameters = mapOf("user" to user)
        val jsonString = createJsonString("setUser", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.setUser(withArg {
                assertThat(it.id, `is`("foo"))
                assertThat(it.userId, `is`("bar"))
                assertThat(it.deviceId, `is`("abcd1234"))
                assertThat(it.identifiers.size, `is`(2))
                assertThat(it.identifiers["foobar"], `is`("foofoo"))
                assertThat(it.identifiers["foobar2"], `is`("barbar"))
                assertThat(it.properties.size, `is`(3))
                assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                assertThat(it.properties["string"] as String, `is`("text"))
                val array = it.properties["array"] as ArrayList<*>
                assertThat(array.size, `is`(2))
                assertThat((array[0] as Number).toDouble(), `is`(123.0))
                assertThat(array[1], `is`("123"))
            })
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with set user with invalid parameters`() {
        val parameters = emptyMap<String, Any>()
        val jsonString = createJsonString("setUser", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isNullOrEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with set user id`() {
        val userId = "abcd1234"
        val parameters = mapOf("userId" to userId)
        val jsonString = createJsonString("setUserId", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.setUserId(withArg { assertThat(it, `is`(userId)) })
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with set user id with invalid parameters`() {
        val parameters = emptyMap<String, Any>()
        val jsonString = createJsonString("setUserId", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with set device id`() {
        val parameters = mapOf("deviceId" to "abcd1234")
        val jsonString = createJsonString("setDeviceId", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.setDeviceId(withArg { assertThat(it, `is`("abcd1234")) })
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with set device id with invalid parameters`() {
        val parameters = emptyMap<String, Any>()
        val jsonString = createJsonString("setDeviceId", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with set user property`() {
        val parameters = mapOf("key" to "foo", "value" to "bar")
        val jsonString = createJsonString("setUserProperty", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.setUserProperty(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`("bar")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with set user property with invalid parameters`() {
        val parameters = mapOf<String, Any>()
        val jsonString = createJsonString("setUserProperty", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with update user properties`() {
        val operations = mapOf(
            "\$set" to mapOf(
                "null" to null,
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123", null),
                "map" to mapOf("key" to "value")
            ),
            "\$setOnce" to mapOf("foo" to "bar")
        )
        val parameters = mapOf("operations" to operations)
        val jsonString = createJsonString("updateUserProperties", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.updateUserProperties(withArg {
                val map = it.asMap()
                assertThat(map.size, `is`(2))

                val set = map[PropertyOperation.SET] as Map<String, *>
                assertThat(set.size, `is`(3))
                assertThat((set["number"] as Number).toDouble(), `is`(123.0))
                assertThat(set["string"], `is`("text"))

                val array = set["array"] as ArrayList<*>
                assertThat(array.size, `is`(2))
                assertThat((array[0] as Number).toDouble(), `is`(123.0))
                assertThat(array[1], `is`("123"))

                val setOnce = map[PropertyOperation.SET_ONCE] as Map<String, *>
                assertThat(setOnce.size, `is`(1))
                assertThat(setOnce["foo"], `is`("bar"))
            })
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with update user properties with invalid parameters`() {
        val parameters = mapOf<String, Any>()
        val jsonString = createJsonString("updateUserProperties", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with reset user`() {
        val jsonString = createJsonString("resetUser")
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.resetUser()
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with variation`() {
        every { app.variation(any(), any<Variation>()) } returns Variation.B
        val parameters = mapOf("experimentKey" to 1, "defaultVariation" to "D")
        val jsonString = createJsonString("variation", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.variation(
                withArg { assertThat(it, `is`(1)) },
                withArg<Variation> { assertThat(it.name, `is`("D")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("B"))
        }
    }

    @Test
    fun `invoke with variation with user string`() {
        every { app.variation(any(), any<String>(), any()) } returns Variation.B
        val parameters = mapOf(
            "experimentKey" to 1,
            "defaultVariation" to "D",
            "user" to "abcd1234"
        )
        val jsonString = createJsonString("variation", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.variation(
                withArg { assertThat(it, `is`(1)) },
                withArg<String> { assertThat(it, `is`("abcd1234")) },
                withArg { assertThat(it.name, `is`("D")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("B"))
        }
    }

    @Test
    fun `invoke with variation with user object`() {
        every { app.variation(any(), any<User>(), any()) } returns Variation.B
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf("foo" to "bar"),
            "properties" to mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            )
        )
        val parameters = mapOf(
            "experimentKey" to 1,
            "defaultVariation" to "D",
            "user" to user
        )
        val jsonString = createJsonString("variation", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.variation(
                withArg { assertThat(it, `is`(1)) },
                withArg<User> {
                    assertThat(it.id, `is`("foo"))
                    assertThat(it.userId, `is`("bar"))
                    assertThat(it.deviceId, `is`("abcd1234"))
                    assertThat(it.identifiers.size, `is`(1))
                    assertThat(it.identifiers["foo"], `is`("bar"))
                    assertThat(it.properties.size, `is`(3))
                    assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                    assertThat(it.properties["string"], `is`("text"))
                    val array = it.properties["array"] as ArrayList<*>
                    assertThat(array.size, `is`(2))
                    assertThat((array[0] as Number).toDouble(), `is`(123.0))
                    assertThat(array[1], `is`("123"))
                },
                withArg { assertThat(it.name, `is`("D")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("B"))
        }
    }

    @Test
    fun `invoke with variation with invalid parameters case`() {
        val parameters = emptyMap<String, Any>()
        val jsonString = createJsonString("variation", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with variation detail`() {
        every { app.variationDetail(any(), any<Variation>()) } returns Decision.of(Variation.B, DecisionReason.DEFAULT_RULE)
        val parameters = mapOf("experimentKey" to 1, "defaultVariation" to "D")
        val jsonString = createJsonString("variationDetail", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.variationDetail(
                withArg { assertThat(it, `is`(1)) },
                withArg<Variation> { assertThat(it.name, `is`("D")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))

            val map = data as Map<*, *>
            assertNull(map["experiment"])
            assertThat(map["variation"], `is`("B"))
            assertThat(map["reason"], `is`("DEFAULT_RULE"))

            val config = map["config"] as Map<*, *>
            val configParameters = config["parameters"] as Map<*, *>
            assertThat(configParameters.isEmpty(), `is`(true))
        }
    }

    @Test
    fun `invoke with variation detail with user string`() {
        every { app.variationDetail(any(), any<String>(), any()) } returns Decision.of(Variation.B, DecisionReason.DEFAULT_RULE)
        val parameters = mapOf(
            "experimentKey" to 1,
            "defaultVariation" to "D",
            "user" to "abcd1234"
        )
        val jsonString = createJsonString("variationDetail", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.variationDetail(
                withArg { assertThat(it, `is`(1)) },
                withArg<String> { assertThat(it, `is`("abcd1234")) },
                withArg { assertThat(it.name, `is`("D")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))

            val map = data as Map<*, *>
            assertNull(map["experiment"])
            assertThat(map["variation"], `is`("B"))
            assertThat(map["reason"], `is`("DEFAULT_RULE"))

            val config = map["config"] as Map<*, *>
            val configParameters = config["parameters"] as Map<*, *>
            assertThat(configParameters.isEmpty(), `is`(true))
        }
    }

    @Test
    fun `invoke with variation detail with user object`() {
        every { app.variationDetail(any(), any<User>(), any()) } returns Decision.of(Variation.B, DecisionReason.DEFAULT_RULE)
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf("foo" to "bar"),
            "properties" to mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            )
        )
        val parameters = mapOf(
            "experimentKey" to 1,
            "defaultVariation" to "D",
            "user" to user
        )
        val jsonString = createJsonString("variationDetail", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.variationDetail(
                withArg { assertThat(it, `is`(1)) },
                withArg<User> {
                    assertThat(it.id, `is`("foo"))
                    assertThat(it.userId, `is`("bar"))
                    assertThat(it.deviceId, `is`("abcd1234"))
                    assertThat(it.identifiers.size, `is`(1))
                    assertThat(it.identifiers["foo"], `is`("bar"))
                    assertThat(it.properties.size, `is`(3))
                    assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                    assertThat(it.properties["string"], `is`("text"))
                    val array = it.properties["array"] as ArrayList<*>
                    assertThat(array.size, `is`(2))
                    assertThat((array[0] as Number).toDouble(), `is`(123.0))
                    assertThat(array[1], `is`("123"))
                },
                withArg { assertThat(it.name, `is`("D")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))

            val map = data as Map<*, *>
            assertNull(map["experiment"])
            assertThat(map["variation"], `is`("B"))
            assertThat(map["reason"], `is`("DEFAULT_RULE"))

            val config = map["config"] as Map<*, *>
            val configParameters = config["parameters"] as Map<*, *>
            assertThat(configParameters.isEmpty(), `is`(true))
        }
    }

    @Test
    fun `invoke with variation detail with invalid parameters case`() {
        val parameters = emptyMap<String, Any>()
        val jsonString = createJsonString("variationDetail", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with is feature on`() {
        every { app.isFeatureOn(any()) } returns true
        val parameters = mapOf("featureKey" to 1)
        val jsonString = createJsonString("isFeatureOn", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.isFeatureOn(withArg {
                assertThat(it, `is`(1))
            })
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`(true))
        }
    }

    @Test
    fun `invoke with is feature on with user string`() {
        every { app.isFeatureOn(any(), any<String>()) } returns true
        val parameters = mapOf("featureKey" to 1, "user" to "abcd1234")
        val jsonString = createJsonString("isFeatureOn", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.isFeatureOn(
                withArg { assertThat(it, `is`(1)) },
                withArg<String> { assertThat(it, `is`("abcd1234")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`(true))
        }
    }

    @Test
    fun `invoke with is feature on with user object`() {
        every { app.isFeatureOn(any(), any<User>()) } returns true
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf("foo" to "bar"),
            "properties" to mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            )
        )
        val parameters = mapOf("featureKey" to 1, "user" to user)
        val jsonString = createJsonString("isFeatureOn", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.isFeatureOn(
                withArg { assertThat(it, `is`(1)) },
                withArg<User> {
                    assertThat(it.id, `is`("foo"))
                    assertThat(it.userId, `is`("bar"))
                    assertThat(it.deviceId, `is`("abcd1234"))
                    assertThat(it.identifiers.size, `is`(1))
                    assertThat(it.identifiers["foo"], `is`("bar"))
                    assertThat(it.properties.size, `is`(3))
                    assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                    assertThat(it.properties["string"], `is`("text"))
                    val array = it.properties["array"] as ArrayList<*>
                    assertThat(array.size, `is`(2))
                    assertThat(array[0], `is`(123.0))
                    assertThat(array[1], `is`("123"))
                }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`(true))
        }
    }

    @Test
    fun `invoke with is feature on with invalid parameters case`() {
        val parameters = emptyMap<String, Any>()
        val jsonString = createJsonString("isFeatureOn", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with feature detail`() {
        every { app.featureFlagDetail(any()) } returns FeatureFlagDecision.on(DecisionReason.DEFAULT_RULE, ParameterConfig.empty())
        val parameters = mapOf("featureKey" to 1)
        val jsonString = createJsonString("featureFlagDetail", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.featureFlagDetail(withArg {
                assertThat(it, `is`(1))
            })
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))

            val map = data as Map<*, *>
            assertNull(map["featureFlag"])
            assertThat(map["isOn"], `is`(true))
            assertThat(map["reason"], `is`("DEFAULT_RULE"))

            val config = map["config"] as Map<*, *>
            val configParameters = config["parameters"] as Map<*, *>
            assertThat(configParameters.isEmpty(), `is`(true))
        }
    }

    @Test
    fun `invoke with feature flag detail with user string`() {
        every { app.featureFlagDetail(any(), any<String>()) } returns FeatureFlagDecision.on(DecisionReason.DEFAULT_RULE, ParameterConfig.empty())
        val parameters = mapOf("featureKey" to 1, "user" to "abcd1234")
        val jsonString = createJsonString("featureFlagDetail", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.featureFlagDetail(
                withArg { assertThat(it, `is`(1)) },
                withArg<String> { assertThat(it, `is`("abcd1234")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))

            val map = data as Map<*, *>
            assertNull(map["featureFlag"])
            assertThat(map["isOn"], `is`(true))
            assertThat(map["reason"], `is`("DEFAULT_RULE"))

            val config = map["config"] as Map<*, *>
            val configParameters = config["parameters"] as Map<*, *>
            assertThat(configParameters.isEmpty(), `is`(true))
        }
    }

    @Test
    fun `invoke with feature flag detail with user object`() {
        every { app.featureFlagDetail(any(), any<User>()) } returns FeatureFlagDecision.on(DecisionReason.DEFAULT_RULE, ParameterConfig.empty())
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf("foo" to "bar"),
            "properties" to mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            )
        )
        val parameters = mapOf("featureKey" to 1, "user" to user)
        val jsonString = createJsonString("featureFlagDetail", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.featureFlagDetail(
                withArg { assertThat(it, `is`(1)) },
                withArg<User> {
                    assertThat(it.id, `is`("foo"))
                    assertThat(it.userId, `is`("bar"))
                    assertThat(it.deviceId, `is`("abcd1234"))
                    assertThat(it.identifiers.size, `is`(1))
                    assertThat(it.identifiers["foo"], `is`("bar"))
                    assertThat(it.properties.size, `is`(3))
                    assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                    assertThat(it.properties["string"], `is`("text"))
                    val array = it.properties["array"] as ArrayList<*>
                    assertThat(array.size, `is`(2))
                    assertThat((array[0] as Number).toDouble(), `is`(123.0))
                    assertThat(array[1], `is`("123"))
                }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))

            val map = data as Map<*, *>
            assertNull(map["featureFlag"])
            assertThat(map["isOn"], `is`(true))
            assertThat(map["reason"], `is`("DEFAULT_RULE"))

            val config = map["config"] as Map<*, *>
            val configParameters = config["parameters"] as Map<*, *>
            assertThat(configParameters.isEmpty(), `is`(true))
        }
    }

    @Test
    fun `invoke with feature flag detail with invalid parameters case`() {
        val parameters = emptyMap<String, Any>()
        val jsonString = createJsonString("featureFlagDetail", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with track with event string`() {
        val parameters = mapOf("event" to "foo")
        val jsonString = createJsonString("track", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.track(withArg<String> {
                assertThat(it, `is`("foo"))
            })
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with track with event string with user string`() {
        val parameters = mapOf(
            "event" to "foo",
            "user" to "abcd1234"
        )
        val jsonString = createJsonString("track", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.track(
                withArg<String> {
                    assertThat(it, `is`("foo"))
                },
                withArg<String> {
                    assertThat(it, `is`("abcd1234"))
                }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with track with event string with user object`() {
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf("foo" to "bar"),
            "properties" to mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            )
        )
        val parameters = mapOf(
            "event" to "foo",
            "user" to user
        )
        val jsonString = createJsonString("track", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.track(
                withArg<String> {
                    assertThat(it, `is`("foo"))
                },
                withArg<User> {
                    assertThat(it.id, `is`("foo"))
                    assertThat(it.userId, `is`("bar"))
                    assertThat(it.deviceId, `is`("abcd1234"))
                    assertThat(it.identifiers.size, `is`(1))
                    assertThat(it.identifiers["foo"], `is`("bar"))
                    assertThat(it.properties.size, `is`(3))
                    assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                    assertThat(it.properties["string"], `is`("text"))
                    val array = it.properties["array"] as ArrayList<*>
                    assertThat(array.size, `is`(2))
                    assertThat((array[0] as Number).toDouble(), `is`(123.0))
                    assertThat(array[1], `is`("123"))
                }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with track with event object`() {
        val event = mapOf(
            "key" to "foo",
            "value" to 123,
            "properties" to mapOf("abc" to "def")
        )
        val parameters = mapOf("event" to event)
        val jsonString = createJsonString("track", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.track(withArg<Event> {
                assertThat(it.key, `is`("foo"))
                assertThat(it.value, `is`(123.0))
                assertThat(it.properties.size, `is`(1))
                assertThat(it.properties["abc"], `is`("def"))
            })
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with track with event object with user string`() {
        val event = mapOf(
            "key" to "foo",
            "value" to 123,
            "properties" to mapOf("abc" to "def")
        )
        val parameters = mapOf(
            "event" to event,
            "user" to "abcd1234"
        )
        val jsonString = createJsonString("track", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.track(
                withArg<Event> {
                    assertThat(it.key, `is`("foo"))
                    assertThat(it.value, `is`(123.0))
                    assertThat(it.properties.size, `is`(1))
                    assertThat(it.properties["abc"], `is`("def"))
                },
                withArg<String> {
                    assertThat(it, `is`("abcd1234"))
                }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with track with event object with user object`() {
        val event = mapOf(
            "key" to "foo",
            "value" to 123,
            "properties" to mapOf("abc" to "def")
        )
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf("foo" to "bar"),
            "properties" to mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            )
        )
        val parameters = mapOf(
            "event" to event,
            "user" to user
        )
        val jsonString = createJsonString("track", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.track(
                withArg<Event> {
                    assertThat(it.key, `is`("foo"))
                    assertThat(it.value, `is`(123.0))
                    assertThat(it.properties.size, `is`(1))
                    assertThat(it.properties["abc"], `is`("def"))
                },
                withArg<User> {
                    assertThat(it.id, `is`("foo"))
                    assertThat(it.userId, `is`("bar"))
                    assertThat(it.deviceId, `is`("abcd1234"))
                    assertThat(it.identifiers.size, `is`(1))
                    assertThat(it.identifiers["foo"], `is`("bar"))
                    assertThat(it.properties.size, `is`(3))
                    assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                    assertThat(it.properties["string"], `is`("text"))
                    val array = it.properties["array"] as ArrayList<*>
                    assertThat(array.size, `is`(2))
                    assertThat((array[0] as Number).toDouble(), `is`(123.0))
                    assertThat(array[1], `is`("123"))
                }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with track with invalid parameters`() {
        val parameters = emptyMap<String, Any>()
        val jsonString = createJsonString("track", parameters)
        val result = bridge.invoke(jsonString)
        verify { app wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with remote config - string`() {
        every { app.remoteConfig().getString(any(), any()) } returns "foo"
        val parameters = mapOf(
            "key" to "foo",
            "valueType" to "string",
            "defaultValue" to "abc"
        )
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.remoteConfig()
            app.remoteConfig().getString(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`("abc")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("foo"))
        }
    }

    @Test
    fun `invoke with remote config with user string - string`() {
        every { app.remoteConfig(any()).getString(any(), any()) } returns "foo"
        val parameters = mapOf(
            "key" to "foo",
            "valueType" to "string",
            "defaultValue" to "abc",
            "user" to "abcd1234"
        )
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.remoteConfig(
                withArg {
                    assertThat(it.userId, `is`("abcd1234"))
                }
            ).getString(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`("abc")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("foo"))
        }
    }

    @Test
    fun `invoke with remote config with user object - string`() {
        every { app.remoteConfig(any()).getString(any(), any()) } returns "foo"
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf("foo" to "bar"),
            "properties" to mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            )
        )
        val parameters = mapOf(
            "key" to "foo",
            "valueType" to "string",
            "defaultValue" to "abc",
            "user" to user
        )
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.remoteConfig(
                withArg {
                    assertThat(it.id, `is`("foo"))
                    assertThat(it.userId, `is`("bar"))
                    assertThat(it.deviceId, `is`("abcd1234"))
                    assertThat(it.identifiers.size, `is`(1))
                    assertThat(it.identifiers["foo"], `is`("bar"))
                    assertThat(it.properties.size, `is`(3))
                    assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                    assertThat(it.properties["string"], `is`("text"))
                    val array = it.properties["array"] as ArrayList<*>
                    assertThat(array.size, `is`(2))
                    assertThat((array[0] as Number).toDouble(), `is`(123.0))
                    assertThat(array[1], `is`("123"))
                }
            ).getString(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`("abc")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("foo"))
        }
    }

    @Test
    fun `invoke with remote config - number`() {
        every { app.remoteConfig().getDouble(any(), any()) } returns 123.0
        val parameters = mapOf(
            "key" to "foo",
            "valueType" to "number",
            "defaultValue" to 1000.0
        )
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.remoteConfig()
            app.remoteConfig().getDouble(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`(1000.0)) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("123.0"))
        }
    }

    @Test
    fun `invoke with remote config with user string - number`() {
        every { app.remoteConfig(any()).getDouble(any(), any()) } returns 123.0
        val parameters = mapOf(
            "key" to "foo",
            "valueType" to "number",
            "defaultValue" to 1000.0,
            "user" to "abcd1234"
        )
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.remoteConfig(
                withArg {
                    assertThat(it.userId, `is`("abcd1234"))
                }
            ).getDouble(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`(1000.0)) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("123.0"))
        }
    }

    @Test
    fun `invoke with remote config with user object - number`() {
        every { app.remoteConfig(any()).getDouble(any(), any()) } returns 123.0
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf("foo" to "bar"),
            "properties" to mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            )
        )
        val parameters = mapOf(
            "key" to "foo",
            "valueType" to "number",
            "defaultValue" to 1000.0,
            "user" to user
        )
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.remoteConfig(
                withArg {
                    assertThat(it.id, `is`("foo"))
                    assertThat(it.userId, `is`("bar"))
                    assertThat(it.deviceId, `is`("abcd1234"))
                    assertThat(it.identifiers.size, `is`(1))
                    assertThat(it.identifiers["foo"], `is`("bar"))
                    assertThat(it.properties.size, `is`(3))
                    assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                    assertThat(it.properties["string"], `is`("text"))
                    val array = it.properties["array"] as ArrayList<*>
                    assertThat(array.size, `is`(2))
                    assertThat((array[0] as Number).toDouble(), `is`(123.0))
                    assertThat(array[1], `is`("123"))
                }
            ).getDouble(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`(1000.0)) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("123.0"))
        }
    }

    @Test
    fun `invoke with remote config - boolean`() {
        every { app.remoteConfig().getBoolean(any(), any()) } returns true
        val parameters = mapOf(
            "key" to "foo",
            "valueType" to "boolean",
            "defaultValue" to false
        )
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.remoteConfig()
            app.remoteConfig().getBoolean(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`(false)) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("true"))
        }
    }

    @Test
    fun `invoke with remote config with user string - boolean`() {
        every { app.remoteConfig(any()).getBoolean(any(), any()) } returns true
        val parameters = mapOf(
            "key" to "foo",
            "valueType" to "boolean",
            "defaultValue" to false,
            "user" to "abcd1234"
        )
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.remoteConfig(
                withArg {
                    assertThat(it.userId, `is`("abcd1234"))
                }
            ).getBoolean(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`(false)) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("true"))
        }
    }

    @Test
    fun `invoke with remote config with user object - boolean`() {
        every { app.remoteConfig(any()).getBoolean(any(), any()) } returns true
        val user = mapOf(
            "id" to "foo",
            "userId" to "bar",
            "deviceId" to "abcd1234",
            "identifiers" to mapOf("foo" to "bar"),
            "properties" to mapOf(
                "number" to 123,
                "string" to "text",
                "array" to arrayOf(123, "123")
            )
        )
        val parameters = mapOf(
            "key" to "foo",
            "valueType" to "boolean",
            "defaultValue" to false,
            "user" to user
        )
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.remoteConfig(
                withArg {
                    assertThat(it.id, `is`("foo"))
                    assertThat(it.userId, `is`("bar"))
                    assertThat(it.deviceId, `is`("abcd1234"))
                    assertThat(it.identifiers.size, `is`(1))
                    assertThat(it.identifiers["foo"], `is`("bar"))
                    assertThat(it.properties.size, `is`(3))
                    assertThat((it.properties["number"] as Number).toDouble(), `is`(123.0))
                    assertThat(it.properties["string"], `is`("text"))
                    val array = it.properties["array"] as ArrayList<*>
                    assertThat(array.size, `is`(2))
                    assertThat((array[0] as Number).toDouble(), `is`(123.0))
                    assertThat(array[1], `is`("123"))
                }
            ).getBoolean(
                withArg { assertThat(it, `is`("foo")) },
                withArg { assertThat(it, `is`(false)) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertThat(data, `is`("true"))
        }
    }

    @Test
    fun `invoke with remote config with invalid parameters`() {
        val remoteConfig = mockk<HackleRemoteConfig>()
        every { app.remoteConfig() } returns remoteConfig
        val parameters = emptyMap<String, Any>()
        val jsonString = createJsonString("remoteConfig", parameters)
        val result = bridge.invoke(jsonString)
        verify { remoteConfig wasNot Called }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(false))
            assertThat(message.isEmpty(), `is`(false))
            assertNull(data)
        }
    }
    
    @Test
    fun `invoke set current screen with parameters`() {
        every { app.setCurrentScreen(any(), any()) } answers { }
        val parameters = mapOf(
            "screenName" to "mainActivity",
            "className" to "mainActivityClass",
        )
        val jsonString = createJsonString("setCurrentScreen", parameters)
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.setCurrentScreen(
                withArg { assertThat(it, `is`("mainActivity")) },
                withArg { assertThat(it, `is`("mainActivityClass")) }
            )
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
        }
    }

    @Test
    fun `invoke with show user explorer`() {
        every { app.showUserExplorer() } answers { }
        val jsonString = createJsonString("showUserExplorer")
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.showUserExplorer()
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
            assertNull(data)
        }
    }

    @Test
    fun `invoke with hide user explorer`() {
        every { app.hideUserExplorer() } answers { }
        val jsonString = createJsonString("hideUserExplorer")
        val result = bridge.invoke(jsonString)
        verify(exactly = 1) {
            app.hideUserExplorer()
        }
        result.parseJson<BridgeResponse>().apply {
            assertThat(success, `is`(true))
            assertThat(message, `is`("OK"))
        }
    }

    private fun createJsonString(command: String, parameters: Map<String, Any>? = null): String
        = mapOf<String, Any>(
            "_hackle" to mapOf(
                "command" to command,
                "parameters" to parameters
            )
        ).toJson()
}