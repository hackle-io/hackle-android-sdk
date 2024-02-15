package io.hackle.android.internal.workspace.dto

import io.hackle.sdk.core.model.InAppMessage
import org.junit.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

internal class InAppMessageDtoTest {

    @Test
    fun `inAppMessage config test`() {
        val workspace = ResourcesWorkspaceFetcher("inappmessage.json").fetch()

        expectThat(workspace.getInAppMessageOrNull(1L))
            .isNotNull()
            .InAppMessage(1L, 1L, InAppMessage.Status.PAUSE)
            .and {
                get { targetContext.targets } isEqualTo emptyList()
                get { targetContext.overrides } isEqualTo emptyList()
            }
            .and {
                get { messageContext }
                    .and {
                        get { defaultLang } isEqualTo "ko"
                        get { experimentContext }.isNull()
                        get { platformTypes } isEqualTo listOf(
                            InAppMessage.PlatformType.ANDROID,
                            InAppMessage.PlatformType.IOS
                        )
                        get { orientations } isEqualTo listOf(
                            InAppMessage.Orientation.VERTICAL,
                            InAppMessage.Orientation.HORIZONTAL
                        )
                    }
            }
            .and {
                get { period } isEqualTo InAppMessage.Period.Always
            }

        expectThat(workspace.getInAppMessageOrNull(2))
            .isNotNull()
            .and {
                get { this.messageContext.experimentContext } isEqualTo InAppMessage.ExperimentContext(42)
            }

    }

    @Test
    fun `inAppMessage unsupported config test`() {
        val workspace = ResourcesWorkspaceFetcher("unsupported_inappmessage.json").fetch()

        //timeUnit
        expectThat(workspace.getInAppMessageOrNull(1L))
            .isNull()

        //status
        expectThat(workspace.getInAppMessageOrNull(2L))
            .isNull()

        //messageContext platformTypes
        expectThat(workspace.getInAppMessageOrNull(3L))
            .isNull()

        //messageContext orientations
        expectThat(workspace.getInAppMessageOrNull(4L))

        //message ImageOrientation
        expectThat(workspace.getInAppMessageOrNull(6L))
            .isNull()

        //message Button ActionType
        expectThat(workspace.getInAppMessageOrNull(7L))
            .isNull()

        //message Button ActionBehavior
        expectThat(workspace.getInAppMessageOrNull(8L))
            .isNull()

        //message Image Button ActionType
        expectThat(workspace.getInAppMessageOrNull(9L))
            .isNull()

        //message Image ActionBehavior
        expectThat(workspace.getInAppMessageOrNull(10L))
            .isNull()


        //message layout DisplayType
        expectThat(workspace.getInAppMessageOrNull(11L))
            .isNull()


        //message layoutType
        expectThat(workspace.getInAppMessageOrNull(12L))
            .isNull()


        //eventTriggerRule target
        expectThat(workspace.getInAppMessageOrNull(13L))
            .isNotNull()
            .hasEventTriggerRules(
                InAppMessage.EventTrigger.Rule("home", emptyList())
            )

        //targetContext target
        expectThat(workspace.getInAppMessageOrNull(14L))
            .isNotNull()
            .hasTargetContext(
                InAppMessage.TargetContext(
                    emptyList(),
                    overrides = emptyList()
                )
            )

    }

    @Test
    fun `inAppMessage unsupported config test 2`() {
        val workspace = ResourcesWorkspaceFetcher("iam_invalid.json").fetch()
        expectThat(workspace.inAppMessages) {
            get { size } isEqualTo 0
        }

    }

    private fun Assertion.Builder<InAppMessage>.InAppMessage(
        id: Long,
        key: Long,
        status: InAppMessage.Status
    ) = compose("InAppMessage") {
        get("InAppMessage.id") { this.id } isEqualTo id
        get("InAppMessage.key") { this.key } isEqualTo key
        get("InAppMessage.status") { this.status } isEqualTo status
    } then {
        if (allPassed) pass() else fail()
    }

    private fun Assertion.Builder<InAppMessage>.hasEventTriggerRules(vararg rules: InAppMessage.EventTrigger.Rule) =
        assert("InAppMessage.eventTriggerRules") {
            val actual = rules.toList()
            if (it.eventTrigger.rules == actual) {
                pass()
            } else {
                fail(actual)
            }
        }

    private fun Assertion.Builder<InAppMessage>.hasTargetContext(targetContext: InAppMessage.TargetContext) =
        assert("InAppMessage.targetContext") {
            if (it.targetContext == targetContext) {
                pass()
            } else {
                fail(targetContext)
            }
        }
}