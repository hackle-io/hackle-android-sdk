package io.hackle.android

import android.content.Context
import android.content.Intent
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User

object Hackle

val Hackle.app: HackleApp get() = HackleApp.getInstance()

fun Hackle.registerActivityLifecycleCallbacks(context: Context) {
    HackleApp.registerActivityLifecycleCallbacks(context)
}

fun Hackle.isHacklePushMessage(intent: Intent) = HackleApp.isHacklePushMessage(intent)

fun Hackle.user(id: String) = User.of(id)
fun Hackle.user(id: String? = null, init: User.Builder.() -> Unit) =
    User.builder().id(id).apply(init).build()

fun Hackle.event(key: String) = Event.of(key)
fun Hackle.event(key: String, init: Event.Builder.() -> Unit) =
    Event.builder(key).apply(init).build()


fun Hackle.remoteConfig() = app.remoteConfig()

fun Hackle.setUser(user: User) = app.setUser(user)
fun Hackle.setUserId(userId: String?) = app.setUserId(userId)
fun Hackle.setDeviceId(deviceId: String) = app.setDeviceId(deviceId)
fun Hackle.setUserProperty(key: String, value: Any?) = app.setUserProperty(key, value)
fun Hackle.resetUser() = app.resetUser()

fun Hackle.fetch(callback: Runnable? = null) = app.fetch(callback)

@Deprecated(
    "Use remoteConfig() with setUser(user) instead",
    ReplaceWith("app.remoteConfig()")
)
fun Hackle.remoteConfig(user: User) = app.remoteConfig(user)

@Deprecated("Do not use the method because Hackle SDK will register push token by self. (Will remove v2.38.0)")
fun Hackle.setPushToken(token: String) = app.setPushToken(token)
