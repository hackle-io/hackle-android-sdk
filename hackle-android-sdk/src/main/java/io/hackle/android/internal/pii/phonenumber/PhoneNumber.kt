package io.hackle.android.internal.pii.phonenumber

internal class PhoneNumber {
    companion object {
        fun filtered(phoneNumber: String): String {
            return phoneNumber
                .filter { it.isDigit() || it == '+'  }
                .take(16)
        }
    }
}