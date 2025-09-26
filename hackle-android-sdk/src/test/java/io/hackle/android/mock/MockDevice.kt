package io.hackle.android.mock

import io.hackle.android.internal.model.Device
import io.hackle.android.internal.platform.model.PackageInfo

internal class MockDevice(
    override val id: String,
    override val properties: Map<String, Any>,
    override val isIdCreated: Boolean = false,
    override val packageInfo: PackageInfo = PackageInfo("test.app", "1.0.0", 1L, null, null)
) : Device