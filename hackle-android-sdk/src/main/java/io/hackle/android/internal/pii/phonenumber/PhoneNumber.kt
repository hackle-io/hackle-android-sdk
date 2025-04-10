package io.hackle.android.internal.pii.phonenumber

internal data class PhoneNumber(val value: String) {
    companion object {
        fun create(value: String): PhoneNumber {
            val filteredValue = value
                .filter { it.isDigit() || it == '+' }
                .take(16)
            return PhoneNumber(filteredValue)
        }
    }
}