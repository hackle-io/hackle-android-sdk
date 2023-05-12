package io.hackle.android.internal.inappmessage


import io.hackle.sdk.common.PropertiesBuilder
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
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo


internal class InAppMessageTriggerDeterminerTest {


    @RelaxedMockK
    private lateinit var targetMatcher: TargetMatcher

    @InjectMockKs
    private lateinit var sut: InAppMessageTriggerDeterminer

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `이벤트 트리거 룰의 이벤트 키와 매칭되는 인앱 메시지들만 결과에 포함된다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        val track = mockk<UserEvent.Track>()

        every { inAppMessage.eventTriggerRules } returns listOf(
            InAppMessage.EventTriggerRule(
                "test",
                emptyList()
            )
        )
        every { track.event.key } returns "NOT_MATCH"

        val actual = sut.determine(listOf(inAppMessage), track, workspace)

        expectThat(actual) isEqualTo emptyList()
    }

    @Test
    fun `이벤트 트리거룰의 이벤트 키가 매칭된다면 프로퍼티 조건이 비어있어도 결과에 포함 된다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        val track = mockk<UserEvent.Track>()

        every { inAppMessage.eventTriggerRules } returns listOf(
            InAppMessage.EventTriggerRule(
                "MATCH",
                emptyList()
            )
        )
        every { track.event.key } returns "MATCH"

        val actual = sut.determine(listOf(inAppMessage), track, workspace)
        expectThat(actual.contains(inAppMessage)) isEqualTo true
    }

    @Test
    fun `이벤트 트리거 룰에 프로퍼티 조건이 존재하는데 프로퍼티 조건이 맞지 않으면 결과에 포함되지 않는다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
        val track = mockk<UserEvent.Track>()

        every { inAppMessage.key } returns 123L
        every { track.user } returns mockk()
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
        expectThat(actual.contains(inAppMessage)) isEqualTo false
    }


    @Test
    fun `이벤트 트리거 룰에 프로퍼티 조건이 존재한다면 프로퍼티 조건이 전부 맞아야 결과에 포함된다`() {
        val workspace = mockk<Workspace>()
        val inAppMessage = mockk<InAppMessage>()
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

        every { inAppMessage.key } returns 123L
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
        expectThat(actual.contains(inAppMessage)) isEqualTo true
    }


}