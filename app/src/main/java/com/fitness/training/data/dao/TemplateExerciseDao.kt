package com.fitness.training.data.dao

import androidx.room.*
import com.fitness.training.data.entity.TemplateExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateExerciseDao {
    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY sort_order ASC")
    fun getExercisesByTemplate(templateId: Long): Flow<List<TemplateExercise>>
    
    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY sort_order ASC")
    suspend fun getExercisesByTemplateId(templateId: Long): List<TemplateExercise>
    
    @Insert
    suspend fun insertTemplateExercise(templateExercise: TemplateExercise): Long
    
    @Update
    suspend fun updateTemplateExercise(templateExercise: TemplateExercise)
    
    @Delete
    suspend fun deleteTemplateExercise(templateExercise: TemplateExercise)
    
    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteExercisesByTemplate(templateId: Long)
    
    @Query("DELETE FROM template_exercises WHERE templateId IN (SELECT id FROM workout_templates WHERE userId = :userId AND isPreset = 0)")
    suspend fun deleteByUser(userId: Long)
}



