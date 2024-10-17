package io.hackle.android.internal.event.dedup

import androidx.test.core.app.ApplicationProvider
import io.hackle.android.internal.event.UserEvents
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.Experiment
import io.hackle.sdk.core.user.HackleUser
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@RunWith(RobolectricTestRunner::class)
class ExposureEventDedupDeterminerTest {

    @Test
    fun `supports`() {
        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234", -1)
        expectThat(sut.supports(UserEvents.track("test"))).isFalse()
        expectThat(sut.supports(mockk<UserEvent.Exposure>())).isTrue()
    }

    @Test
    fun `dedupInterval 이 -1 이면 중복제거 하지 않는다`() {

        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",-1)
        val event = event(HackleUser.of("test_id"))

        val actual = sut.isDedupTarget(event)

        assertFalse(actual)
    }

    @Test
    fun `첫 번째 노출이벤트면 중복제거 하지 않는다`() {

        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",1000)
        val event = event(HackleUser.of("test_id"))

        val actual = sut.isDedupTarget(event)

        assertFalse(actual)
    }

    @Test
    fun `같은 사용자의 같은 노출이벤트에 대해 중복제거 기간 이내에 들어온 이벤트는 중복제거 한다`() {

        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",1000)
        val event1 = event(HackleUser.of("test_id"))
        val event2 = event(HackleUser.of("test_id"))

        assertFalse(sut.isDedupTarget(event1))
        assertTrue(sut.isDedupTarget(event2))
    }

    @Test
    fun `같은 사용자의 같은 노출이벤트지만 중복제거 기간 이후에 들어오면 중복제거 하지 않는다`() {
        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",100)

        val user = HackleUser.of("test_id")
        val event1 = event(user)
        val event2 = event(user)

        assertFalse(sut.isDedupTarget(event1))
        Thread.sleep(1000)
        assertFalse(sut.isDedupTarget(event2))
    }

    @Test
    fun `같은 사용자의 중복제거 기간 이내지만 다른 실험에 대한 분배면 중복제거 하지 않는다`() {
        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",1000)

        val user = HackleUser.of("test_id")
        val event1 = event(user, 1)
        val event2 = event(user, 2)

        assertFalse(sut.isDedupTarget(event1))
        assertFalse(sut.isDedupTarget(event2))
    }

    @Test
    fun `같은 사용자의 중복제거 기간 이내지만 분배사유가 변경되면 중복제거 하지 않는다`() {
        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",1000)

        val user = HackleUser.of("test_id")
        val event1 = event(user, decisionReason = DecisionReason.TRAFFIC_ALLOCATED)
        val event2 = event(user, decisionReason = DecisionReason.EXPERIMENT_PAUSED)

        assertFalse(sut.isDedupTarget(event1))
        assertFalse(sut.isDedupTarget(event2))
    }

    @Test
    fun `사용자의 속성이 변경되어도 식별자만 같으면 같은 사용자로 판단하고 중복제거한다`() {
        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",1000)

        val event1 = event(HackleUser.of(User.builder("test_id").build()))
        val event2 = event(HackleUser.of(User.builder("test_id").property("age", 30).build()))

        assertFalse(sut.isDedupTarget(event1))
        assertTrue(sut.isDedupTarget(event2))
    }

    @Test
    fun TC1() {
        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",1000)

        val userA = HackleUser.of("a")
        val userB = HackleUser.of("b")

        val event1 = event(userA)
        val event2 = event(userA)
        val event3 = event(userB)
        val event4 = event(userA)

        assertFalse(sut.isDedupTarget(event1))
        assertTrue(sut.isDedupTarget(event2))
        assertFalse(sut.isDedupTarget(event3))
        assertFalse(sut.isDedupTarget(event4))
    }

    @Test
    fun TC2() {
        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",1000)

        val userA = HackleUser.of("a")
        val userAA = HackleUser.of(User.builder("a").userId("aa").build())

        val event1 = event(userA)
        val event2 = event(userA)
        val event3 = event(userAA)
        val event4 = event(userA)

        assertFalse(sut.isDedupTarget(event1))
        assertTrue(sut.isDedupTarget(event2))
        assertFalse(sut.isDedupTarget(event3))
        assertFalse(sut.isDedupTarget(event4))
    }

    @Test
    fun TC3() {
        val sut = ExposureEventDedupDeterminer(ApplicationProvider.getApplicationContext(), "abce1234",1000)

        val userA = HackleUser.of("a")

        val event1 = event(userA, experimentId = 1)
        val event2 = event(userA, experimentId = 2)
        val event3 = event(userA, experimentId = 1)
        val event4 = event(userA, experimentId = 2)

        assertFalse(sut.isDedupTarget(event1))
        assertFalse(sut.isDedupTarget(event2))
        assertTrue(sut.isDedupTarget(event3))
        assertTrue(sut.isDedupTarget(event4))
    }

    private fun event(
        user: HackleUser,
        experimentId: Long = 1,
        decisionReason: DecisionReason = DecisionReason.TRAFFIC_ALLOCATED,
    ): UserEvent.Exposure {
        return mockk {
            every { this@mockk.user } returns user
            every { this@mockk.experiment } returns experiment(experimentId)
            every { this@mockk.variationId } returns 1
            every { this@mockk.variationKey } returns "A"
            every { this@mockk.decisionReason } returns decisionReason
        }
    }

    private fun experiment(id: Long): Experiment {
        return mockk {
            every { this@mockk.id } returns id
        }
    }
}