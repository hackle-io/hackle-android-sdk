package io.hackle.android.explorer.activity.user

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity.CLIPBOARD_SERVICE
import io.hackle.android.R
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.sdk.core.internal.metrics.Metrics

internal class IdentifierView : FrameLayout {

    private val identifierType: TextView
    private val identifierValue: TextView
    private val copyButton: Button

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.hackle_view_identifier, this, true)
        identifierType = view.findViewById(R.id.hackle_identifier_type)
        identifierValue = view.findViewById(R.id.hackle_identifier_value)
        copyButton = view.findViewById(R.id.hackle_identifier_value_copy_button)
    }

    fun bind(item: IdentifierItem) {
        identifierType.text = item.type

        if (item.value == null) {
            copyButton.isEnabled = false
            return
        }

        identifierValue.text = item.value
        copyButton.setOnClickListener {
            copy(item.value)
            Metrics.counter("user.explorer.identifier.copy").increment()
        }
    }

    private fun copy(text: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("copied text", text)
        clipboard.setPrimaryClip(clip)
        runOnUiThread {
            Toast.makeText(
                context, context.getString(R.string.hackle_label_copied), Toast.LENGTH_SHORT).show()
        }
    }
}
