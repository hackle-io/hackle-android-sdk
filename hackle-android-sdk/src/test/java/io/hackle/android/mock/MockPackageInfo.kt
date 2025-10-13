package io.hackle.android.mock

import io.hackle.android.internal.platform.packageinfo.PackageInfo
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo

internal class MockPackageInfo(
    override val packageVersionInfo: PackageVersionInfo,
    override val properties: Map<String, Any> = emptyMap()
) : PackageInfo