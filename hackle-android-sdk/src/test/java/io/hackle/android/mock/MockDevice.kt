package io.hackle.android.mock

import io.hackle.android.internal.platform.device.Device

internal class MockDevice(
    override val id: String,
    override val properties: Map<String, Any>
) : Device