package io.hackle.android.internal.inappmessage

import io.hackle.android.Hackle
import io.hackle.android.HackleApp
import io.hackle.android.app
import io.hackle.android.inappmessage.InAppMessageRenderer
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.decision.InAppMessageDecision
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.WorkspaceFetcher
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test


class InAppMessageManagerTest {

    @RelaxedMockK
    private lateinit var workspaceFetcher: WorkspaceFetcher

    @RelaxedMockK
    private lateinit var core: HackleCore

    @RelaxedMockK
    private lateinit var appStateManager: AppStateManager

    @RelaxedMockK
    private lateinit var inAppMessageRenderer: InAppMessageRenderer

    @RelaxedMockK
    private lateinit var inAppMessageTriggerDeterminer: InAppMessageTriggerDeterminer

    @InjectMockKs
    private lateinit var sut: InAppMessageManager


    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(HackleApp.Companion)
        every { Hackle.app.track(any<Event>()) } returns mockk()
    }

    @Test
    fun `워크스페이스 정보를 가져 오지 못한 경우 판단하지 않고 return 한다`() {
        every { workspaceFetcher.fetch() } returns null

        val track = mockk<UserEvent.Track>()
        sut.onEventPublish(track)

        verify(exactly = 0) { inAppMessageTriggerDeterminer.determine(any(), any(), any()) }
        verify(exactly = 0) { core.inAppMessage(any(), any()) }
        verify(exactly = 0) { inAppMessageRenderer.render(any()) }

    }

    @Test
    fun `트랙 이벤트가 아니면 판단하지 않고 return 한다 `() {
        every { workspaceFetcher.fetch() } returns mockk()

        val exposure = mockk<UserEvent.Exposure>()
        sut.onEventPublish(exposure)

        verify(exactly = 0) { inAppMessageTriggerDeterminer.determine(any(), any(), any()) }
        verify(exactly = 0) { core.inAppMessage(any(), any()) }
        verify(exactly = 0) { inAppMessageRenderer.render(any()) }

    }


    @Test
    fun `triggerDeterminer 에서 보여주기로 판단된 캠페인들만 core 에서 판단한다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        every { workspaceFetcher.fetch() } returns workspace
        every { workspace.inAppMessages } returns listOf(inAppMessage)
        every { inAppMessageTriggerDeterminer.determine(any(), any(), any()) } returns listOf(
            inAppMessage
        )
        every { inAppMessage.key } returns 1L


        val track = mockk<UserEvent.Track>()
        every { track.user } returns mockk()

        sut.onEventPublish(track)

        verify(exactly = 1) { inAppMessageTriggerDeterminer.determine(any(), any(), any()) }
        verify(exactly = 1) { core.inAppMessage(any(), any()) }
        verify(exactly = 0) { inAppMessageRenderer.render(any()) }
    }

    @Test
    fun `currentState 가 FORGROUND 가 아닌 경우 인앱 메시지를 그리지 않는다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        every { workspaceFetcher.fetch() } returns workspace
        every { workspace.inAppMessages } returns listOf(inAppMessage)
        every { inAppMessageTriggerDeterminer.determine(any(), any(), any()) } returns listOf(
            inAppMessage
        )
        every { inAppMessage.key } returns 1L
        every { appStateManager.currentState } returns AppState.BACKGROUND

        val track = mockk<UserEvent.Track>()
        every { track.user } returns mockk()

        every { core.inAppMessage(inAppMessage.key, track.user) } returns InAppMessageDecision(
            inAppMessage = inAppMessage,
            message = mockk(),
            reason = DecisionReason.IN_APP_MESSAGE_TARGET,
            isShow = true
        )

        sut.onEventPublish(track)

        verify(exactly = 1) { inAppMessageTriggerDeterminer.determine(any(), any(), any()) }
        verify(exactly = 1) { core.inAppMessage(any(), any()) }
        verify(exactly = 0) { inAppMessageRenderer.render(any()) }
    }


    @Test
    fun `모든 조건이 맞으면 inAppMessageRenderer 을 호출하여 인앱 메시지를 렌더링하고 impression 이벤트를 발생시킨다`(){
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        every { workspaceFetcher.fetch() } returns workspace
        every { workspace.inAppMessages } returns listOf(inAppMessage)
        every { inAppMessageTriggerDeterminer.determine(any(), any(), any()) } returns listOf(
            inAppMessage
        )
        every { inAppMessage.key } returns 1L
        every { appStateManager.currentState } returns AppState.FOREGROUND

        val track = mockk<UserEvent.Track>()
        every { track.user } returns mockk()

        every { core.inAppMessage(inAppMessage.key, track.user) } returns InAppMessageDecision(
            inAppMessage = inAppMessage,
            message = mockk(),
            reason = DecisionReason.IN_APP_MESSAGE_TARGET,
            isShow = true
        )

        sut.onEventPublish(track)

        verify(exactly = 1) { inAppMessageTriggerDeterminer.determine(any(), any(), any()) }
        verify(exactly = 1) { core.inAppMessage(any(), any()) }
        verify(exactly = 1) { HackleApp.Companion.getInstance().track(any<Event>())}
        verify(exactly = 1) { inAppMessageRenderer.render(any()) }
    }


    @After
    fun after(){
        unmockkObject(HackleApp.Companion)
    }


}