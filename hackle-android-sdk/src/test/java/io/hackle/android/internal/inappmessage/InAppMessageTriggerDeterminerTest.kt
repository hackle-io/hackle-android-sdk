package io.hackle.android.internal.inappmessage


import io.hackle.android.internal.monitoring.metric.DecisionMetrics
import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.decision.InAppMessageDecision
import io.hackle.sdk.core.evaluation.match.TargetMatcher
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.ValueType
import io.hackle.sdk.core.workspace.Workspace
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
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

internal class InAppMessageTriggerDeterminerTest {


    @RelaxedMockK
    private lateinit var targetMatcher: TargetMatcher

    @RelaxedMockK
    private lateinit var core: HackleCore

    @InjectMockKs
    private lateinit var sut: InAppMessageTriggerDeterminer

    private lateinit var decision: InAppMessageDecision
    private lateinit var inAppMessage: InAppMessage
    private lateinit var message: InAppMessage.MessageContext.Message

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(DecisionMetrics)
        mockkObject(InAppMessageTriggerDeterminer.InAppMessageRequest.Companion)

        inAppMessage = mockk()
        message = mockk()
        decision = mockk()
        every { decision.inAppMessage } returns inAppMessage
        every { decision.message } returns message
        every { inAppMessage.key } returns 123L
        every { DecisionMetrics.inAppMessage(any(), any(), any()) } returns mockk()
        every {
            InAppMessageTriggerDeterminer.InAppMessageRequest.Companion.of(
                any(),
                any(),
                any()
            )
        } returns mockk()

    }


    @Test
    fun `유저 이벤트가 Track 타입이 아니면 null 을 리턴 한다`() {
        val exposure = mockk<UserEvent.Exposure>()
        val workspace = mockk<Workspace>()

        val actual = sut.determine(listOf(inAppMessage), exposure, workspace)

        expectThat(actual).isNull()
    }

    @Test
    fun `이벤트 트리거 룰의 이벤트 키와 매칭되는 인앱 메시지들이 없으면 core 로직을 타지 않는다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        val track = mockk<UserEvent.Track>()

        every { inAppMessage.key } returns 123L
        every { inAppMessage.eventTriggerRules } returns listOf(
            InAppMessage.EventTriggerRule(
                "test",
                emptyList()
            )
        )

        every { decision.isShow } returns true
        every { core.inAppMessage(any(), any()) } returns decision
        every { track.event.key } returns "NOT_MATCH"

        val actual = sut.determine(listOf(inAppMessage), track, workspace)

        verify(exactly = 0) { decision.isShow }
        expectThat(actual).isNull()

    }

    @Test
    fun `이벤트 트리거룰의 이벤트 키가 매칭된다면 프로퍼티 조건이 비어있어도 core 로직을 탄다`() {
        val workspace = mockk<Workspace>()
        val track = mockk<UserEvent.Track>()
        every { track.user } returns mockk()
        every { decision.isShow } returns true
        every { core.inAppMessage(any(), any()) } returns decision
        every { inAppMessage.eventTriggerRules } returns listOf(
            InAppMessage.EventTriggerRule(
                "MATCH",
                emptyList()
            )
        )
        every { track.event.key } returns "MATCH"

        val actual = sut.determine(listOf(inAppMessage), track, workspace)

        verify(exactly = 1) { decision.isShow }
        expectThat(actual).isNotNull()

    }

    @Test
    fun `이벤트 트리거 룰에 프로퍼티 조건이 존재하는데 프로퍼티 조건이 맞지 않으면 core 로직을 타지 않는다`() {
        val workspace = mockk<Workspace>()
        val track = mockk<UserEvent.Track>()
        every { track.user } returns mockk()
        every { decision.isShow } returns true
        every { core.inAppMessage(any(), any()) } returns decision
        every { inAppMessage.eventTriggerRules } returns listOf(
            InAppMessage.EventTriggerRule(
                "MATCH",
                listOf(
                    Target(
                        conditions = listOf(
                            Target.Condition(
                                key = Target.Key(Target.Key.Type.EVENT_PROPERTY, "age"),
                                match = Target.Match(
                                    Target.Match.Type.MATCH,
                                    operator = Target.Match.Operator.IN,
                                    ValueType.NUMBER,
                                    values = listOf(22)
                                )
                            )
                        )
                    )
                )
            )
        )
        every { track.event.key } returns "MATCH"

        val actual = sut.determine(listOf(inAppMessage), track, workspace)
        verify(exactly = 0) { decision.isShow }
        expectThat(actual).isNull()

    }


    @Test
    fun `이벤트 트리거 룰에 프로퍼티 조건이 존재한다면 프로퍼티 조건이 전부 맞아야 core 로직을 탄다`() {
        val workspace = mockk<Workspace>()
        val track = mockk<UserEvent.Track>()
        val target = Target(
            conditions = listOf(
                Target.Condition(
                    key = Target.Key(Target.Key.Type.EVENT_PROPERTY, "age"),
                    match = Target.Match(
                        Target.Match.Type.MATCH,
                        operator = Target.Match.Operator.IN,
                        ValueType.NUMBER,
                        values = listOf(22)
                    )
                )
            )
        )
        every { decision.isShow } returns true
        every { core.inAppMessage(any(), any()) } returns decision
        every { track.user } returns mockk()
        every { track.event.properties } returns PropertiesBuilder().add(mapOf("age" to 22)).build()
        every { inAppMessage.eventTriggerRules } returns listOf(
            InAppMessage.EventTriggerRule(
                "MATCH",
                listOf(
                    target
                )
            )
        )
        every { targetMatcher.matches(any(), any(), target) } returns true
        every { track.event.key } returns "MATCH"

        val actual = sut.determine(listOf(inAppMessage), track, workspace)
        verify(exactly = 1) { decision.isShow }
        expectThat(actual).isNotNull()
    }

    @Test
    fun `트리거 조건에도 맞고 코어 로직 결과 보여주기로 했다면 InAppMessageRenderSource 를 리턴한다`() {
        val workspace = mockk<Workspace>()
        val track = mockk<UserEvent.Track>()
        every { decision.isShow } returns true
        every { core.inAppMessage(any(), any()) } returns decision
        every { inAppMessage.eventTriggerRules } returns listOf(
            InAppMessage.EventTriggerRule(
                "MATCH",
                emptyList()
            )
        )
        every { track.event.key } returns "MATCH"
        every { track.user } returns mockk()

        val actual = sut.determine(listOf(inAppMessage), track, workspace)

        expectThat(actual) {
            get { inAppMessage } isEqualTo decision.inAppMessage
            get { message } isEqualTo decision.message
        }
    }


    @After
    fun clear() {
        unmockkObject(DecisionMetrics)
        unmockkObject(InAppMessageTriggerDeterminer.InAppMessageRequest.Companion)
    }
}