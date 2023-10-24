package io.hackle.android.internal.bridge.model

import com.google.gson.annotations.SerializedName

internal data class BridgeResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: Any? = null
)