package io.hackle.android.internal.remoteconfig

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.DecisionReason.REMOTE_CONFIG_PARAMETER_NOT_FOUND
import io.hackle.sdk.common.decision.RemoteConfigDecision
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.model.ValueType
import io.hackle.sdk.core.user.HackleUser
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo


internal class ContextRemoteConfigTest {

    @MockK
    private lateinit var core: HackleCore

    @MockK
    private lateinit var userManager: UserManager

    private lateinit var sut: ContextRemoteConfig
    
    private var hackleAppContext = HackleAppContext.create(mapOf(
        "test_string_key" to "from_context",
        "test_double_key" to 100.5,
        "test_boolean_key" to true
    ))

    @Before
    fun setUp() {
        userManager = mockk()
        core = mockk(relaxed = true)
        val remoteConfigProcess = RemoteConfigProcessor(core, userManager)
        sut = ContextRemoteConfig(remoteConfigProcess, mockk(), hackleAppContext)
    }

    @Test
    fun `getString은 core의 remoteConfig 결과를 반환한다`() {
        val key = "test_string_key"
        val defaultValue = "default"
        val expectedValue = "from_core"
        val mockDecision = RemoteConfigDecision.of(expectedValue, DecisionReason.TRAFFIC_ALLOCATED)
        val mockHackleUser = mockk<HackleUser>()

        every { userManager.resolve(any(), hackleAppContext) } returns mockHackleUser
        every { core.remoteConfig(key, mockHackleUser, ValueType.STRING, defaultValue) } returns mockDecision

        val actual = sut.getString(key, defaultValue)

        expectThat(actual).isEqualTo(expectedValue)
        verify { userManager.resolve(any(), hackleAppContext) }
    }


    @Test
    fun `getInt은 core의 remoteConfig 결과를 int로 변환하여 반환한다`() {
        val key = "test_int_key"
        val defaultValue = 42
        val expectedValue = 100
        val mockDecision = RemoteConfigDecision.of(expectedValue, DecisionReason.TRAFFIC_ALLOCATED)
        val mockHackleUser = mockk<HackleUser>()

        every { userManager.resolve(any(), hackleAppContext) } returns mockHackleUser
        every { core.remoteConfig(key, mockHackleUser, ValueType.NUMBER, defaultValue) } returns mockDecision

        val actual = sut.getInt(key, defaultValue)

        expectThat(actual).isEqualTo(expectedValue)
        verify { userManager.resolve(any(), hackleAppContext) }
    }


    @Test
    fun `getLong은 core의 remoteConfig 결과를 Long으로 변환하여 반환한다`() {
        val key = "test_long_key"
        val defaultValue = 42L
        val expectedValue = 100L
        val mockDecision = RemoteConfigDecision.of(expectedValue, DecisionReason.TRAFFIC_ALLOCATED)
        val mockHackleUser = mockk<HackleUser>()

        every { userManager.resolve(any(), hackleAppContext) } returns mockHackleUser
        every { core.remoteConfig(key, mockHackleUser, ValueType.NUMBER, defaultValue) } returns mockDecision

        val actual = sut.getLong(key, defaultValue)

        expectThat(actual).isEqualTo(expectedValue)
        verify { userManager.resolve(any(), hackleAppContext) }
    }



    @Test
    fun `getDouble은 core의 remoteConfig 결과를 double로 변환하여 반환한다`() {
        val key = "test_double_key"
        val defaultValue = 42.0
        val expectedValue = 100.5
        val mockDecision = RemoteConfigDecision.of(expectedValue, DecisionReason.TRAFFIC_ALLOCATED)
        val mockHackleUser = mockk<HackleUser>()

        every { userManager.resolve(any(), hackleAppContext) } returns mockHackleUser
        every { core.remoteConfig(key, mockHackleUser, ValueType.NUMBER, defaultValue) } returns mockDecision

        val actual = sut.getDouble(key, defaultValue)

        expectThat(actual).isEqualTo(expectedValue)
        verify { userManager.resolve(any(), hackleAppContext) }
    }

    @Test
    fun `getBoolean은 core의 remoteConfig 결과를 boolean로 변환하여 반환한다`() {
        val key = "test_double_key"
        val defaultValue = false
        val expectedValue = true
        val mockDecision = RemoteConfigDecision.of(expectedValue, DecisionReason.TRAFFIC_ALLOCATED)
        val mockHackleUser = mockk<HackleUser>()

        every { userManager.resolve(any(), hackleAppContext) } returns mockHackleUser
        every { core.remoteConfig(key, mockHackleUser, ValueType.BOOLEAN, defaultValue) } returns mockDecision

        val actual = sut.getBoolean(key, defaultValue)

        expectThat(actual).isEqualTo(expectedValue)
        verify { userManager.resolve(any(), hackleAppContext) }
    }

    @Test
    fun `알 수 없는 값은 defaultValue를 반환한다`() {
        val key = "test_double_key"
        val defaultValue = 42.0
        val mockDecision = RemoteConfigDecision.of(defaultValue, REMOTE_CONFIG_PARAMETER_NOT_FOUND)
        val mockHackleUser = mockk<HackleUser>()

        every { userManager.resolve(any(), hackleAppContext) } returns mockHackleUser
        every { core.remoteConfig(key, mockHackleUser, ValueType.NUMBER, defaultValue) } returns mockDecision

        val actual = sut.getDouble(key, defaultValue)

        expectThat(actual).isEqualTo(defaultValue)
        verify { userManager.resolve(any(), hackleAppContext) }
    }
}
