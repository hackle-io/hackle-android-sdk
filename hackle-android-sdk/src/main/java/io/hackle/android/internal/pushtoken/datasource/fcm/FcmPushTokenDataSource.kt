package io.hackle.android.internal.pushtoken.datasource.fcm

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import io.hackle.android.internal.pushtoken.datasource.PushTokenDataSource
import io.hackle.sdk.core.internal.log.Logger

internal class FcmPushTokenDataSource(context: Context) : PushTokenDataSource {

    private val firebaseApp: FirebaseApp?

    init {
        firebaseApp = createFirebaseInstance(context)
    }

    override fun getPushToken(): String? {
        try {
            log.debug { "Attempting FirebaseMessaging.getToken." }
            val token = getPushTokenFromFirebaseMessaging()
            log.debug { "Successfully receive push token." }
            return token
        } catch (throwable: Throwable) {
            log.debug { "Not succeeded FirebaseMessaging.getToken." }
        }

        try {
            log.debug { "Attempting FirebaseInstanceId.getToken." }
            val token = getPushTokenFromFirebaseInstanceId()
            log.debug { "Successfully receive push token." }
            return token
        } catch (throwable: Throwable) {
            log.debug { "Not succeeded FirebaseInstanceId.getToken: $throwable" }
        }

        return null
    }

    private fun createFirebaseInstance(context: Context): FirebaseApp? {
        try {
            val options = FirebaseOptions.fromResource(context) ?: return null
            return FirebaseApp.initializeApp(context, options, FCM_APP_NAME)
        } catch (e: Exception) {
            log.debug { "Cannot create Firebase App instance: $e" }
        }
        return null
    }

    // FirebaseMessaging.getToken API was introduced since firebase-messaging:21.0.0
    private fun getPushTokenFromFirebaseMessaging(): String? {
        val firebaseApp = firebaseApp ?: return null
        val instance = firebaseApp.get(FirebaseMessaging::class.java)
        val tokenTask = instance.token
        return Tasks.await(tokenTask)
    }

    // This method for less than firebase-messaging:21.0.0
    @Deprecated("")
    private fun getPushTokenFromFirebaseInstanceId(): String? {
        val firebaseApp = firebaseApp ?: return null
        val firebaseInstanceIdClass = Class.forName("com.google.firebase.iid.FirebaseInstanceId")
        val getInstanceMethod = firebaseInstanceIdClass.getMethod("getInstance", FirebaseApp::class.java)
        val instanceId = getInstanceMethod.invoke(null, firebaseApp)
        val getTokenMethod = instanceId.javaClass.getMethod("getToken")
        return getTokenMethod.invoke(instanceId) as? String
    }
    
    companion object {

        private const val FCM_APP_NAME = "HACKLE_SDK_APP"
        private val log = Logger<FcmPushTokenDataSource>()
    }
}