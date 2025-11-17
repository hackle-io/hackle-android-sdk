package io.hackle.android.sdk.tester

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.hackle.android.Hackle
import io.hackle.android.HackleApp
import io.hackle.android.HackleConfig
import io.hackle.android.app
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations
import io.hackle.sdk.common.subscription.HackleSubscriptionStatus
import java.util.concurrent.Executors
import kotlin.concurrent.thread

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
    private val apiUri = BuildConfig.YOUR_API_URI

    private val executor = Executors.newScheduledThreadPool(4)
    private lateinit var userService: UserService

    private var isShowUserExplorer = false

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        userService = UserService(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val config = HackleConfig.builder()
            .eventUri(eventUri)
            .sdkUri(sdkUri)
            .monitoringUri(monitoringUri)
            .apiUri(apiUri)
            .logLevel(Log.DEBUG)
            .build()

        HackleApp.initializeApp(this, sdkKey, config) {
            findViewById<TextView>(R.id.sdk_status).also { it.text = "INITIALIZED" }
            HackleApp.getInstance().showUserExplorer()
        }


        findViewById<TextView>(R.id.sdk_status).setOnClickListener {
            startActivity(Intent(this, SubActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.ab_text_btn).setOnClickListener {
            longOrNull(R.id.experiment_key)?.let { experimentKey ->
                thread {
                    result(Hackle.app.variationDetail(experimentKey).toString())
                }
            }

        }

        findViewById<Button>(R.id.feature_flag_btn).setOnClickListener {
            longOrNull(R.id.feature_key)?.let { featureKey ->
                val decision = Hackle.app.featureFlagDetail(featureKey)
                result(decision.toString())
            }
        }

        findViewById<Button>(R.id.rc_btn).setOnClickListener {
            textOrNull(R.id.rc_key)?.let { parameterKey ->
                val value = Hackle.app.remoteConfig().getString(parameterKey, "Input default")
                result(value)
            }
        }

        findViewById<EditText>(R.id.event_key).setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                findViewById<Button>(R.id.track_btn).performClick()
                true
            } else {
                false
            }
        }

        findViewById<Button>(R.id.track_btn).setOnClickListener {
            textOrNull(R.id.event_key)?.let { eventKey ->
                executor.submit {
                    Hackle.app.track(eventKey)
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

        findViewById<Switch>(R.id.push_switch).setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) HackleSubscriptionStatus.SUBSCRIBED else HackleSubscriptionStatus.UNSUBSCRIBED
            Hackle.app.updatePushSubscriptions(
                HackleSubscriptionOperations.builder()
                    .marketing(status)
                    .information(status)
                    .custom("chat", status)
                    .build()
            )
        }

        findViewById<Switch>(R.id.sms_switch).setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) HackleSubscriptionStatus.SUBSCRIBED else HackleSubscriptionStatus.UNSUBSCRIBED
            Hackle.app.updateSmsSubscriptions(
                HackleSubscriptionOperations.builder()
                    .marketing(status)
                    .information(status)
                    .custom("chat", status)
                    .build()
            )
        }

        findViewById<Switch>(R.id.kakao_switch).setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) HackleSubscriptionStatus.SUBSCRIBED else HackleSubscriptionStatus.UNSUBSCRIBED
            Hackle.app.updateKakaoSubscriptions(
                HackleSubscriptionOperations.builder()
                    .marketing(status)
                    .information(status)
                    .custom("chat", status)
                    .build()
            )
        }

        findViewById<Button>(R.id.secondPage_btn).setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content)) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(10, systemBarInsets.top, 10, imeInsets.bottom)
            insets
        }
    }

    private fun result(value: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.result).also {
                it.text = value
            }
        }

    }
}