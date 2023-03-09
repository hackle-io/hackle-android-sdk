package io.hackle.android.explorer

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import io.hackle.android.Hackle
import io.hackle.android.R
import io.hackle.android.app

class HackleUserExplorerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hackle_user_explorer_activity)

        findViewById<ImageView>(R.id.hackle_ue_iv_close).setOnClickListener {
            onBackPressed()
        }

        val defaultIdTextView = findViewById<TextView>(R.id.hackle_ue_tv_default_id)
        val deviceIdTextView = findViewById<TextView>(R.id.hackle_ue_tv_device_id)
        val userIdTextView = findViewById<TextView>(R.id.hackle_ue_tv_user_id)

        val user = Hackle.app.user

        defaultIdTextView.text = user.id ?: getString(R.string.hackle_label_not_avail)
        deviceIdTextView.text = user.deviceId ?: getString(R.string.hackle_label_not_avail)
        userIdTextView.text = user.userId ?: getString(R.string.hackle_label_not_avail)

        findViewById<Button>(R.id.hackle_ue_btn_copy_default_id).setOnClickListener {
            copyText(it.context, user.id)
        }

        findViewById<Button>(R.id.hackle_ue_btn_copy_device_id).setOnClickListener {
            copyText(it.context, user.deviceId)
        }

        findViewById<Button>(R.id.hackle_ue_btn_copy_user_id).setOnClickListener {
            copyText(it.context, user.userId)
        }
    }

    private fun copyText(context: Context, text: String?) {
        if (text == null) {
            return
        }
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("copied text", text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, getString(R.string.hackle_label_copied), Toast.LENGTH_SHORT).show()
    }
}
