package io.hackle.android.internal.workspace.evaluation

internal interface WorkspaceEvaluationCache {
    fun get(key: WorkspaceEvaluationContext.Key): WorkspaceEvaluationContext?
    fun put(context: WorkspaceEvaluationContext): List<WorkspaceEvaluationContext>
    fun latest(): WorkspaceEvaluationContext?
    fun restore(contexts: List<WorkspaceEvaluationContext>)
}

internal class LruWorkspaceEvaluationCache(private val capacity: Int) : WorkspaceEvaluationCache {

    private val lock = Any()
    private val entries = HashMap<WorkspaceEvaluationContext.Key, WorkspaceEvaluationContext>()
    private val order = ArrayList<WorkspaceEvaluationContext.Key>()

    override fun get(key: WorkspaceEvaluationContext.Key): WorkspaceEvaluationContext? {
        return synchronized(lock) { entries[key] }
    }

    override fun put(context: WorkspaceEvaluationContext): List<WorkspaceEvaluationContext> {
        synchronized(lock) {
            remove(context.key)
            add(context)
            evict()
            return order.map { entries.getValue(it) }
        }
    }

    private fun remove(key: WorkspaceEvaluationContext.Key) {
        entries.remove(key)
        order.remove(key)
    }

    private fun add(context: WorkspaceEvaluationContext) {
        entries[context.key] = context
        order.add(context.key)
    }

    private fun evict() {
        if (order.size > capacity) {
            remove(order.first())
        }
    }

    override fun latest(): WorkspaceEvaluationContext? {
        synchronized(lock) {
            val key = order.lastOrNull() ?: return null
            return entries[key]
        }
    }

    override fun restore(contexts: List<WorkspaceEvaluationContext>) {
        synchronized(lock) {
            entries.clear()
            order.clear()
            for (context in contexts.takeLast(capacity)) {
                add(context)
            }
        }
    }
}
