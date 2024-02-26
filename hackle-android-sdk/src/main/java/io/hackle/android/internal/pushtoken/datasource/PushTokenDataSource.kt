package io.hackle.android.internal.pushtoken.datasource

internal interface PushTokenDataSource {
    
    fun getPushToken(): String?
}