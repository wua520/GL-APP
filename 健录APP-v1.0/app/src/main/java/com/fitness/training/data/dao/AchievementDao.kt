package com.fitness.training.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.fitness.training.data.entity.Achievement

@Dao
interface AchievementDao {
    
    @Query("SELECT * FROM achievements ORDER BY category, targetValue")
    fun getAllAchievements(): LiveData<List<Achievement>>
    
    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): LiveData<List<Achievement>>
    
    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: String): Achievement?
    
    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    fun getUnlockedCount(): LiveData<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: Achievement)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<Achievement>)
    
    @Update
    suspend fun update(achievement: Achievement)
    
    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :time WHERE id = :id")
    suspend fun unlockAchievement(id: String, time: Long)
    
    @Query("UPDATE achievements SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int)
    
    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getCount(): Int
    
    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievementsSync(): List<Achievement>

    @Query("UPDATE achievements SET isUnlocked = 0, unlockedAt = NULL, progress = 0")
    suspend fun resetAll()
}
