package com.fitness.training.data.dao

import androidx.room.*
import com.fitness.training.data.entity.WorkoutExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY `order` ASC")
    fun getExercisesByWorkout(workoutId: Long): Flow<List<WorkoutExercise>>
    
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY `order` ASC")
    suspend fun getExercisesByWorkoutId(workoutId: Long): List<WorkoutExercise>
    
    @Insert
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long
    
    @Update
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise)
    
    @Delete
    suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExercise)
    
    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesByWorkout(workoutId: Long)
    
    @Query("DELETE FROM workout_exercises WHERE workoutId IN (SELECT id FROM workouts WHERE userId = :userId)")
    suspend fun deleteByUser(userId: Long)
}



