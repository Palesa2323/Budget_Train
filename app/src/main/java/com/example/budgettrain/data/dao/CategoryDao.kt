package com.example.budgettrain.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.budgettrain.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<Category>)

    @Query("SELECT * FROM categories ORDER BY name")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: Category): Long
}


