package io.hackle.android

import android.content.Context
import android.content.Intent
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations

object Hackle

val Hackle.app: HackleApp get() = HackleApp.getInstance()

fun Hackle.registerActivityLifecycleCallbacks(context: Context) {
    HackleApp.registerActivityLifecycleCallbacks(context)
}

fun Hackle.isHacklePushMessage(intent: Intent) = HackleApp.isHacklePushMessage(intent)

fun Hackle.initialize(
    context: Context,
    sdkKey: String,
    config: HackleConfig = HackleConfig.DEFAULT,
    onReady: () -> Unit = {},
): HackleApp {
    return HackleApp.initializeApp(context, sdkKey, config, onReady)
}


fun Hackle.initialize(
    context: Context,
    sdkKey: String,
    user: User? = null,
    config: HackleConfig = HackleConfig.DEFAULT,
    onReady: () -> Unit = {},
): HackleApp {
    return HackleApp.initializeApp(context, sdkKey, user, config, onReady)
}

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

fun Hackle.setPhoneNumber(phoneNumber: String) = app.setPhoneNumber(phoneNumber)
fun Hackle.unsetPhoneNumber() = app.unsetPhoneNumber()

fun Hackle.updatePushSubscriptions(operations: HackleSubscriptionOperations) = app.updatePushSubscriptions(operations)
fun Hackle.updateSmsSubscriptions(operations: HackleSubscriptionOperations) = app.updateSmsSubscriptions(operations)
fun Hackle.updateKakaoSubscriptions(operations: HackleSubscriptionOperations) = app.updateKakaoSubscriptions(operations)

fun Hackle.fetch(callback: Runnable? = null) = app.fetch(callback)

@Deprecated(
    "Use remoteConfig() with setUser(user) instead",
    ReplaceWith("app.remoteConfig()")
)
fun Hackle.remoteConfig(user: User) = app.remoteConfig(user)

@Deprecated("Do not use the method because Hackle SDK will register push token by self. (Will remove v2.38.0)")
fun Hackle.setPushToken(token: String) = app.setPushToken(token)
