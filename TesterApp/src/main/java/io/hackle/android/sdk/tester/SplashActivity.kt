package io.hackle.android.sdk.tester

import android.app.Application
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import io.hackle.android.Hackle
import io.hackle.android.app

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

//        init()
    }

    private fun init() {

        val hackleApp = Hackle.app
        val variation = hackleApp.isFeatureOn(42)
    }
}