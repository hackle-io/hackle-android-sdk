package io.hackle.android.sdk.tester

import android.app.Activity
import android.widget.CheckBox
import android.widget.EditText

fun Activity.isChecked(id: Int): Boolean {
    return findViewById<CheckBox>(id).isChecked
}

fun Activity.textOrNull(id: Int): String? {
    val text = findViewById<EditText>(id)?.text?.toString()
    if (text.isNullOrBlank()) {
        return null
    }
    return text
}

fun Activity.longOrNull(id: Int): Long? {
    return textOrNull(id)?.toLongOrNull()
}