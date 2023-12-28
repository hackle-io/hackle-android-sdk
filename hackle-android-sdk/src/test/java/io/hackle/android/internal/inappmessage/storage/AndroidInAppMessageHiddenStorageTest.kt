package io.hackle.android.internal.inappmessage.storage

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.support.InAppMessages
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class AndroidInAppMessageHiddenStorageTest {

    private lateinit var keyValueRepository: KeyValueRepository
    private lateinit var sut: AndroidInAppMessageHiddenStorage


    @Before
    fun before() {
        keyValueRepository = MapKeyValueRepository()
        sut = AndroidInAppMessageHiddenStorage(keyValueRepository)
    }

    @Test
    fun `데이터가 없는 경우 false`() {
        val inAppMessage = InAppMessages.create()
        expectThat(sut.exist(inAppMessage, 0))
    }

    @Test
    fun `데이터가 있지만 만료시간이 넘은 경우 false`() {
        val inAppMessage = InAppMessages.create()
        sut.put(inAppMessage, 42)
        expectThat(keyValueRepository.getLong("1", -1)) isEqualTo 42

        val actual = sut.exist(inAppMessage, 43)
        expectThat(actual).isFalse()
        expectThat(keyValueRepository.getLong("1", -1)) isEqualTo -1
    }

    @Test
    fun `데이터가 있고 만료시간 이내인 경우 true`() {
        val inAppMessage = InAppMessages.create()
        sut.put(inAppMessage, 42)
        expectThat(keyValueRepository.getLong("1", -1)) isEqualTo 42


        val actual = sut.exist(inAppMessage, 42)
        expectThat(actual).isTrue()
        expectThat(keyValueRepository.getLong("1", -1)) isEqualTo 42
    }
}
