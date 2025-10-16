package io.hackle.android.internal.user

import io.hackle.sdk.core.user.HackleUser

internal interface UserDecorator {
    fun decorate(user: HackleUser): HackleUser
}

internal fun HackleUser.decorateWith(decorator: UserDecorator): HackleUser {
    return decorator.decorate(this)
}
