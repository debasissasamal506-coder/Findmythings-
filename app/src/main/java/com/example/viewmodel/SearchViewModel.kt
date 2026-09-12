package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ItemEntity
import com.example.data.repository.BoxRepository
import com.example.data.repository.ItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val itemRepository: ItemRepository,
    private val boxRepository: BoxRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _onlyWithVoiceNote = MutableStateFlow(false)
    val onlyWithVoiceNote: StateFlow<Boolean> = _onlyWithVoiceNote.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<ItemEntity>> =
        combine(_searchQuery, _selectedCategory, _onlyWithVoiceNote) { query, category, voiceOnly ->
            Triple(query.trim(), category, voiceOnly)
        }.flatMapLatest { (query, category, voiceOnly) ->
            val baseFlow = if (query.isEmpty()) {
                if (category != null) {
                    itemRepository.getItemsByCategory(category)
                } else {
                    itemRepository.allItems
                }
            } else {
                itemRepository.searchItems(query).combine(itemRepository.allItems) { results, _ ->
                    if (category != null) {
                        results.filter { it.category.equals(category, ignoreCase = true) }
                    } else {
                        results
                    }
                }
            }
            baseFlow.combine(itemRepository.allItems) { items, _ ->
                if (voiceOnly) {
                    items.filter { !it.audioUri.isNullOrBlank() }
                } else {
                    items
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun toggleOnlyVoiceNote() {
        _onlyWithVoiceNote.value = !_onlyWithVoiceNote.value
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _onlyWithVoiceNote.value = false
    }

    fun toggleFavorite(item: ItemEntity) {
        viewModelScope.launch {
            itemRepository.toggleFavorite(item.id, !item.favorite)
        }
    }

    class Factory(
        private val itemRepository: ItemRepository,
        private val boxRepository: BoxRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(itemRepository, boxRepository) as T
        }
    }
}
