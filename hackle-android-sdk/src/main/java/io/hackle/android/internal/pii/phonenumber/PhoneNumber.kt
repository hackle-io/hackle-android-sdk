package io.hackle.android.internal.pii.phonenumber

internal data class PhoneNumber(val value: String) {
    companion object {
        fun create(phoneNumber: String): PhoneNumber {
            return PhoneNumber(phoneNumber)
        }
    }
}