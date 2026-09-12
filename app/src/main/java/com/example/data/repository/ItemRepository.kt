package com.example.data.repository

import com.example.data.local.ItemDao
import com.example.data.local.ItemEntity
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {
    val allItems: Flow<List<ItemEntity>> = itemDao.getAllItems()
    val favoriteItems: Flow<List<ItemEntity>> = itemDao.getFavoriteItems()
    val itemCount: Flow<Int> = itemDao.getItemCount()

    fun getRecentlyAdded(limit: Int = 10): Flow<List<ItemEntity>> =
        itemDao.getRecentlyAddedItems(limit)

    fun getRecentlyViewed(limit: Int = 10): Flow<List<ItemEntity>> =
        itemDao.getRecentlyViewedItems(limit)

    fun getItemById(id: Long): Flow<ItemEntity?> =
        itemDao.getItemById(id)

    suspend fun getItemByIdDirect(id: Long): ItemEntity? =
        itemDao.getItemByIdDirect(id)

    fun getItemsByBoxId(boxId: Long): Flow<List<ItemEntity>> =
        itemDao.getItemsByBoxId(boxId)

    fun getItemsByCategory(category: String): Flow<List<ItemEntity>> =
        itemDao.getItemsByCategory(category)

    fun searchItems(query: String): Flow<List<ItemEntity>> =
        itemDao.searchItems(query)

    suspend fun insertItem(item: ItemEntity): Long =
        itemDao.insertItem(item)

    suspend fun updateItem(item: ItemEntity) =
        itemDao.updateItem(item)

    suspend fun deleteItem(item: ItemEntity) =
        itemDao.deleteItem(item)

    suspend fun deleteItemById(id: Long) =
        itemDao.deleteItemById(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        itemDao.updateFavorite(id, isFavorite)

    suspend fun markViewed(id: Long) =
        itemDao.updateLastViewed(id)

    suspend fun removeItemFromBox(itemId: Long) {
        val item = itemDao.getItemByIdDirect(itemId)
        if (item != null) {
            itemDao.updateItem(item.copy(boxId = null, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun addItemToBox(itemId: Long, boxId: Long) {
        val item = itemDao.getItemByIdDirect(itemId)
        if (item != null) {
            itemDao.updateItem(item.copy(boxId = boxId, updatedAt = System.currentTimeMillis()))
        }
    }
}
