package io.hackle.android

import android.content.Context
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User

object Hackle

val Hackle.app: HackleApp get() = HackleApp.getInstance()

fun Hackle.initialize(
    context: Context,
    sdkKey: String,
    config: HackleConfig = HackleConfig.DEFAULT,
): HackleApp =
    HackleApp.initializeApp(context, sdkKey, config)

fun Hackle.initialize(
    context: Context,
    sdkKey: String,
    config: HackleConfig = HackleConfig.DEFAULT,
    onReady: () -> Unit,
): HackleApp =
    HackleApp.initializeApp(context, sdkKey, config) { onReady() }

fun Hackle.user(id: String) = User.of(id)
fun Hackle.user(id: String, init: User.Builder.() -> Unit) = User.builder(id).apply(init).build()

fun Hackle.event(key: String) = Event.of(key)
fun Hackle.event(key: String, init: Event.Builder.() -> Unit) = Event.builder(key).apply(init).build()

fun Hackle.remoteConfig(user: User = Hackle.user(app.deviceId)) = app.remoteConfig(user)
