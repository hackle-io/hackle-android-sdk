package io.hackle.android.provider

import io.mockk.*
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class LifecycleProviderTest {

    @Test
    fun `query should return null`() {
        // given
        val provider = LifecycleProvider()
        val mockUri = mockk<android.net.Uri>()

        // when
        val result = provider.query(mockUri, null, null, null, null)

        // then
        expectThat(result == null).isTrue()
    }

    @Test
    fun `getType should return null`() {
        // given
        val provider = LifecycleProvider()
        val mockUri = mockk<android.net.Uri>()

        // when
        val result = provider.getType(mockUri)

        // then
        expectThat(result == null).isTrue()
    }

    @Test
    fun `insert should return null`() {
        // given
        val provider = LifecycleProvider()
        val mockUri = mockk<android.net.Uri>()

        // when
        val result = provider.insert(mockUri, null)

        // then
        expectThat(result == null).isTrue()
    }

    @Test
    fun `delete should return 0`() {
        // given
        val provider = LifecycleProvider()
        val mockUri = mockk<android.net.Uri>()

        // when
        val result = provider.delete(mockUri, null, null)

        // then
        expectThat(result).isEqualTo(0)
    }

    @Test
    fun `update should return 0`() {
        // given
        val provider = LifecycleProvider()
        val mockUri = mockk<android.net.Uri>()

        // when
        val result = provider.update(mockUri, null, null, null)

        // then
        expectThat(result).isEqualTo(0)
    }
}
