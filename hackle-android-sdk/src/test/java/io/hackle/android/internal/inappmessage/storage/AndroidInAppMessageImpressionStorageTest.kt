package io.hackle.android.internal.inappmessage.storage

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.core.evaluation.target.InAppMessageImpression
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

internal class AndroidInAppMessageImpressionStorageTest {

    private lateinit var repository: KeyValueRepository
    private lateinit var sut: AndroidInAppMessageImpressionStorage

    @Before
    fun before() {
        repository = MapKeyValueRepository()
        sut = AndroidInAppMessageImpressionStorage(repository)
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