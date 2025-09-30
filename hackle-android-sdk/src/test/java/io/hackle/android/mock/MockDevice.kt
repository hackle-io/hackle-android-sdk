package io.hackle.android.mock

import io.hackle.android.internal.model.Device

internal class MockDevice(
    override val id: String,
    override val properties: Map<String, Any>,
    override val isIdCreated: Boolean = false
) : Device