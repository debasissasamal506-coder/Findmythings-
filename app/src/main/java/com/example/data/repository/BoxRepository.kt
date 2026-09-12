package com.example.data.repository

import com.example.data.local.BoxDao
import com.example.data.local.BoxEntity
import com.example.data.local.ItemDao
import com.example.model.BoxWithItemCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BoxRepository(
    private val boxDao: BoxDao,
    private val itemDao: ItemDao
) {
    val allBoxes: Flow<List<BoxEntity>> = boxDao.getAllBoxes()
    val boxCount: Flow<Int> = boxDao.getBoxCount()

    val boxesWithItemCounts: Flow<List<BoxWithItemCount>> =
        combine(boxDao.getAllBoxes(), itemDao.getAllItems()) { boxes, items ->
            boxes.map { box ->
                val count = items.count { it.boxId == box.id }
                BoxWithItemCount(box = box, itemCount = count)
            }
        }

    fun getBoxById(id: Long): Flow<BoxEntity?> =
        boxDao.getBoxById(id)

    suspend fun getBoxByIdDirect(id: Long): BoxEntity? =
        boxDao.getBoxByIdDirect(id)

    fun searchBoxes(query: String): Flow<List<BoxEntity>> =
        boxDao.searchBoxes(query)

    suspend fun insertBox(box: BoxEntity): Long =
        boxDao.insertBox(box)

    suspend fun updateBox(box: BoxEntity) =
        boxDao.updateBox(box)

    suspend fun deleteBox(boxId: Long) {
        // Unassign items first to maintain cleanliness
        itemDao.removeItemsFromBox(boxId)
        boxDao.deleteBoxById(boxId)
    }
}
