package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEvents
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresenter
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.support.InAppMessages
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs

class InAppMessageManagerTest {

    @RelaxedMockK
    private lateinit var determiner: InAppMessageDeterminer

    @RelaxedMockK
    @MockK
    private lateinit var presenter: InAppMessagePresenter

    @RelaxedMockK
    private lateinit var appStateManager: AppStateManager

    @InjectMockKs
    private lateinit var sut: InAppMessageManager

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { appStateManager.currentState } returns AppState.FOREGROUND
    }

    @Test
    fun `when cannot determine message then should not present`() {
        // given
        every { determiner.determineOrNull(any()) } returns null

        // when
        sut.onEvent(mockk())

        // then
        verify {
            presenter wasNot Called
        }
    }

    @Test
    fun `when exception occurs while determining message then should not present`() {
        // given
        every { determiner.determineOrNull(any()) } throws IllegalArgumentException("fail")

        // when
        sut.onEvent(mockk())

        // then
        verify {
            presenter wasNot Called
        }
    }

    @Test
    fun `when current state is not FOREGROUND then should not present`() {
        // given
        val context = InAppMessages.context()
        every { determiner.determineOrNull(any()) } returns context
        every { appStateManager.currentState } returns AppState.BACKGROUND

        // when
        sut.onEvent(mockk())

        // then
        verify {
            presenter wasNot Called
        }
    }

    @Test
    fun `when message is determined then present the message`() {
        // given
        val context = InAppMessages.context()
        every { determiner.determineOrNull(any()) } returns context

        // when
        sut.onEvent(UserEvents.track("test"))

        // then
        verify(exactly = 1) {
            presenter.present(withArg { expectThat(it) isSameInstanceAs context })
        }
    }
}
