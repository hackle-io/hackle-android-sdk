package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import io.hackle.android.R
import io.hackle.sdk.core.model.InAppMessage

internal class InAppMessageTextContainerView : LinearLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private lateinit var titleTextView: TextView

    private lateinit var bodyTextView: InAppMessageBodyTextView


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    fun bind(message: InAppMessage.MessageContext.Message) {
        titleTextView = findViewById(R.id.hackle_in_app_title_text)
        bodyTextView = findViewById(R.id.hackle_in_app_body_text)

        if (message.text == null) {
            bodyTextView.visibility = TextView.GONE
            titleTextView.visibility = TextView.GONE
            return
        }
        titleTextView.text = message.text!!.title.text
        bodyTextView.text = message.text!!.body.text


        titleTextView.setTextColor(Color.parseColor(message.text!!.title.style.textColor))
        bodyTextView.setTextColor(Color.parseColor(message.text!!.body.style.textColor))
    }


}