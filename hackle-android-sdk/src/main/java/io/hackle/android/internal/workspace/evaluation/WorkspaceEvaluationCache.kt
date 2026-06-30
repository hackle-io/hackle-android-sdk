package io.hackle.android.internal.workspace.evaluation

internal interface WorkspaceEvaluationCache {
    fun get(key: WorkspaceEvaluationContext.Key): WorkspaceEvaluationRecord?
    fun put(record: WorkspaceEvaluationRecord): List<WorkspaceEvaluationRecord>
    fun latest(): WorkspaceEvaluationRecord?
    fun restore(records: List<WorkspaceEvaluationRecord>)
}

internal class LruWorkspaceEvaluationCache(private val capacity: Int) : WorkspaceEvaluationCache {

    private val lock = Any()
    private val entries = HashMap<WorkspaceEvaluationContext.Key, WorkspaceEvaluationRecord>()
    private val order = ArrayList<WorkspaceEvaluationContext.Key>()

    override fun get(key: WorkspaceEvaluationContext.Key): WorkspaceEvaluationRecord? {
        return synchronized(lock) { entries[key] }
    }

    override fun put(record: WorkspaceEvaluationRecord): List<WorkspaceEvaluationRecord> {
        synchronized(lock) {
            remove(record.key)
            add(record)
            evict()
            return order.map { entries.getValue(it) }
        }
    }

    private fun remove(key: WorkspaceEvaluationContext.Key) {
        entries.remove(key)
        order.remove(key)
    }

    private fun add(record: WorkspaceEvaluationRecord) {
        entries[record.key] = record
        order.add(record.key)
    }

    private fun evict() {
        if (order.size > capacity) {
            remove(order.first())
        }
    }

    override fun latest(): WorkspaceEvaluationRecord? {
        synchronized(lock) {
            val key = order.lastOrNull() ?: return null
            return entries[key]
        }
    }

    override fun restore(records: List<WorkspaceEvaluationRecord>) {
        synchronized(lock) {
            entries.clear()
            order.clear()
            for (record in records.takeLast(capacity)) {
                add(record)
            }
        }
    }
}
