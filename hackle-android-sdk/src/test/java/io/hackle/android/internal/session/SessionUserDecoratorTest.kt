import io.hackle.android.internal.session.Session
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.session.SessionUserDecorator
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

class SessionUserDecoratorTest {

    @MockK
    private lateinit var sessionManager: SessionManager

    @InjectMockKs
    private lateinit var sut: SessionUserDecorator

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `이미 sessionId가 있는 경우 decorate 하지 않음`() {
        // given
        val user = HackleUser.builder().identifier(IdentifierType.SESSION, "session_id").build()

        // when
        val actual = sut.decorate(user)

        // then
        expectThat(actual) isSameInstanceAs user
    }

    @Test
    fun `currentSession이 없는 경우 decorate 하지 않음`() {
        // given
        val user = HackleUser.builder().identifier(IdentifierType.DEVICE, "device_id").build()
        every { sessionManager.currentSession } returns null

        // when
        val actual = sut.decorate(user)

        // then
        expectThat(actual) isSameInstanceAs user
    }

    @Test
    fun `currentSession을 추가한다`() {
        // given
        val user = HackleUser.builder().identifier(IdentifierType.DEVICE, "device_id").build()
        every { sessionManager.currentSession } returns Session("session_01")

        // when
        val actual = sut.decorate(user)

        // then
        expectThat(actual) isEqualTo HackleUser.builder()
            .identifier(IdentifierType.DEVICE, "device_id")
            .identifier(IdentifierType.SESSION, "session_01")
            .build()
    }
}
