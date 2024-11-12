package io.hackle.android.sdk.tester

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.hackle.android.Hackle
import io.hackle.android.app

class SecondPageActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        findViewById<Button>(R.id.showBannerIAM_btn).setOnClickListener {
            Hackle.app.track("show_iam")
        }

        findViewById<Button>(R.id.showBottomSheetIAM_btn).setOnClickListener {
            Hackle.app.track("show_iam_bottom")
        }
    }
}