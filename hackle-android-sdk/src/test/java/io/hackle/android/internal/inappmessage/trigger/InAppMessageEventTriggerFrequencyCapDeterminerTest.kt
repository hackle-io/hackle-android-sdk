package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.event.UserEvents
import io.hackle.android.internal.inappmessage.storage.InAppMessageImpression
import io.hackle.android.internal.inappmessage.storage.InAppMessageImpressionStorage
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.hackle.sdk.core.workspace.Workspace
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InAppMessageEventTriggerFrequencyCapDeterminerTest {

    private lateinit var storage: InAppMessageImpressionStorage
    private lateinit var sut: InAppMessageEventTriggerFrequencyCapDeterminer

    private lateinit var workspace: Workspace

    @Before
    fun before() {
        storage = InAppMessageImpressionStorage(MapKeyValueRepository())
        sut = InAppMessageEventTriggerFrequencyCapDeterminer(storage)
        workspace = mockk()
    }

    @Test
    fun `when frequencyCap is null then returns true`() {
        // given
        val inAppMessage = InAppMessages.create(eventTrigger = InAppMessages.eventTrigger(frequencyCap = null))
        val event = UserEvents.track("test")

        // when
        val actual = sut.isTriggerTarget(workspace, inAppMessage, event)

        // then
        expectThat(actual).isTrue()
    }

    @Test
    fun `when frequencyCap is empty then returns true`() {
        // given
        val inAppMessage = InAppMessages.create(
            eventTrigger = InAppMessages.eventTrigger(
                frequencyCap = InAppMessages.frequencyCap(
                    identifierCaps = listOf(),
                    durationCap = null
                )
            )
        )
        val event = UserEvents.track("test")

        // when
        val actual = sut.isTriggerTarget(workspace, inAppMessage, event)

        // then
        expectThat(actual).isTrue()
    }

    @Test
    fun `identifier cap`() {
        val inAppMessage = InAppMessages.create(
            eventTrigger = InAppMessages.eventTrigger(
                frequencyCap = InAppMessages.frequencyCap(
                    identifierCaps = listOf(
                        InAppMessage.EventTrigger.IdentifierCap("\$id", 3)
                    ),
                    durationCap = null
                )
            )
        )
        val user = HackleUser.builder().identifier("\$id", "user").build()
        val event = UserEvents.track("test", user = user)


        storage.set(
            inAppMessage, listOf(
                impression(user, 1),
                impression(user, 2),
            )
        )
        expectThat(sut.isTriggerTarget(workspace, inAppMessage, event)).isTrue()

        storage.set(
            inAppMessage, listOf(
                impression(user, 1),
                impression(user, 2),
                impression(user, 3),
            )
        )
        expectThat(sut.isTriggerTarget(workspace, inAppMessage, event)).isFalse()
    }

    @Test
    fun `duration cap`() {
        val inAppMessage = InAppMessages.create(
            eventTrigger = InAppMessages.eventTrigger(
                frequencyCap = InAppMessages.frequencyCap(
                    identifierCaps = listOf(),
                    durationCap = InAppMessage.EventTrigger.DurationCap(durationMillis = 10, count = 3)
                )
            )
        )
        val user = HackleUser.builder().identifier("\$id", "user").build()
        val event = UserEvents.track("test", user = user, timestamp = 50)


        storage.set(
            inAppMessage, listOf(
                impression(user, 40),
                impression(user, 41),
                impression(user, 42),
            )
        )
        expectThat(sut.isTriggerTarget(workspace, inAppMessage, event)).isFalse()

        storage.set(
            inAppMessage, listOf(
                impression(user, 39),
                impression(user, 40),
                impression(user, 41),
            )
        )
        expectThat(sut.isTriggerTarget(workspace, inAppMessage, event)).isTrue()
    }

    @Test
    fun `IdentifierCapPredicate - when user identifier is null then false`() {
        val cap = InAppMessage.EventTrigger.IdentifierCap("\$id", 42)
        val sut = InAppMessageEventTriggerFrequencyCapDeterminer.IdentifierCapPredicate(cap)

        val event = UserEvents.track("test", HackleUser.builder().identifier(IdentifierType.DEVICE, "a").build())
        val impression = InAppMessageImpression(mapOf("\$id" to "a"), 42)

        expectThat(sut.matches(event, impression)).isFalse()
    }

    @Test
    fun `IdentifierCapPredicate - when impression identifier is null then false`() {
        val cap = InAppMessage.EventTrigger.IdentifierCap("\$id", 42)
        val sut = InAppMessageEventTriggerFrequencyCapDeterminer.IdentifierCapPredicate(cap)

        val event = UserEvents.track("test", HackleUser.builder().identifier(IdentifierType.ID, "a").build())
        val impression = InAppMessageImpression(mapOf("\$deviceID" to "a"), 42)

        expectThat(sut.matches(event, impression)).isFalse()
    }

    @Test
    fun `IdentifierCapPredicate - nat matches`() {
        val cap = InAppMessage.EventTrigger.IdentifierCap("\$id", 42)
        val sut = InAppMessageEventTriggerFrequencyCapDeterminer.IdentifierCapPredicate(cap)

        val event = UserEvents.track("test", HackleUser.builder().identifier(IdentifierType.ID, "a").build())
        val impression = InAppMessageImpression(mapOf("\$id" to "b"), 42)

        expectThat(sut.matches(event, impression)).isFalse()
    }

    @Test
    fun `IdentifierCapPredicate - matches`() {
        val cap = InAppMessage.EventTrigger.IdentifierCap("\$id", 42)
        val sut = InAppMessageEventTriggerFrequencyCapDeterminer.IdentifierCapPredicate(cap)

        val event = UserEvents.track("test", HackleUser.builder().identifier(IdentifierType.ID, "a").build())
        val impression = InAppMessageImpression(mapOf("\$id" to "a"), 42)

        expectThat(sut.matches(event, impression)).isTrue()
    }

    @Test
    fun `DurationCapPredicate`() {
        val cap = InAppMessage.EventTrigger.DurationCap(100, 320)
        val sut = InAppMessageEventTriggerFrequencyCapDeterminer.DurationCapPredicate(cap)

        fun assert(eventTs: Long, impressionTs: Long, result: Boolean) {
            val event = UserEvents.track("test", timestamp = eventTs)
            val impression = InAppMessageImpression(mapOf("\$id" to "a"), impressionTs)

            expectThat(sut.matches(event, impression)) isEqualTo result
        }

        assert(200, 100, true)
        assert(200, 99, false)
    }

    private fun impression(user: HackleUser, timestamp: Long): InAppMessageImpression {
        return InAppMessageImpression(user.identifiers, timestamp)
    }
}