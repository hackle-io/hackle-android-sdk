package io.hackle.android.internal.pii.phonenumber

internal data class PhoneNumber(val value: String) {
    companion object {
        fun create(phoneNumber: String): PhoneNumber {
            val filteredValue = phoneNumber
                .filter { it.isDigit() || it == '+' }
                .take(16)
            return PhoneNumber(filteredValue)
        }
    }
}