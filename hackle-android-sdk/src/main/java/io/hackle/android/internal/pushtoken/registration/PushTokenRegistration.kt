package io.hackle.android.internal.pushtoken.registration

internal interface PushTokenRegistration {
    
    fun getPushToken(): String?
}