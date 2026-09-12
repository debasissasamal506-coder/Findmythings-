package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BoxDao {
    @Query("SELECT * FROM boxes ORDER BY name ASC")
    fun getAllBoxes(): Flow<List<BoxEntity>>

    @Query("SELECT * FROM boxes WHERE id = :id")
    fun getBoxById(id: Long): Flow<BoxEntity?>

    @Query("SELECT * FROM boxes WHERE id = :id")
    suspend fun getBoxByIdDirect(id: Long): BoxEntity?

    @Query("SELECT * FROM boxes WHERE name LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' OR (notes IS NOT NULL AND notes LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchBoxes(query: String): Flow<List<BoxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBox(box: BoxEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(boxes: List<BoxEntity>)

    @Update
    suspend fun updateBox(box: BoxEntity)

    @Delete
    suspend fun deleteBox(box: BoxEntity)

    @Query("DELETE FROM boxes WHERE id = :id")
    suspend fun deleteBoxById(id: Long)

    @Query("SELECT COUNT(*) FROM boxes")
    fun getBoxCount(): Flow<Int>

    @Query("SELECT * FROM boxes")
    suspend fun getAllBoxesList(): List<BoxEntity>

    @Query("DELETE FROM boxes")
    suspend fun clearAll()
}
