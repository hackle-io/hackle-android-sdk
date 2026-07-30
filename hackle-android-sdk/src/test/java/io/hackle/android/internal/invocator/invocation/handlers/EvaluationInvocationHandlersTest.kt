package io.hackle.android.internal.invocator.invocation.handlers

import com.google.gson.GsonBuilder
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.support.assertThrows
import io.hackle.sdk.common.ParameterConfig
import io.hackle.sdk.common.Variation
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.model.ValueType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class EvaluationInvocationHandlersTest {

    private lateinit var core: HackleAppCore
    private val gson = GsonBuilder().serializeNulls().create()

    @Before
    fun setup() {
        core = mockk(relaxUnitFun = true)
    }

    private fun request(command: String, parameters: Map<String, Any?>? = null): InvocationRequest {
        val map = mapOf<String, Any>(
            "_hackle" to mapOf(
                "command" to command,
                "parameters" to parameters,
                "browserProperties" to mapOf("url" to "https://hackle.io")
            )
        )
        return InvocationRequest.parse(gson.toJson(map))
    }

    // AB_TEST - Variation

    @Test
    fun `VariationInvocationHandler - variation 이름을 반환한다`() {
        // given
        every { core.variationDetail(any(), any()) } returns Decision.of(
            Variation.B, DecisionReason.DEFAULT_RULE
        )
        val sut = VariationInvocationHandler(core)
        val params = mapOf<String, Any?>("experimentKey" to 1, "defaultVariation" to "A")

        // when
        val response = sut.invoke(request("variation", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isEqualTo("B")
        }
    }

    @Test
    fun `VariationInvocationHandler - defaultVariation 파라미터는 무시된다`() {
        // given
        every { core.variationDetail(any(), any()) } returns Decision.of(
            Variation.A, DecisionReason.DEFAULT_RULE
        )
        val sut = VariationInvocationHandler(core)
        val params = mapOf<String, Any?>("experimentKey" to 1, "defaultVariation" to "D")

        // when
        val response = sut.invoke(request("variation", params))

        // then
        verify(exactly = 1) {
            core.variationDetail(1L, any<HackleAppContext>())
        }
        expectThat(response) {
            get { data }.isEqualTo("A")
        }
    }

    @Test
    fun `VariationInvocationHandler - user 파라미터는 무시된다`() {
        // given
        every { core.variationDetail(any(), any()) } returns Decision.of(
            Variation.B, DecisionReason.DEFAULT_RULE
        )
        val sut = VariationInvocationHandler(core)
        val params = mapOf<String, Any?>("experimentKey" to 1, "user" to "user-123")

        // when
        sut.invoke(request("variation", params))

        // then
        verify(exactly = 1) {
            core.variationDetail(any(), any<HackleAppContext>())
        }
    }

    @Test
    fun `VariationInvocationHandler - experimentKey가 없으면 예외를 던진다`() {
        val sut = VariationInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("variation", emptyMap()))
        }
    }

    // AB_TEST - VariationDetail

    @Test
    fun `VariationDetailInvocationHandler - DecisionDto를 반환한다`() {
        // given
        every { core.variationDetail(any(), any()) } returns Decision.of(
            Variation.B, DecisionReason.DEFAULT_RULE
        )
        val sut = VariationDetailInvocationHandler(core)
        val params = mapOf<String, Any?>("experimentKey" to 1, "defaultVariation" to "D")

        // when
        val response = sut.invoke(request("variationDetail", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isNotNull().and {
                get { variation }.isEqualTo("B")
                get { reason }.isEqualTo("DEFAULT_RULE")
            }
        }
    }

    // FEATURE_FLAG - IsFeatureOn

    @Test
    fun `IsFeatureOnInvocationHandler - isOn 값을 반환한다`() {
        // given
        every { core.featureFlagDetail(any(), any()) } returns FeatureFlagDecision.on(
            DecisionReason.DEFAULT_RULE, ParameterConfig.empty()
        )
        val sut = IsFeatureOnInvocationHandler(core)
        val params = mapOf<String, Any?>("featureKey" to 42)

        // when
        val response = sut.invoke(request("isFeatureOn", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isEqualTo(true)
        }
    }

    @Test
    fun `IsFeatureOnInvocationHandler - feature off이면 false를 반환한다`() {
        // given
        every { core.featureFlagDetail(any(), any()) } returns FeatureFlagDecision.off(
            DecisionReason.DEFAULT_RULE, ParameterConfig.empty()
        )
        val sut = IsFeatureOnInvocationHandler(core)
        val params = mapOf<String, Any?>("featureKey" to 42)

        // when
        val response = sut.invoke(request("isFeatureOn", params))

        // then
        expectThat(response.data).isEqualTo(false)
    }

    @Test
    fun `IsFeatureOnInvocationHandler - user 파라미터는 무시된다`() {
        // given
        every { core.featureFlagDetail(any(), any()) } returns FeatureFlagDecision.on(
            DecisionReason.DEFAULT_RULE, ParameterConfig.empty()
        )
        val sut = IsFeatureOnInvocationHandler(core)
        val params = mapOf<String, Any?>("featureKey" to 1, "user" to "user-456")

        // when
        sut.invoke(request("isFeatureOn", params))

        // then
        verify(exactly = 1) {
            core.featureFlagDetail(any(), any<HackleAppContext>())
        }
    }

    @Test
    fun `IsFeatureOnInvocationHandler - featureKey가 없으면 예외를 던진다`() {
        val sut = IsFeatureOnInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("isFeatureOn", emptyMap()))
        }
    }

    // FEATURE_FLAG - FeatureFlagDetail

    @Test
    fun `FeatureFlagDetailInvocationHandler - FeatureFlagDecisionDto를 반환한다`() {
        // given
        every { core.featureFlagDetail(any(), any()) } returns FeatureFlagDecision.on(
            DecisionReason.DEFAULT_RULE, ParameterConfig.empty()
        )
        val sut = FeatureFlagDetailInvocationHandler(core)
        val params = mapOf<String, Any?>("featureKey" to 1)

        // when
        val response = sut.invoke(request("featureFlagDetail", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isNotNull().and {
                get { isOn }.isTrue()
                get { reason }.isEqualTo("DEFAULT_RULE")
            }
        }
    }

    // REMOTE_CONFIG

    @Test
    fun `RemoteConfigInvocationHandler - string 타입의 값을 반환한다`() {
        // given
        every { core.remoteConfig(any(), any(), any(), any()).value } returns "hello"
        val sut = RemoteConfigInvocationHandler(core)
        val params = mapOf<String, Any?>("key" to "config_key", "valueType" to "string", "defaultValue" to "default")

        // when
        val response = sut.invoke(request("remoteConfig", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isEqualTo("hello")
        }
        verify(exactly = 1) { core.remoteConfig("config_key", ValueType.STRING, "default", any()) }
    }

    @Test
    fun `RemoteConfigInvocationHandler - number 타입의 값을 반환한다`() {
        // given
        every { core.remoteConfig(any(), any(), any(), any()).value } returns 42.0
        val sut = RemoteConfigInvocationHandler(core)
        val params = mapOf<String, Any?>("key" to "num_key", "valueType" to "number", "defaultValue" to 0.0)

        // when
        val response = sut.invoke(request("remoteConfig", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isEqualTo(42.0)
        }
    }

    @Test
    fun `RemoteConfigInvocationHandler - boolean 타입의 값을 반환한다`() {
        // given
        every { core.remoteConfig(any(), any(), any(), any()).value } returns true
        val sut = RemoteConfigInvocationHandler(core)
        val params = mapOf<String, Any?>("key" to "bool_key", "valueType" to "boolean", "defaultValue" to false)

        // when
        val response = sut.invoke(request("remoteConfig", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isEqualTo(true)
        }
    }

    @Test
    fun `RemoteConfigInvocationHandler - user 파라미터는 무시된다`() {
        // given
        every { core.remoteConfig(any(), any(), any(), any()).value } returns "value"
        val sut = RemoteConfigInvocationHandler(core)
        val params = mapOf<String, Any?>(
            "key" to "k", "valueType" to "string", "defaultValue" to "d", "user" to "user-789"
        )

        // when
        sut.invoke(request("remoteConfig", params))

        // then
        verify(exactly = 1) {
            core.remoteConfig("k", ValueType.STRING, "d", any())
        }
    }

    @Test
    fun `RemoteConfigInvocationHandler - 지원하지 않는 valueType이면 예외를 던진다`() {
        val sut = RemoteConfigInvocationHandler(core)
        val params = mapOf<String, Any?>("key" to "k", "valueType" to "invalid", "defaultValue" to "d")
        assertThrows<IllegalArgumentException> {
            sut.invoke(request("remoteConfig", params))
        }
    }

    @Test
    fun `RemoteConfigInvocationHandler - key가 없으면 예외를 던진다`() {
        val sut = RemoteConfigInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("remoteConfig", emptyMap()))
        }
    }
}
