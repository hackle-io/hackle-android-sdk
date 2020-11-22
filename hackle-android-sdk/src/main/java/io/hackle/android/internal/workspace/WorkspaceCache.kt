package io.hackle.android.internal.workspace

import io.hackle.sdk.core.workspace.Workspace

internal class WorkspaceCache {

    private var workspace: Workspace? = null

    fun get(): Workspace? {
        return workspace
    }

    fun put(workspace: Workspace) {
        this.workspace = workspace
    }
}