package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ItemEntity
import com.example.data.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val itemRepository: ItemRepository
) : ViewModel() {

    val favoriteItems: StateFlow<List<ItemEntity>> =
        itemRepository.favoriteItems
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(item: ItemEntity) {
        viewModelScope.launch {
            itemRepository.toggleFavorite(item.id, !item.favorite)
        }
    }

    class Factory(
        private val itemRepository: ItemRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FavoritesViewModel(itemRepository) as T
        }
    }
}
