package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = BoxEntity::class,
            parentColumns = ["id"],
            childColumns = ["boxId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["boxId"]),
        Index(value = ["category"]),
        Index(value = ["favorite"]),
        Index(value = ["lastViewedAt"])
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: String,
    val photoUri: String? = null,
    val mainLocation: String,
    val room: String,
    val storage: String,
    val specificLocation: String,
    val notes: String? = null,
    val favorite: Boolean = false,
    val boxId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastViewedAt: Long = System.currentTimeMillis(),
    val reminderDate: Long? = null,
    val reminderEnabled: Boolean = false,
    val audioUri: String? = null,
    val audioDurationSec: Int = 0
) {
    val hasVoiceNote: Boolean
        get() = !audioUri.isNullOrBlank()

    fun formatAudioDuration(): String {
        val minutes = audioDurationSec / 60
        val seconds = audioDurationSec % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun formatLocationHierarchy(): String {
        return listOf(mainLocation, room, storage, specificLocation)
            .filter { it.isNotBlank() }
            .joinToString(" → ")
    }

    fun fullLocation(): String = formatLocationHierarchy()

    fun locationParts(): List<String> {
        return listOf(mainLocation, room, storage, specificLocation)
            .filter { it.isNotBlank() }
    }
}
