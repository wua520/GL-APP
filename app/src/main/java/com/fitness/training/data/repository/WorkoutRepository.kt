package com.fitness.training.data.repository

import com.fitness.training.data.dao.*
import com.fitness.training.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val workoutSetDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao
) {
    // Workout
    fun getAllWorkouts(userId: Long): Flow<List<Workout>> = workoutDao.getAllWorkouts(userId)
    
    fun getWorkoutsByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<Workout>> = 
        workoutDao.getWorkoutsByDateRange(userId, startDate, endDate)
    
    suspend fun getWorkoutById(id: Long): Workout? = workoutDao.getWorkoutById(id)
    
    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)
    
    suspend fun updateWorkout(workout: Workout) = workoutDao.updateWorkout(workout)
    
    suspend fun deleteWorkout(workout: Workout) = workoutDao.deleteWorkout(workout)
    
    suspend fun deleteWorkoutById(id: Long) = workoutDao.deleteWorkoutById(id)
    
    fun getWorkoutCount(userId: Long): Flow<Int> = workoutDao.getWorkoutCount(userId)
    
    suspend fun getWorkoutCountByDateRange(userId: Long, startDate: Long, endDate: Long): Int = 
        workoutDao.getWorkoutCountByDateRange(userId, startDate, endDate)
    
    suspend fun getTotalDurationByDateRange(userId: Long, startDate: Long, endDate: Long): Long? = 
        workoutDao.getTotalDurationByDateRange(userId, startDate, endDate)
    
    // 获取训练详情（包含动作和组数据）
    suspend fun getWorkoutWithDetails(workoutId: Long): WorkoutWithDetails? {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return null
        val workoutExercises = workoutExerciseDao.getExercisesByWorkout(workoutId).first()
        
        val exerciseDetails = workoutExercises.map { we ->
            val exercise = exerciseDao.getExerciseById(we.exerciseId)
            val sets = workoutSetDao.getSetsByWorkoutExercise(we.id).first()
            WorkoutExerciseDetail(
                workoutExercise = we,
                exercise = exercise,
                sets = sets
            )
        }
        
        return WorkoutWithDetails(workout, exerciseDetails)
    }
    
    // WorkoutExercise
    fun getExercisesByWorkout(workoutId: Long): Flow<List<WorkoutExercise>> = 
        workoutExerciseDao.getExercisesByWorkout(workoutId)
    
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long = 
        workoutExerciseDao.insertWorkoutExercise(workoutExercise)
    
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise) = 
        workoutExerciseDao.updateWorkoutExercise(workoutExercise)
    
    suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExercise) = 
        workoutExerciseDao.deleteWorkoutExercise(workoutExercise)
    
    // WorkoutSet
    fun getSetsByWorkoutExercise(workoutExerciseId: Long): Flow<List<WorkoutSet>> = 
        workoutSetDao.getSetsByWorkoutExercise(workoutExerciseId)
    
    suspend fun insertSet(set: WorkoutSet): Long = workoutSetDao.insertSet(set)
    
    suspend fun updateSet(set: WorkoutSet) = workoutSetDao.updateSet(set)
    
    suspend fun deleteSet(set: WorkoutSet) = workoutSetDao.deleteSet(set)
    
    // Exercise
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()
    
    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)
    
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<Exercise>> = 
        exerciseDao.getExercisesByMuscleGroup(muscleGroup)
    
    fun searchExercises(query: String): Flow<List<Exercise>> = exerciseDao.searchExercises(query)
    
    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insertExercise(exercise)
    
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.updateExercise(exercise)
}

// 训练详情数据类
data class WorkoutWithDetails(
    val workout: Workout,
    val exercises: List<WorkoutExerciseDetail>
)

data class WorkoutExerciseDetail(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise?,
    val sets: List<WorkoutSet>
)
