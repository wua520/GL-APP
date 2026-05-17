package com.fitness.training.data.dao

import androidx.room.*
import com.fitness.training.data.entity.Food
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods WHERE isCustom = 0 OR userId = :userId ORDER BY name ASC")
    fun getAllFoods(userId: Long): Flow<List<Food>>
    
    @Query("SELECT * FROM foods WHERE (isCustom = 0 OR userId = :userId) AND name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 20")
    suspend fun searchFoods(userId: Long, query: String): List<Food>
    
    @Query("SELECT * FROM foods WHERE (isCustom = 0 OR userId = :userId) AND category = :category ORDER BY name ASC")
    fun getFoodsByCategory(userId: Long, category: String): Flow<List<Food>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: Food): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<Food>)
    
    @Update
    suspend fun update(food: Food)
    
    @Delete
    suspend fun delete(food: Food)
    
    @Query("SELECT COUNT(*) FROM foods WHERE isCustom = 0")
    suspend fun getCount(): Int
    
    @Query("DELETE FROM foods WHERE isCustom = 1 AND userId = :userId")
    suspend fun deleteCustomFoods(userId: Long)
}
