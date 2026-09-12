package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ItemEntity
import com.example.data.local.TaskEntity
import com.example.data.repository.ItemRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TodoTab {
    TODAY,
    UPCOMING,
    COMPLETED
}

class TodoViewModel(
    private val taskRepository: TaskRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(TodoTab.TODAY)
    val selectedTab: StateFlow<TodoTab> = _selectedTab.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // All tasks stream
    val allTasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incompleteTasks: StateFlow<List<TaskEntity>> = taskRepository.incompleteTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks: StateFlow<List<TaskEntity>> = taskRepository.completedTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = taskRepository.totalTaskCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val incompleteCount: StateFlow<Int> = taskRepository.incompleteTaskCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Items list for attaching a related Thing to a task
    val allThings: StateFlow<List<ItemEntity>> = itemRepository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed Today's Tasks
    val todayTasks: StateFlow<List<TaskEntity>> = combine(allTasks) { tasksArray ->
        val tasks = tasksArray[0]
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = cal.timeInMillis
        tasks.filter { !it.isCompleted && (it.dueDate == null || it.dueDate <= endOfDay) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed Upcoming Tasks
    val upcomingTasks: StateFlow<List<TaskEntity>> = combine(allTasks) { tasksArray ->
        val tasks = tasksArray[0]
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = cal.timeInMillis
        tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate > endOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: TodoTab) {
        _selectedTab.value = tab
    }

    fun selectCategoryFilter(cat: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == cat) null else cat
    }

    fun toggleTask(taskId: Long, currentCompleted: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompleted(taskId, !currentCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun addTask(
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: Long?,
        dueTimeFormatted: String,
        reminderSet: Boolean,
        repeatType: String,
        relatedThing: ItemEntity?
    ) {
        if (title.isBlank()) return

        val task = TaskEntity(
            title = title.trim(),
            description = description.trim(),
            category = category,
            priority = priority,
            dueDate = dueDate,
            dueTimeFormatted = dueTimeFormatted,
            isCompleted = false,
            reminderSet = reminderSet,
            repeatType = repeatType,
            relatedThingId = relatedThing?.id,
            relatedThingName = relatedThing?.name,
            relatedThingLocation = relatedThing?.fullLocation()
        )

        viewModelScope.launch {
            taskRepository.insertTask(task)
        }
    }

    class Factory(
        private val taskRepository: TaskRepository,
        private val itemRepository: ItemRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
                return TodoViewModel(taskRepository, itemRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
