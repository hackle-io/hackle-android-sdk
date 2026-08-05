package io.hackle.android.internal.user

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.sdk.common.User
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class DefaultUserRepositoryTest {

    private lateinit var keyValueRepository: KeyValueRepository
    private lateinit var sut: DefaultUserRepository

    @Before
    fun before() {
        keyValueRepository = MapKeyValueRepository()
        sut = DefaultUserRepository(keyValueRepository)
    }

    @Test
    fun `get - 저장된 유저가 없으면 null을 리턴한다`() {
        expectThat(sut.get()).isNull()
    }

    @Test
    fun `set, get - 저장한 유저를 다시 로드한다`() {
        // given
        val user = User.builder()
            .id("id")
            .userId("user_id")
            .deviceId("device_id")
            .identifier("custom", "custom_id")
            .property("grade", "gold")
            .build()

        // when
        sut.set(user)

        // then
        expectThat(sut.get()) isEqualTo user
    }

    @Test
    fun `set - 기존 저장본을 덮어쓴다`() {
        // given
        sut.set(User.builder().userId("user_id_1").build())

        // when
        sut.set(User.builder().userId("user_id_2").build())

        // then
        expectThat(sut.get()?.userId) isEqualTo "user_id_2"
    }

    @Test
    fun `get - 파싱에 실패하면 null을 리턴한다`() {
        // given
        keyValueRepository.putString("user", "invalid-json")

        // when
        val actual = sut.get()

        // then
        expectThat(actual).isNull()
    }
}
