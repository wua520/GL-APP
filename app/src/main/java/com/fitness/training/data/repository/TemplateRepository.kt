package com.fitness.training.data.repository

import com.fitness.training.data.dao.ExerciseDao
import com.fitness.training.data.dao.TemplateExerciseDao
import com.fitness.training.data.dao.WorkoutTemplateDao
import com.fitness.training.data.entity.Exercise
import com.fitness.training.data.entity.TemplateExercise
import com.fitness.training.data.entity.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TemplateRepository(
    private val templateDao: WorkoutTemplateDao,
    private val templateExerciseDao: TemplateExerciseDao,
    private val exerciseDao: ExerciseDao
) {
    fun getAllTemplates(userId: Long): Flow<List<WorkoutTemplate>> = templateDao.getAllTemplates(userId)
    
    suspend fun getTemplateById(id: Long): WorkoutTemplate? = templateDao.getTemplateById(id)
    
    suspend fun insertTemplate(template: WorkoutTemplate): Long = templateDao.insertTemplate(template)
    
    suspend fun updateTemplate(template: WorkoutTemplate) = templateDao.updateTemplate(template)
    
    suspend fun deleteTemplate(template: WorkoutTemplate) = templateDao.deleteTemplate(template)
    
    fun getTemplateExercises(templateId: Long): Flow<List<TemplateExercise>> = 
        templateExerciseDao.getExercisesByTemplate(templateId)
    
    suspend fun insertTemplateExercise(templateExercise: TemplateExercise): Long = 
        templateExerciseDao.insertTemplateExercise(templateExercise)
    
    suspend fun deleteTemplateExercises(templateId: Long) = 
        templateExerciseDao.deleteExercisesByTemplate(templateId)
    
    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)
    
    // 获取模板详情（包含动作信息）
    suspend fun getTemplateWithExercises(templateId: Long): TemplateWithExercises? {
        val template = templateDao.getTemplateById(templateId) ?: return null
        val templateExercises = templateExerciseDao.getExercisesByTemplate(templateId).first()
        val exerciseDetails = templateExercises.map { te ->
            val exercise = exerciseDao.getExerciseById(te.exerciseId)
            TemplateExerciseDetail(te, exercise)
        }
        return TemplateWithExercises(template, exerciseDetails)
    }
}

data class TemplateExerciseDetail(
    val templateExercise: TemplateExercise,
    val exercise: Exercise?
)

data class TemplateWithExercises(
    val template: WorkoutTemplate,
    val exercises: List<TemplateExerciseDetail>
)
