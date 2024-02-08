package io.hackle.android.mock

import io.hackle.android.internal.workspace.WorkspaceConfig
import io.hackle.android.internal.workspace.repository.WorkspaceConfigRepository

internal class MockWorkspaceConfigRepository(
    var value: WorkspaceConfig? = null
) : WorkspaceConfigRepository {
    override fun get(): WorkspaceConfig? = value

    override fun set(value: WorkspaceConfig) {
        this.value = value
    }
}