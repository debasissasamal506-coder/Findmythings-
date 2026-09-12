package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentlyAddedItems(limit: Int = 10): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY lastViewedAt DESC LIMIT :limit")
    fun getRecentlyViewedItems(limit: Int = 10): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE favorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemById(id: Long): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemByIdDirect(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE boxId = :boxId ORDER BY createdAt DESC")
    fun getItemsByBoxId(boxId: Long): Flow<List<ItemEntity>>

    @Query("SELECT COUNT(*) FROM items WHERE boxId = :boxId")
    fun getItemCountForBox(boxId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE boxId = :boxId")
    suspend fun getItemCountForBoxDirect(boxId: Long): Int

    @Query("SELECT * FROM items WHERE category = :category ORDER BY createdAt DESC")
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>

    @Query("""
        SELECT items.* FROM items 
        LEFT JOIN boxes ON items.boxId = boxes.id
        WHERE items.name LIKE '%' || :query || '%'
           OR items.category LIKE '%' || :query || '%'
           OR items.mainLocation LIKE '%' || :query || '%'
           OR items.room LIKE '%' || :query || '%'
           OR items.storage LIKE '%' || :query || '%'
           OR items.specificLocation LIKE '%' || :query || '%'
           OR (items.notes IS NOT NULL AND items.notes LIKE '%' || :query || '%')
           OR (boxes.name IS NOT NULL AND boxes.name LIKE '%' || :query || '%')
        ORDER BY items.updatedAt DESC
    """)
    fun searchItems(query: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE items SET favorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE items SET lastViewedAt = :viewedAt WHERE id = :id")
    suspend fun updateLastViewed(id: Long, viewedAt: Long = System.currentTimeMillis())

    @Query("UPDATE items SET boxId = NULL WHERE boxId = :boxId")
    suspend fun removeItemsFromBox(boxId: Long)

    @Query("SELECT COUNT(*) FROM items")
    fun getItemCount(): Flow<Int>

    @Query("SELECT * FROM items")
    suspend fun getAllItemsList(): List<ItemEntity>

    @Query("DELETE FROM items")
    suspend fun clearAll()
}
