package com.example.data

import com.example.network.RemoteTodo
import com.example.network.TodoPlaceholderApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID

enum class LogType {
    INFO, SUCCESS, WARNING, ERROR
}

data class SyncLog(
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType,
    val message: String
)

class TaskRepository(
    private val taskDao: TaskDao,
    private val api: TodoPlaceholderApi
) {
    val allTasksFlow: Flow<List<Task>> = taskDao.getAllTasksFlow()

    private val _syncLogs = MutableSharedFlow<SyncLog>(extraBufferCapacity = 64)
    val syncLogs: SharedFlow<SyncLog> = _syncLogs

    suspend fun emitLog(type: LogType, message: String) {
        _syncLogs.emit(SyncLog(type = type, message = message))
    }

    suspend fun getAllTasksDirect(): List<Task> = taskDao.getAllTasksDirect()

    suspend fun createTask(title: String, description: String, isOnline: Boolean): Task {
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            syncStatus = if (isOnline) SyncStatus.PENDING_CREATE else SyncStatus.PENDING_CREATE
        )
        taskDao.insertTask(task)
        emitLog(LogType.INFO, "Local Task created: '${title}' (ID: ${task.id.take(6)})")
        return task
    }

    suspend fun updateTaskStatus(id: String, isCompleted: Boolean, isOnline: Boolean) {
        val existingTask = taskDao.getTaskById(id) ?: return
        val updatedTask = existingTask.copy(
            isCompleted = isCompleted,
            updatedAt = System.currentTimeMillis(),
            syncStatus = if (existingTask.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_UPDATE else existingTask.syncStatus
        )
        taskDao.insertTask(updatedTask)
        emitLog(LogType.INFO, "Updated task '${existingTask.title}' status locally to completed=$isCompleted")
    }

    suspend fun updateTaskDetails(id: String, title: String, description: String) {
        val existingTask = taskDao.getTaskById(id) ?: return
        val updatedTask = existingTask.copy(
            title = title,
            description = description,
            updatedAt = System.currentTimeMillis(),
            syncStatus = if (existingTask.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_UPDATE else existingTask.syncStatus
        )
        taskDao.insertTask(updatedTask)
        emitLog(LogType.INFO, "Updated task '${existingTask.title}' details locally")
    }

    suspend fun deleteTaskLocally(id: String) {
        val existingTask = taskDao.getTaskById(id) ?: return
        if (existingTask.syncStatus == SyncStatus.PENDING_CREATE) {
            // If it was never synced to server, we can just hard delete it locally
            taskDao.deleteTaskById(id)
            emitLog(LogType.INFO, "Hard deleted unsynced task '${existingTask.title}'")
        } else {
            // Soft delete so we can synchronize deletion to safety on next sync
            val deletedTask = existingTask.copy(
                isDeletedLocally = true,
                syncStatus = SyncStatus.PENDING_DELETE,
                updatedAt = System.currentTimeMillis()
            )
            taskDao.insertTask(deletedTask)
            emitLog(LogType.INFO, "Soft deleted synced task '${existingTask.title}' (marked for cloud removal)")
        }
    }

    suspend fun clearCompletedTasks() {
        val tasks = taskDao.getAllTasksDirect()
        tasks.forEach { task ->
            if (task.isCompleted) {
                deleteTaskLocally(task.id)
            }
        }
    }

    suspend fun resetDatabase() {
        taskDao.clearAllTasks()
        emitLog(LogType.WARNING, "Local database reset initiated.")
    }

    /**
     * Bidirectional Offline Sync Protocol
     */
    suspend fun syncTasks(isDeviceOnline: Boolean): Boolean {
        if (!isDeviceOnline) {
            emitLog(LogType.ERROR, "Sync failed: Device is currently in Simulated OFFLINE Mode.")
            return false
        }

        emitLog(LogType.INFO, "Initializing secure server-sync connection...")
        try {
            // 1. Process local deletions (Soft Del)
            val deletedTasks = taskDao.getLocallyDeletedTasks()
            if (deletedTasks.isNotEmpty()) {
                emitLog(LogType.INFO, "Found ${deletedTasks.size} soft-deleted tasks ready to purge on remote...")
                deletedTasks.forEach { task ->
                    val serverIdStr = task.id.substringAfter("server_", "")
                    val serverId = serverIdStr.toIntOrNull()
                    if (serverId != null) {
                        try {
                            emitLog(LogType.INFO, "Purging task on cloud (REST DELETE /todos/$serverId)...")
                            api.deleteTodo(serverId)
                            emitLog(LogType.SUCCESS, "Successfully deleted remote task ID $serverId")
                        } catch (e: Exception) {
                            emitLog(LogType.WARNING, "API deletion result returned: ID $serverId unpersisted on backend. Force local purge.")
                        }
                    }
                    taskDao.deleteTaskById(task.id)
                }
            }

            // 2. Upload pending local creations
            val pendingTasks = taskDao.getPendingTasks()
            val creations = pendingTasks.filter { it.syncStatus == SyncStatus.PENDING_CREATE }
            if (creations.isNotEmpty()) {
                emitLog(LogType.INFO, "Uploading ${creations.size} pending creations to remote...")
                creations.forEach { task ->
                    try {
                        val payload = RemoteTodo(
                            title = task.title,
                            completed = task.isCompleted
                        )
                        emitLog(LogType.INFO, "Triggering REST POST /todos (Task: '${task.title}')")
                        val response = api.createTodo(payload)
                        emitLog(LogType.SUCCESS, "Saved remote for ID ${task.id.take(6)}. Response ID: ${response.id}")
                        // Mark synced in local
                        val syncedTask = task.copy(syncStatus = SyncStatus.SYNCED)
                        taskDao.insertTask(syncedTask)
                    } catch (e: Exception) {
                        emitLog(LogType.ERROR, "Failed uploading task info: ${e.message}")
                    }
                }
            }

            // 3. Upload pending local updates
            val updates = pendingTasks.filter { it.syncStatus == SyncStatus.PENDING_UPDATE }
            if (updates.isNotEmpty()) {
                emitLog(LogType.INFO, "Uploading ${updates.size} pending completed/details updates...")
                updates.forEach { task ->
                    try {
                        val serverIdStr = task.id.substringAfter("server_", "")
                        val serverId = serverIdStr.toIntOrNull() ?: 1 // Default Mock server id for clean REST sync
                        val payload = RemoteTodo(
                            id = serverId,
                            title = task.title,
                            completed = task.isCompleted
                        )
                        emitLog(LogType.INFO, "Triggering REST PUT /todos/$serverId (Status Completed: ${task.isCompleted})")
                        api.updateTodo(serverId, payload)
                        emitLog(LogType.SUCCESS, "Updated remote task status for server ID $serverId")
                        // Mark synced
                        val syncedTask = task.copy(syncStatus = SyncStatus.SYNCED)
                        taskDao.insertTask(syncedTask)
                    } catch (e: Exception) {
                        emitLog(LogType.ERROR, "Failed updating remote status: ${e.message}")
                    }
                }
            }

            // 4. Download and pull server updates to local (bidirectional merge)
            emitLog(LogType.INFO, "Downloading fresh task list from Cloud (GET /todos)...")
            val remoteTodos = api.getTodos()
            emitLog(LogType.SUCCESS, "Successfully fetched ${remoteTodos.size} todos from server.")

            var mergedQty = 0
            remoteTodos.forEach { remote ->
                val localId = "server_${remote.id}"
                // Check if already in DB
                val currentLocal = taskDao.getTaskById(localId)
                if (currentLocal == null) {
                    // Create new locally. Since it came from remote, it is fully SYNCED
                    val newTask = Task(
                        id = localId,
                        title = remote.title.replaceFirstChar { it.uppercase() },
                        description = "Discovered via Cloud Sync",
                        isCompleted = remote.completed,
                        syncStatus = SyncStatus.SYNCED,
                        createdAt = System.currentTimeMillis() - (1000 * 3600 * (remote.id ?: 1)) // simulated stagger
                    )
                    taskDao.insertTask(newTask)
                    mergedQty++
                } else if (currentLocal.syncStatus == SyncStatus.SYNCED) {
                    // Pull server status if local matches synced (server wins update)
                    if (currentLocal.isCompleted != remote.completed) {
                        val updated = currentLocal.copy(
                            isCompleted = remote.completed,
                            updatedAt = System.currentTimeMillis()
                        )
                        taskDao.insertTask(updated)
                        emitLog(LogType.INFO, "Merged server task '${remote.title.take(15)}...' updated state.")
                    }
                }
            }

            if (mergedQty > 0) {
                emitLog(LogType.SUCCESS, "Bidirectional sync complete: Merged $mergedQty new tasks into local DB")
            } else {
                emitLog(LogType.SUCCESS, "Local tasks already in-sync with server.")
            }

            return true

        } catch (e: Exception) {
            emitLog(LogType.ERROR, "Sync process failed abruptly: ${e.message ?: "Unknown socket error"}")
            return false
        }
    }
}
