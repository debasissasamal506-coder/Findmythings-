package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.BoxEntity
import com.example.data.local.ItemEntity
import com.example.data.local.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(private val database: AppDatabase) {

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val items = database.itemDao().getAllItemsList()
        val boxes = database.boxDao().getAllBoxesList()
        val tasks = database.taskDao().getAllTasksList()

        val root = JSONObject()
        root.put("appName", "Find My Things")
        root.put("version", 3)
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("itemCount", items.size)
        root.put("boxCount", boxes.size)
        root.put("taskCount", tasks.size)

        val boxesArray = JSONArray()
        for (box in boxes) {
            val bObj = JSONObject()
            bObj.put("id", box.id)
            bObj.put("name", box.name)
            bObj.put("location", box.location)
            bObj.put("notes", box.notes ?: "")
            bObj.put("createdAt", box.createdAt)
            bObj.put("updatedAt", box.updatedAt)
            boxesArray.put(bObj)
        }
        root.put("boxes", boxesArray)

        val itemsArray = JSONArray()
        for (item in items) {
            val iObj = JSONObject()
            iObj.put("id", item.id)
            iObj.put("name", item.name)
            iObj.put("category", item.category)
            iObj.put("photoUri", item.photoUri ?: "")
            iObj.put("mainLocation", item.mainLocation)
            iObj.put("room", item.room)
            iObj.put("storage", item.storage)
            iObj.put("specificLocation", item.specificLocation)
            iObj.put("notes", item.notes ?: "")
            iObj.put("favorite", item.favorite)
            if (item.boxId != null) {
                iObj.put("boxId", item.boxId)
            }
            iObj.put("createdAt", item.createdAt)
            iObj.put("updatedAt", item.updatedAt)
            iObj.put("lastViewedAt", item.lastViewedAt)
            iObj.put("reminderDate", item.reminderDate ?: 0L)
            iObj.put("reminderEnabled", item.reminderEnabled)
            iObj.put("audioUri", item.audioUri ?: "")
            iObj.put("audioDurationSec", item.audioDurationSec)
            itemsArray.put(iObj)
        }
        root.put("items", itemsArray)

        val tasksArray = JSONArray()
        for (task in tasks) {
            val tObj = JSONObject()
            tObj.put("id", task.id)
            tObj.put("title", task.title)
            tObj.put("description", task.description)
            tObj.put("category", task.category)
            tObj.put("priority", task.priority)
            tObj.put("isCompleted", task.isCompleted)
            if (task.dueDate != null) tObj.put("dueDate", task.dueDate)
            tObj.put("dueTimeFormatted", task.dueTimeFormatted ?: "")
            tObj.put("reminderSet", task.reminderSet)
            tObj.put("repeatType", task.repeatType)
            if (task.relatedThingId != null) tObj.put("relatedThingId", task.relatedThingId)
            tObj.put("relatedThingName", task.relatedThingName ?: "")
            tObj.put("relatedThingLocation", task.relatedThingLocation ?: "")
            if (task.relatedBoxId != null) tObj.put("relatedBoxId", task.relatedBoxId)
            tObj.put("relatedBoxName", task.relatedBoxName ?: "")
            tObj.put("relatedBoxLocation", task.relatedBoxLocation ?: "")
            tObj.put("createdAt", task.createdAt)
            tObj.put("updatedAt", task.updatedAt)
            if (task.completedAt != null) tObj.put("completedAt", task.completedAt)
            tasksArray.put(tObj)
        }
        root.put("tasks", tasksArray)

        root.toString(2)
    }

    sealed class RestoreResult {
        data class Success(val itemsRestored: Int, val boxesRestored: Int, val tasksRestored: Int = 0) : RestoreResult()
        data class Error(val message: String) : RestoreResult()
    }

    suspend fun restoreFromJson(jsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("items") && !root.has("boxes") && !root.has("tasks")) {
                return@withContext RestoreResult.Error("Invalid backup file: missing items, boxes, or tasks data.")
            }

            val restoredBoxes = mutableListOf<BoxEntity>()
            if (root.has("boxes")) {
                val boxesArray = root.getJSONArray("boxes")
                for (i in 0 until boxesArray.length()) {
                    val b = boxesArray.getJSONObject(i)
                    restoredBoxes.add(
                        BoxEntity(
                            id = b.optLong("id", 0L),
                            name = b.optString("name", "Box"),
                            location = b.optString("location", ""),
                            notes = if (b.optString("notes").isBlank()) null else b.optString("notes"),
                            createdAt = b.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = b.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val restoredItems = mutableListOf<ItemEntity>()
            if (root.has("items")) {
                val itemsArray = root.getJSONArray("items")
                for (i in 0 until itemsArray.length()) {
                    val itm = itemsArray.getJSONObject(i)
                    val boxIdVal = if (itm.has("boxId") && !itm.isNull("boxId")) itm.getLong("boxId") else null
                    restoredItems.add(
                        ItemEntity(
                            id = itm.optLong("id", 0L),
                            name = itm.optString("name", "Item"),
                            category = itm.optString("category", "Other"),
                            photoUri = if (itm.optString("photoUri").isBlank()) null else itm.optString("photoUri"),
                            mainLocation = itm.optString("mainLocation", ""),
                            room = itm.optString("room", ""),
                            storage = itm.optString("storage", ""),
                            specificLocation = itm.optString("specificLocation", ""),
                            notes = if (itm.optString("notes").isBlank()) null else itm.optString("notes"),
                            favorite = itm.optBoolean("favorite", false),
                            boxId = boxIdVal,
                            createdAt = itm.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = itm.optLong("updatedAt", System.currentTimeMillis()),
                            lastViewedAt = itm.optLong("lastViewedAt", System.currentTimeMillis()),
                            reminderDate = if (itm.optLong("reminderDate", 0L) > 0) itm.getLong("reminderDate") else null,
                            reminderEnabled = itm.optBoolean("reminderEnabled", false),
                            audioUri = if (itm.optString("audioUri").isBlank()) null else itm.optString("audioUri"),
                            audioDurationSec = itm.optInt("audioDurationSec", 0)
                        )
                    )
                }
            }

            val restoredTasks = mutableListOf<TaskEntity>()
            if (root.has("tasks")) {
                val tasksArray = root.getJSONArray("tasks")
                for (i in 0 until tasksArray.length()) {
                    val t = tasksArray.getJSONObject(i)
                    val relatedThingIdVal = if (t.has("relatedThingId") && !t.isNull("relatedThingId")) t.getLong("relatedThingId") else null
                    val relatedBoxIdVal = if (t.has("relatedBoxId") && !t.isNull("relatedBoxId")) t.getLong("relatedBoxId") else null
                    val dueDateVal = if (t.has("dueDate") && !t.isNull("dueDate")) t.getLong("dueDate") else null
                    val completedAtVal = if (t.has("completedAt") && !t.isNull("completedAt")) t.getLong("completedAt") else null

                    restoredTasks.add(
                        TaskEntity(
                            id = t.optLong("id", 0L),
                            title = t.optString("title", "Task"),
                            description = t.optString("description", ""),
                            category = t.optString("category", "Other"),
                            priority = t.optString("priority", "Medium"),
                            isCompleted = t.optBoolean("isCompleted", false),
                            dueDate = dueDateVal,
                            dueTimeFormatted = t.optString("dueTimeFormatted", ""),
                            reminderSet = t.optBoolean("reminderSet", false),
                            repeatType = t.optString("repeatType", "None"),
                            relatedThingId = relatedThingIdVal,
                            relatedThingName = if (t.optString("relatedThingName").isBlank()) null else t.optString("relatedThingName"),
                            relatedThingLocation = if (t.optString("relatedThingLocation").isBlank()) null else t.optString("relatedThingLocation"),
                            relatedBoxId = relatedBoxIdVal,
                            relatedBoxName = if (t.optString("relatedBoxName").isBlank()) null else t.optString("relatedBoxName"),
                            relatedBoxLocation = if (t.optString("relatedBoxLocation").isBlank()) null else t.optString("relatedBoxLocation"),
                            createdAt = t.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = t.optLong("updatedAt", System.currentTimeMillis()),
                            completedAt = completedAtVal
                        )
                    )
                }
            }

            // Insert boxes first, then items, then tasks
            database.boxDao().insertAll(restoredBoxes)
            database.itemDao().insertAll(restoredItems)
            database.taskDao().insertAll(restoredTasks)

            RestoreResult.Success(
                itemsRestored = restoredItems.size,
                boxesRestored = restoredBoxes.size,
                tasksRestored = restoredTasks.size
            )
        } catch (e: Exception) {
            RestoreResult.Error("Failed to restore: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
