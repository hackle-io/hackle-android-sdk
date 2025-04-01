package io.hackle.android.internal.pii.phonenumber

class PhoneNumber {
    companion object {
        fun filtered(phoneNumber: String): String {
            return phoneNumber
                .filter { it.isDigit() || it == '+'  }
                .take(16)
        }
    }
}