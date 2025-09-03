package io.hackle.android.sdk.tester

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import io.hackle.android.Hackle
import io.hackle.android.app

class SubActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)
        findViewById<TextView>(R.id.sdk_status).setOnClickListener {
            startActivity(Intent(applicationContext, SubActivity::class.java))
        }
    }
}