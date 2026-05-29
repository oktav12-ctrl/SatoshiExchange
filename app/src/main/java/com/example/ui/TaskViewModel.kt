package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LogType
import com.example.data.SyncLog
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // Reactive task stream directly from local SQLite
    val tasks: StateFlow<List<Task>> = repository.allTasksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Togglable network connectivity state simulator
    private val _isDeviceOnline = MutableStateFlow(true)
    val isDeviceOnline: StateFlow<Boolean> = _isDeviceOnline.asStateFlow()

    // Sound feedback state control
    private val _isAudioEnabled = MutableStateFlow(true)
    val isAudioEnabled: StateFlow<Boolean> = _isAudioEnabled.asStateFlow()

    // Active synchronization indicator
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Diagnostic console trace logger
    private val _syncLogsList = MutableStateFlow<List<SyncLog>>(emptyList())
    val syncLogsList: StateFlow<List<SyncLog>> = _syncLogsList.asStateFlow()

    init {
        // Feed live log trace events into the ViewModel scrolling console
        viewModelScope.launch {
            repository.emitLog(LogType.SUCCESS, "Local Task Database initialized successfully.")
            repository.emitLog(LogType.INFO, "Pre-flight check: Online Mode toggled ON.")
            
            repository.syncLogs.collect { log ->
                val currentList = _syncLogsList.value
                // Keep last 100 log messages in buffer
                _syncLogsList.value = (currentList + log).takeLast(100)
            }
        }
    }

    fun toggleOnlineSimulator(isOnline: Boolean) {
        _isDeviceOnline.value = isOnline
        viewModelScope.launch {
            val mood = if (isOnline) "ONLINE (REST API enabled)" else "OFFLINE (Local-only database cached)"
            repository.emitLog(
                if (isOnline) LogType.SUCCESS else LogType.WARNING,
                "Device mode swiped to: $mood"
            )
            if (!isOnline) {
                SoundHelper.playWarning()
            } else {
                SoundHelper.playSyncComplete()
            }
        }
    }

    fun toggleAudioFeedback(enabled: Boolean) {
        _isAudioEnabled.value = enabled
        SoundHelper.isAudioEnabled = enabled
        viewModelScope.launch {
            val mood = if (enabled) "ENABLED" else "MUTED"
            repository.emitLog(
                if (enabled) LogType.SUCCESS else LogType.WARNING,
                "System audio feedback has been toggled to: $mood"
            )
            if (enabled) {
                SoundHelper.playTaskCompletion()
            }
        }
    }

    fun addNewTask(title: String, description: String) {
        viewModelScope.launch {
            if (title.isBlank()) return@launch
            repository.createTask(title, description, _isDeviceOnline.value)
        }
    }

    fun toggleTaskCompletion(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTaskStatus(id, isCompleted, _isDeviceOnline.value)
            if (isCompleted) {
                SoundHelper.playTaskCompletion()
            }
        }
    }

    fun updateTaskInfo(id: String, title: String, description: String) {
        viewModelScope.launch {
            repository.updateTaskDetails(id, title, description)
        }
    }

    fun removeTask(id: String) {
        viewModelScope.launch {
            repository.deleteTaskLocally(id)
            SoundHelper.playWarning()
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            repository.clearCompletedTasks()
        }
    }

    fun purgeDatabase() {
        viewModelScope.launch {
            SoundHelper.playWarning()
            repository.resetDatabase()
            _syncLogsList.value = emptyList()
            repository.emitLog(LogType.WARNING, "All local state has been wiped cleanly.")
        }
    }

    fun clearLogStream() {
        _syncLogsList.value = emptyList()
    }

    fun triggerSyncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            repository.emitLog(LogType.INFO, "Starting database sync cycle... checking dirty flags")
            
            // Artificial delay to make sync feel technical, asynchronous, and visually responsive
            kotlinx.coroutines.delay(1200)
            
            val success = repository.syncTasks(_isDeviceOnline.value)
            if (success) {
                SoundHelper.playSyncComplete()
            } else {
                SoundHelper.playWarning()
            }
            _isSyncing.value = false
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
