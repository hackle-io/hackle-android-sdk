package io.hackle.android

import android.content.Context

object Hackle

val Hackle.app: HackleApp get() = HackleApp.getInstance()

fun Hackle.initialize(context: Context, sdkKey: String): HackleApp =
    HackleApp.initializeApp(context, sdkKey)

fun Hackle.initialize(context: Context, sdkKey: String, onReady: () -> Unit): HackleApp =
    HackleApp.initializeApp(context, sdkKey) { onReady() }