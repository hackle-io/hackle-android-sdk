package io.hackle.android.mock

import io.hackle.android.internal.model.PackageInfo
import io.hackle.android.internal.platform.model.PackageVersionInfo

internal class MockPackageInfo(
    override val currentPackageVersionInfo: PackageVersionInfo,
    override val previousPackageVersionInfo: PackageVersionInfo? = null,
    override val properties: Map<String, Any> = emptyMap()
) : PackageInfo {


}