package io.hackle.android.internal.remoteConfig

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.remoteconfig.HackleRemoteConfigImpl
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.decision.DecisionReason
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


internal class HackleRemoteConfigImplTest {

    @MockK
    private lateinit var core: HackleCore

    @MockK
    private lateinit var userManager: UserManager

    private lateinit var sut: HackleRemoteConfigImpl

    @Before
    fun setUp() {
        userManager = mockk()
        core = mockk(relaxed = true)
        sut = HackleRemoteConfigImpl(mockk(), core, userManager)
    }

    @Test
    fun `getString은 core의 remoteConfig 결과를 반환한다`() {
        val key = "test_string_key"
        val defaultValue = "default"
        val expectedValue = "from_core"
        val mockDecision = RemoteConfigDecision.of(expectedValue, DecisionReason.TRAFFIC_ALLOCATED)
        val mockHackleUser = mockk<HackleUser>()

        every { userManager.resolve(any(), HackleAppContext.DEFAULT) } returns mockHackleUser
        every { core.remoteConfig(key, mockHackleUser, ValueType.STRING, defaultValue) } returns mockDecision

        val actual = sut.getString(key, defaultValue)

        expectThat(actual).isEqualTo(expectedValue)
        verify { userManager.resolve(any(), HackleAppContext.DEFAULT) }
    }

    @Test
    fun `getDouble은 core의 remoteConfig 결과를 double로 변환하여 반환한다`() {
        val key = "test_double_key"
        val defaultValue = 42.0
        val expectedValue = 100.5
        val mockDecision = RemoteConfigDecision.of(expectedValue, DecisionReason.TRAFFIC_ALLOCATED)
        val mockHackleUser = mockk<HackleUser>()

        every { userManager.resolve(any(), HackleAppContext.DEFAULT) } returns mockHackleUser
        every { core.remoteConfig(key, mockHackleUser, ValueType.NUMBER, defaultValue) } returns mockDecision

        val actual = sut.getDouble(key, defaultValue)

        expectThat(actual).isEqualTo(expectedValue)
        verify { userManager.resolve(any(), HackleAppContext.DEFAULT) }
    }
    
    @Test
    fun `getBoolean은 core의 remoteConfig 결과를 boolean로 변환하여 반환한다`() {
        val key = "test_double_key"
        val defaultValue = false
        val expectedValue = true
        val mockDecision = RemoteConfigDecision.of(expectedValue, DecisionReason.TRAFFIC_ALLOCATED)
        val mockHackleUser = mockk<HackleUser>()

        every { userManager.resolve(any(), HackleAppContext.DEFAULT) } returns mockHackleUser
        every { core.remoteConfig(key, mockHackleUser, ValueType.BOOLEAN, defaultValue) } returns mockDecision

        val actual = sut.getBoolean(key, defaultValue)

        expectThat(actual).isEqualTo(expectedValue)
        verify { userManager.resolve(any(), HackleAppContext.DEFAULT) }
    }
}