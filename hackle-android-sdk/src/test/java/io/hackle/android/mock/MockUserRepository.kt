package io.hackle.android.mock

import io.hackle.android.internal.user.UserRepository
import io.hackle.sdk.common.User

internal class MockUserRepository(
    var user: User? = null
) : UserRepository {
    override fun get(): User? = user

    override fun set(user: User) {
        this.user = user
    }
}
