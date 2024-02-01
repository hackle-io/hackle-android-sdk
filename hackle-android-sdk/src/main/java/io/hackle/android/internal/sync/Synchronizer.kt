package io.hackle.android.internal.sync

internal interface Synchronizer {
    fun sync(callback: Runnable? = null)
}
