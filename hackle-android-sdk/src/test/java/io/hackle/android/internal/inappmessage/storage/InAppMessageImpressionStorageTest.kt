package io.hackle.android.internal.inappmessage.storage

import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.database.MapKeyValueRepository
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.core.model.InAppMessage
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

internal class InAppMessageImpressionStorageTest {

    private lateinit var repository: KeyValueRepository
    private lateinit var sut: InAppMessageImpressionStorage

    @Before
    fun before() {
        repository = MapKeyValueRepository()
        sut = InAppMessageImpressionStorage(repository)
    }

    @Test
    fun `get and set`() {
        val inAppMessage = InAppMessages.create(id = 42)
        val impression = InAppMessageImpression(identifiers = mapOf("a" to "b"), timestamp = 4242)

        expectThat(sut.get(inAppMessage)).isEmpty()

        sut.set(inAppMessage, listOf(impression))
        expectThat( repository.getString("42")).isNotNull()

        expectThat(sut.get(inAppMessage)).hasSize(1)
        expectThat(sut.get(inAppMessage).first()) isEqualTo impression
    }
}