package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ItemEntity
import com.example.data.local.TaskEntity
import com.example.data.repository.BoxRepository
import com.example.data.repository.ItemRepository
import com.example.data.repository.TaskRepository
import com.example.model.CategoryWithCount
import com.example.model.ItemCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(
    private val itemRepository: ItemRepository,
    private val boxRepository: BoxRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val recentlyAdded: StateFlow<List<ItemEntity>> =
        itemRepository.getRecentlyAdded(limit = 10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyViewed: StateFlow<List<ItemEntity>> =
        itemRepository.getRecentlyViewed(limit = 6)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteItems: StateFlow<List<ItemEntity>> =
        itemRepository.favoriteItems
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalItems: StateFlow<Int> =
        itemRepository.itemCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBoxes: StateFlow<Int> =
        boxRepository.boxCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalTasks: StateFlow<Int> =
        taskRepository.incompleteTaskCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayTasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks.map { tasks ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = cal.timeInMillis
        tasks.filter { !it.isCompleted && (it.dueDate == null || it.dueDate <= endOfDay) }.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryCounts: StateFlow<List<CategoryWithCount>> =
        itemRepository.allItems.map { items ->
            ItemCategory.entries.map { cat ->
                val count = items.count { it.category.equals(cat.displayName, ignoreCase = true) }
                CategoryWithCount(category = cat, count = count)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(item: ItemEntity) {
        viewModelScope.launch {
            itemRepository.toggleFavorite(item.id, !item.favorite)
        }
    }

    fun toggleTask(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompleted(taskId, !isCompleted)
        }
    }

    class Factory(
        private val itemRepository: ItemRepository,
        private val boxRepository: BoxRepository,
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(itemRepository, boxRepository, taskRepository) as T
        }
    }
}

