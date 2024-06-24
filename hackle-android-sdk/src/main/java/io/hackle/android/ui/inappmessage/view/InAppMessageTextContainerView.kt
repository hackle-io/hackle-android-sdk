package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
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

    fun bind(message: InAppMessage.Message) {
        titleTextView = findViewById(R.id.hackle_iam_modal_title_text_view)
        bodyTextView = findViewById(R.id.hackle_iam_modal_body_text_view)

        if (message.text == null) {
            bodyTextView.visibility = TextView.GONE
            titleTextView.visibility = TextView.GONE
            visibility = View.GONE
            return
        }
        titleTextView.text = message.text!!.title.text
        bodyTextView.text = message.text!!.body.text


        titleTextView.setTextColor(Color.parseColor(message.text!!.title.style.textColor))
        bodyTextView.setTextColor(Color.parseColor(message.text!!.body.style.textColor))
        visibility = View.VISIBLE
    }
}