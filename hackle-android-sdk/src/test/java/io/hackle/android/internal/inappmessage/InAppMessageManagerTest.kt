package io.hackle.android.internal.inappmessage

import io.hackle.android.HackleApp
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.ui.inappmessage.InAppMessageRenderer
import io.hackle.android.ui.inappmessage.base.InAppMessageTrack
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
    fun `triggerDeterminer 에서 보여주기로 결정된 InAppMessageRenderSoruce 를 리턴한다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        val track = mockk<UserEvent.Track>()
        every { workspaceFetcher.fetch() } returns workspace
        every { workspace.inAppMessages } returns listOf(inAppMessage)
        every { inAppMessage.messageContext.messages } returns listOf(mockk())
        every {
            inAppMessageTriggerDeterminer.determine(
                any(),
                any(),
                any()
            )
        } returns InAppMessageRenderSource(
            inAppMessage = inAppMessage,
            message = inAppMessage.messageContext.messages.first(),
            properties = emptyMap()
        )
        every { inAppMessage.key } returns 1L
        every { track.user } returns mockk()

        sut.onEventPublish(track)

        verify(exactly = 1) { inAppMessageTriggerDeterminer.determine(any(), any(), any()) }
        verify(exactly = 0) { inAppMessageRenderer.render(any()) }
    }

    @Test
    fun `currentState 가 FORGROUND 가 아닌 경우 인앱 메시지를 그리지 않는다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        val track = mockk<UserEvent.Track>()
        every { workspaceFetcher.fetch() } returns workspace
        every { workspace.inAppMessages } returns listOf(inAppMessage)
        every { inAppMessage.messageContext.messages } returns listOf(mockk())
        every {
            inAppMessageTriggerDeterminer.determine(
                any(),
                any(),
                any()
            )
        } returns InAppMessageRenderSource(
            inAppMessage = inAppMessage,
            message = inAppMessage.messageContext.messages.first(),
            properties = emptyMap()
        )
        every { inAppMessage.key } returns 1L
        every { appStateManager.currentState } returns AppState.BACKGROUND
        every { track.user } returns mockk()
        every { core.inAppMessage(inAppMessage.key, track.user) } returns InAppMessageDecision(
            inAppMessage = inAppMessage,
            message = mockk(),
            reason = DecisionReason.IN_APP_MESSAGE_TARGET
        )

        sut.onEventPublish(track)

        verify(exactly = 1) { inAppMessageTriggerDeterminer.determine(any(), any(), any()) }
        verify(exactly = 0) { inAppMessageRenderer.render(any()) }
    }


    @Test
    fun `모든 조건이 맞으면 inAppMessageRenderer 을 호출하여 인앱 메시지를 렌더링한다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        val track = mockk<UserEvent.Track>()
        every { workspaceFetcher.fetch() } returns workspace
        every { workspace.inAppMessages } returns listOf(inAppMessage)
        every { inAppMessage.messageContext.messages } returns listOf(mockk())
        every {
            inAppMessageTriggerDeterminer.determine(
                any(),
                any(),
                any()
            )
        } returns InAppMessageRenderSource(
            inAppMessage = inAppMessage,
            message = inAppMessage.messageContext.messages.first(),
            properties = emptyMap()
        )
        every { inAppMessage.key } returns 1L
        every { inAppMessage.id } returns 1L
        every { appStateManager.currentState } returns AppState.FOREGROUND
        every { track.user } returns mockk()
        every { core.inAppMessage(inAppMessage.key, track.user) } returns InAppMessageDecision(
            inAppMessage = inAppMessage,
            message = mockk(),
            reason = DecisionReason.IN_APP_MESSAGE_TARGET
        )

        sut.onEventPublish(track)

        verify(exactly = 1) { inAppMessageTriggerDeterminer.determine(any(), any(), any()) }
        verify(exactly = 1) { inAppMessageRenderer.render(any()) }
    }

    @After
    fun after() {
        unmockkObject(InAppMessageTrack)
    }
}