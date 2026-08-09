package com.fitness.training.data.dao

import androidx.room.*
import com.fitness.training.data.entity.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {
    @Query("SELECT * FROM workout_templates WHERE userId = :userId OR isPreset = 1 ORDER BY name ASC")
    fun getAllTemplates(userId: Long): Flow<List<WorkoutTemplate>>
    
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): WorkoutTemplate?
    
    @Query("SELECT * FROM workout_templates WHERE isPreset = 1 ORDER BY name ASC")
    fun getPresetTemplates(): Flow<List<WorkoutTemplate>>
    
    @Insert
    suspend fun insertTemplate(template: WorkoutTemplate): Long
    
    @Update
    suspend fun updateTemplate(template: WorkoutTemplate)
    
    @Delete
    suspend fun deleteTemplate(template: WorkoutTemplate)
    
    @Query("DELETE FROM workout_templates WHERE userId = :userId AND isPreset = 0")
    suspend fun deleteByUser(userId: Long)
}


