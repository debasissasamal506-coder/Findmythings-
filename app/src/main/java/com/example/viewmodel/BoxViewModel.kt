package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BoxEntity
import com.example.data.local.ItemEntity
import com.example.data.repository.BoxRepository
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.ItemRepository
import com.example.model.BoxWithItemCount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BoxViewModel(
    private val boxRepository: BoxRepository,
    private val itemRepository: ItemRepository,
    private val cloudSyncRepository: CloudSyncRepository? = null
) : ViewModel() {

    private val _boxSearchQuery = MutableStateFlow("")
    val boxSearchQuery: StateFlow<String> = _boxSearchQuery.asStateFlow()

    val boxesWithCounts: StateFlow<List<BoxWithItemCount>> =
        combine(boxRepository.boxesWithItemCounts, _boxSearchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.box.name.contains(query, ignoreCase = true) ||
                            it.box.location.contains(query, ignoreCase = true) ||
                            (it.box.notes?.contains(query, ignoreCase = true) == true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentBoxId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentBox: StateFlow<BoxEntity?> =
        _currentBoxId.flatMapLatest { id ->
            if (id != null) boxRepository.getBoxById(id) else MutableStateFlow(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val itemsInCurrentBox: StateFlow<List<ItemEntity>> =
        _currentBoxId.flatMapLatest { id ->
            if (id != null) itemRepository.getItemsByBoxId(id) else MutableStateFlow(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unassignedItems: StateFlow<List<ItemEntity>> =
        combine(itemRepository.allItems, _currentBoxId) { all, currentBoxId ->
            all.filter { it.boxId == null || (currentBoxId != null && it.boxId != currentBoxId) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onBoxSearchChange(query: String) {
        _boxSearchQuery.value = query
    }

    fun selectBox(boxId: Long) {
        _currentBoxId.value = boxId
    }

    fun saveBox(id: Long, name: String, location: String, notes: String?, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = BoxEntity(
                id = id,
                name = name.trim(),
                location = location.trim(),
                notes = notes?.trim()?.ifEmpty { null },
                createdAt = if (id == 0L) now else (boxRepository.getBoxByIdDirect(id)?.createdAt ?: now),
                updatedAt = now
            )
            val savedId = if (id == 0L) {
                boxRepository.insertBox(entity)
            } else {
                boxRepository.updateBox(entity)
                id
            }
            cloudSyncRepository?.autoSyncIfLoggedIn()
            onComplete(savedId)
        }
    }

    fun deleteBox(boxId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            boxRepository.deleteBox(boxId)
            if (_currentBoxId.value == boxId) {
                _currentBoxId.value = null
            }
            cloudSyncRepository?.autoSyncIfLoggedIn()
            onDeleted()
        }
    }

    fun removeItemFromBox(itemId: Long) {
        viewModelScope.launch {
            itemRepository.removeItemFromBox(itemId)
            cloudSyncRepository?.autoSyncIfLoggedIn()
        }
    }

    fun addItemToBox(itemId: Long, boxId: Long) {
        viewModelScope.launch {
            itemRepository.addItemToBox(itemId, boxId)
            cloudSyncRepository?.autoSyncIfLoggedIn()
        }
    }

    fun toggleFavorite(item: ItemEntity) {
        viewModelScope.launch {
            itemRepository.toggleFavorite(item.id, !item.favorite)
            cloudSyncRepository?.autoSyncIfLoggedIn()
        }
    }

    class Factory(
        private val boxRepository: BoxRepository,
        private val itemRepository: ItemRepository,
        private val cloudSyncRepository: CloudSyncRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BoxViewModel(boxRepository, itemRepository, cloudSyncRepository) as T
        }
    }
}
