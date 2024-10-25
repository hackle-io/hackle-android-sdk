package io.hackle.android.sdk.tester

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.hackle.android.Hackle
import io.hackle.android.HackleApp
import io.hackle.android.HackleConfig
import io.hackle.android.app
import io.hackle.sdk.common.HackleInAppMessage
import io.hackle.sdk.common.HackleInAppMessageAction
import io.hackle.sdk.common.HackleInAppMessageListener
import io.hackle.sdk.common.HackleInAppMessageView
import io.hackle.sdk.common.HacklePushSubscriptionStatus
import java.util.concurrent.Executors
import kotlin.concurrent.thread
//import io.hackle.android.explorer.HackleUserExplorerHandler

class MainActivity : AppCompatActivity() {

    /**
     * Create a local.properties file and add sdkKey, eventUri, sdkUri and monitoringUri with your values
     * example in local.properties file:
     *      #Hackle SDK key
     *      sdkKey="abcd1234"
     *
     *      #Uri for dev dashboard
     *      eventUri="https://eventUri.com"
     *      sdkUri="https://sdkUri.com"
     *      monitoringUri="https://monitoringUri.com"
     */
    private val sdkKey = BuildConfig.YOUR_SDK_KEY
    private val eventUri = BuildConfig.YOUR_EVENT_URI
    private val sdkUri = BuildConfig.YOUR_SDK_URI
    private val monitoringUri = BuildConfig.YOUR_MONITORING_URI

    private val executor = Executors.newScheduledThreadPool(4)
    private lateinit var userService: UserService
    override fun onResume() {
        super.onResume()
        Log.i("HackleSdk", "##### onResume")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        userService = UserService(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        (applicationContext as? Application)?.registerActivityLifecycleCallbacks(TestCallback())

        val config = HackleConfig.builder()
//            .eventUri(eventUri)
//            .sdkUri(sdkUri)
//            .monitoringUri(monitoringUri)
//            .logLevel(Log.DEBUG)
//            .pollingIntervalMillis(10000)
//            .sessionTimeoutMillis(10000)
            .build()

        HackleApp.initializeApp(this, sdkKey, config) {
            findViewById<TextView>(R.id.sdk_status).also { it.text = "INITIALIZED" }
            Hackle.app.showUserExplorer()
        }

        Hackle.app.updatePushSubscriptionStatus(HacklePushSubscriptionStatus.SUBSCRIBED)

        Log.i("HackleSdk", "##### app_launch")

        findViewById<TextView>(R.id.sdk_status).setOnClickListener {
//            startActivity(Intent(this, WebViewActivity::class.java))
            startActivity(Intent(this, SubActivity::class.java))
        }

        Hackle.app.showUserExplorer(this)
        findViewById<Button>(R.id.ab_text_btn).setOnClickListener {
            longOrNull(R.id.experiment_key)?.let { experimentKey ->
                if (isChecked(R.id.experiment_with_user)) {
                    executor.submit {
                        result(
                            Hackle.app.variationDetail(experimentKey, userService.user()).toString()
                        )
                    }

                } else {
                    thread {
                        result(Hackle.app.variationDetail(experimentKey).toString())
                    }

                }
            }

        }

        findViewById<Button>(R.id.feature_flag_btn).setOnClickListener {
            longOrNull(R.id.feature_key)?.let { featureKey ->
                val decision = if (isChecked(R.id.feature_flag_with_user)) {
                    Hackle.app.featureFlagDetail(featureKey, userService.user())
                } else {
                    Hackle.app.featureFlagDetail(featureKey)
                }
                result(decision.toString())
            }
        }

        findViewById<Button>(R.id.rc_btn).setOnClickListener {
            textOrNull(R.id.rc_key)?.let { parameterKey ->
                val value = if (isChecked(R.id.rc_with_user)) {
                    Hackle.app.remoteConfig(userService.user())
                        .getString(parameterKey, "Input Default")
                } else {
                    Hackle.app.remoteConfig().getString(parameterKey, "Input default")
                }
                result(value)
            }
        }

        findViewById<Button>(R.id.track_btn).setOnClickListener {
            textOrNull(R.id.event_key)?.let { eventKey ->
                if (isChecked(R.id.track_with_user)) {
                    executor.submit {
                        Hackle.app.track(eventKey, userService.user())
                    }
                } else {
                    executor.submit {
                        Hackle.app.track(eventKey)
                    }
                }
                result(eventKey)
            }
        }

        findViewById<Button>(R.id.set_user_btn).setOnClickListener {
            Hackle.app.setUser(userService.user())
        }

        findViewById<Button>(R.id.set_user_id_btn).setOnClickListener {
            Hackle.app.setUserId(userService.userIdOrNull())
        }

        findViewById<Button>(R.id.set_device_id_btn).setOnClickListener {
            userService.deviceIdOrNull()?.let {
                Hackle.app.setDeviceId(it)
            }

        }

        findViewById<Button>(R.id.set_property_01_btn).setOnClickListener {
            val property = userService.property(R.id.property_key_01, R.id.property_value_01)
            if (property != null) {
                Hackle.app.setUserProperty(property.first, property.second)

            }
        }


//        ArrayAdapter(this, )
//        findViewById<Spinner>(R.id.hackle_spinner).apply {
//
//        }

        findViewById<Button>(R.id.pushSubscription_btn).setOnClickListener {
            Hackle.app.updatePushSubscriptionStatus(HacklePushSubscriptionStatus.SUBSCRIBED)
        }

        findViewById<Button>(R.id.pushUnsubscription_btn).setOnClickListener {
            Hackle.app.updatePushSubscriptionStatus(HacklePushSubscriptionStatus.UNSUBSCRIBED)
        }

        Hackle.app.setInAppMessageListener(object : HackleInAppMessageListener {
            override fun afterInAppMessageClose(inAppMessage: HackleInAppMessage) {
                TODO("Not yet implemented")
            }

            override fun afterInAppMessageOpen(inAppMessage: HackleInAppMessage) {
                TODO("Not yet implemented")
            }

            override fun beforeInAppMessageClose(inAppMessage: HackleInAppMessage) {
                TODO("Not yet implemented")
            }

            override fun beforeInAppMessageOpen(inAppMessage: HackleInAppMessage) {
                TODO("Not yet implemented")
            }

            override fun onInAppMessageClick(
                inAppMessage: HackleInAppMessage,
                view: HackleInAppMessageView,
                action: HackleInAppMessageAction
            ): Boolean {
                TODO("Not yet implemented")
            }
        })
    }

    private fun result(value: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.result).also {
                it.text = value
            }
        }

    }

    val close = if (1 > 2) {
        "1"
    } else {
        null
    }
}