package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.android.support.InAppMessages
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluator
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.hackle.sdk.core.workspace.Workspace
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs

class InAppMessageLayoutResolverTest {

    @MockK
    private lateinit var core: HackleCore

    @MockK
    private lateinit var layoutEvaluator: InAppMessageLayoutEvaluator

    @InjectMockKs
    private lateinit var sut: InAppMessageLayoutResolver

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `resolve`() {
        // given
        val workspace = mockk<Workspace>()
        val inAppMessage = InAppMessages.create()
        val user = HackleUser.builder().identifier(IdentifierType.DEVICE, "device").build()

        val layoutEvaluation = InAppMessages.layoutEvaluation()
        every { core.inAppMessage(any(), any(), layoutEvaluator) } returns layoutEvaluation

        // when
        val actual = sut.resolve(workspace, inAppMessage, user)

        // then
        expectThat(actual) isSameInstanceAs layoutEvaluation
    }
}
