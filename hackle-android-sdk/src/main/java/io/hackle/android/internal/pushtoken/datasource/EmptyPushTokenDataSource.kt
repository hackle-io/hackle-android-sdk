package io.hackle.android.internal.pushtoken.datasource

internal class EmptyPushTokenDataSource : PushTokenDataSource {

    override fun getPushToken(): String? = null
}