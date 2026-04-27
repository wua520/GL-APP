package com.fitness.training.data.dao

import androidx.room.*
import com.fitness.training.data.entity.WorkoutSet
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
    fun getSetsByWorkoutExercise(workoutExerciseId: Long): Flow<List<WorkoutSet>>
    
    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
    suspend fun getSetsByWorkoutExerciseId(workoutExerciseId: Long): List<WorkoutSet>
    
    @Insert
    suspend fun insertSet(set: WorkoutSet): Long
    
    @Update
    suspend fun updateSet(set: WorkoutSet)
    
    @Delete
    suspend fun deleteSet(set: WorkoutSet)
    
    @Query("DELETE FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deleteSetsByWorkoutExercise(workoutExerciseId: Long)
    
    @Query("DELETE FROM workout_sets WHERE workoutExerciseId IN (SELECT we.id FROM workout_exercises we INNER JOIN workouts w ON we.workoutId = w.id WHERE w.userId = :userId)")
    suspend fun deleteByUser(userId: Long)
}



