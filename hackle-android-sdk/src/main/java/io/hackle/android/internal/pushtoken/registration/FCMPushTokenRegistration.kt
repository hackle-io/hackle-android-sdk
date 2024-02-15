package io.hackle.android.internal.pushtoken.registration

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import io.hackle.sdk.core.internal.log.Logger

internal class FCMPushTokenRegistration(context: Context) : PushTokenRegistration {

    private val firebaseApp: FirebaseApp?
    
    init {
        firebaseApp = createFirebaseInstance(context)
    }

    override fun getPushToken(): String? {
        try {
            val firebaseApp = firebaseApp ?: return null
            val instance = firebaseApp.get(FirebaseMessaging::class.java)
            val tokenTask = instance.token
            return Tasks.await(tokenTask)
        } catch (e: Exception) {
            log.debug { "Could not retrieve FCM token: $e" }
        }
        return null
    }

    private fun createFirebaseInstance(context: Context): FirebaseApp? {
        val options = FirebaseOptions.fromResource(context) ?: return null
        return FirebaseApp.initializeApp(context, options, FCM_APP_NAME)
    }
    
    companion object {

        private const val FCM_APP_NAME = "HACKLE_SDK_APP"
        private val log = Logger<FCMPushTokenRegistration>()
    }
}