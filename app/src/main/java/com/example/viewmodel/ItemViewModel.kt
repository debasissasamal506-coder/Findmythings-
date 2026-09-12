package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BoxEntity
import com.example.data.local.ItemEntity
import com.example.data.repository.BoxRepository
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.ItemRepository
import com.example.utils.ImageStorageHelper
import com.example.utils.ReminderScheduler
import com.example.utils.VoiceRecordManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemFormState(
    val id: Long = 0L,
    val name: String = "",
    val category: String = "Documents",
    val photoUri: String? = null,
    val mainLocation: String = "",
    val room: String = "",
    val storage: String = "",
    val specificLocation: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val boxId: Long? = null,
    val reminderDate: Long? = null,
    val reminderEnabled: Boolean = false,
    val audioUri: String? = null,
    val audioDurationSec: Int = 0,
    val nameError: String? = null,
    val locationError: String? = null,
    val isSaving: Boolean = false
) {
    val liveLocationPreview: String
        get() {
            val parts = listOf(mainLocation, room, storage, specificLocation).filter { it.isNotBlank() }
            return if (parts.isEmpty()) "Location preview will appear here" else parts.joinToString(" → ")
        }
}

class ItemViewModel(
    private val itemRepository: ItemRepository,
    private val boxRepository: BoxRepository,
    private val cloudSyncRepository: CloudSyncRepository? = null
) : ViewModel() {

    private val _formState = MutableStateFlow(ItemFormState())
    val formState: StateFlow<ItemFormState> = _formState.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Long>()
    val saveSuccessEvent: SharedFlow<Long> = _saveSuccessEvent.asSharedFlow()

    private val _currentItemId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentItem: StateFlow<ItemEntity?> = _currentItemId.flatMapLatest { id ->
        if (id != null) itemRepository.getItemById(id) else kotlinx.coroutines.flow.flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentBox: StateFlow<BoxEntity?> = currentItem.flatMapLatest { item ->
        val bId = item?.boxId
        if (bId != null) boxRepository.getBoxById(bId) else kotlinx.coroutines.flow.flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableBoxes: StateFlow<List<BoxEntity>> =
        boxRepository.allBoxes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initForNewItem(defaultCategory: String = "Documents", presetBoxId: Long? = null) {
        _currentItemId.value = null
        _formState.value = ItemFormState(
            category = defaultCategory,
            boxId = presetBoxId
        )
    }

    fun loadItem(itemId: Long, recordView: Boolean = true) {
        _currentItemId.value = itemId
        viewModelScope.launch {
            val item = itemRepository.getItemByIdDirect(itemId)
            if (item != null) {
                if (recordView) {
                    itemRepository.markViewed(itemId)
                }
                // Also populate form state in case user navigates to edit
                _formState.value = ItemFormState(
                    id = item.id,
                    name = item.name,
                    category = item.category,
                    photoUri = item.photoUri,
                    mainLocation = item.mainLocation,
                    room = item.room,
                    storage = item.storage,
                    specificLocation = item.specificLocation,
                    notes = item.notes ?: "",
                    favorite = item.favorite,
                    boxId = item.boxId,
                    reminderDate = item.reminderDate,
                    reminderEnabled = item.reminderEnabled,
                    audioUri = item.audioUri,
                    audioDurationSec = item.audioDurationSec
                )
            }
        }
    }

    fun onAudioRecorded(uri: String, durationSec: Int) {
        // If there was an existing audio file different from this new one, remove it
        val oldUri = _formState.value.audioUri
        if (!oldUri.isNullOrBlank() && oldUri != uri) {
            VoiceRecordManager.deleteVoiceFile(oldUri)
        }
        _formState.value = _formState.value.copy(audioUri = uri, audioDurationSec = durationSec)
    }

    fun onRemoveAudio() {
        val oldUri = _formState.value.audioUri
        if (!oldUri.isNullOrBlank()) {
            VoiceRecordManager.deleteVoiceFile(oldUri)
        }
        _formState.value = _formState.value.copy(audioUri = null, audioDurationSec = 0)
    }

    fun onNameChange(name: String) {
        _formState.value = _formState.value.copy(name = name, nameError = null)
    }

    fun onCategoryChange(category: String) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun onPhotoSelected(photoUri: String?) {
        _formState.value = _formState.value.copy(photoUri = photoUri)
    }

    fun onMainLocationChange(value: String) {
        _formState.value = _formState.value.copy(mainLocation = value, locationError = null)
    }

    fun onRoomChange(value: String) {
        _formState.value = _formState.value.copy(room = value, locationError = null)
    }

    fun onStorageChange(value: String) {
        _formState.value = _formState.value.copy(storage = value, locationError = null)
    }

    fun onSpecificLocationChange(value: String) {
        _formState.value = _formState.value.copy(specificLocation = value, locationError = null)
    }

    fun onNotesChange(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }

    fun onFavoriteToggle(favorite: Boolean) {
        _formState.value = _formState.value.copy(favorite = favorite)
    }

    fun onBoxSelected(boxId: Long?) {
        _formState.value = _formState.value.copy(boxId = boxId)
    }

    fun onReminderToggle(enabled: Boolean) {
        _formState.value = _formState.value.copy(reminderEnabled = enabled)
    }

    fun onReminderDateSelected(timestamp: Long) {
        _formState.value = _formState.value.copy(reminderDate = timestamp, reminderEnabled = true)
    }

    fun saveItem(context: Context) {
        val state = _formState.value
        val trimmedName = state.name.trim()
        if (trimmedName.isBlank()) {
            _formState.value = state.copy(nameError = "Please enter an item name")
            return
        }

        val hasLocation = state.mainLocation.isNotBlank() ||
                state.room.isNotBlank() ||
                state.storage.isNotBlank() ||
                state.specificLocation.isNotBlank()

        if (!hasLocation) {
            _formState.value = state.copy(locationError = "Please enter at least one location detail")
            return
        }

        _formState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = ItemEntity(
                id = state.id,
                name = trimmedName,
                category = state.category,
                photoUri = state.photoUri,
                mainLocation = state.mainLocation.trim(),
                room = state.room.trim(),
                storage = state.storage.trim(),
                specificLocation = state.specificLocation.trim(),
                notes = state.notes.trim().ifEmpty { null },
                favorite = state.favorite,
                boxId = state.boxId,
                createdAt = if (state.id == 0L) now else (itemRepository.getItemByIdDirect(state.id)?.createdAt ?: now),
                updatedAt = now,
                lastViewedAt = now,
                reminderDate = state.reminderDate,
                reminderEnabled = state.reminderEnabled && (state.reminderDate ?: 0) > now,
                audioUri = state.audioUri,
                audioDurationSec = state.audioDurationSec
            )

            val savedId = if (state.id == 0L) {
                itemRepository.insertItem(entity)
            } else {
                itemRepository.updateItem(entity)
                state.id
            }

            // Handle reminders
            val savedEntity = entity.copy(id = savedId)
            if (savedEntity.reminderEnabled && savedEntity.reminderDate != null) {
                ReminderScheduler.scheduleReminder(context, savedEntity)
            } else {
                ReminderScheduler.cancelReminder(context, savedId)
            }

            _formState.value = state.copy(isSaving = false)
            _saveSuccessEvent.emit(savedId)
            cloudSyncRepository?.autoSyncIfLoggedIn()
        }
    }

    fun toggleFavoriteCurrentItem() {
        val item = currentItem.value ?: return
        viewModelScope.launch {
            val newFav = !item.favorite
            itemRepository.toggleFavorite(item.id, newFav)
            cloudSyncRepository?.autoSyncIfLoggedIn()
        }
    }

    fun deleteItem(context: Context, itemId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val item = itemRepository.getItemByIdDirect(itemId)
            if (item != null) {
                ReminderScheduler.cancelReminder(context, itemId)
                ImageStorageHelper.deleteInternalImage(item.photoUri)
                VoiceRecordManager.deleteVoiceFile(item.audioUri)
                itemRepository.deleteItem(item)
                cloudSyncRepository?.autoSyncIfLoggedIn()
            }
            onDeleted()
        }
    }

    class Factory(
        private val itemRepository: ItemRepository,
        private val boxRepository: BoxRepository,
        private val cloudSyncRepository: CloudSyncRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ItemViewModel(itemRepository, boxRepository, cloudSyncRepository) as T
        }
    }
}
