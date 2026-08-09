package com.fitness.training.data.dao

import androidx.room.*
import com.fitness.training.data.entity.Workout
import kotlinx.coroutines.flow.Flow

data class BestSetResult(
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val weight: Double,
    val reps: Int,
    val estimated1RM: Double
)

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY date DESC")
    fun getAllWorkouts(userId: Long): Flow<List<Workout>>
    
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): Workout?
    
    @Query("SELECT * FROM workouts WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getWorkoutsByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<Workout>>
    
    @Query("SELECT * FROM workouts WHERE userId = :userId AND date >= :startDate AND date < :endDate ORDER BY date DESC")
    suspend fun getWorkoutsByDateSync(userId: Long, startDate: Long, endDate: Long): List<Workout>
    
    @Insert
    suspend fun insertWorkout(workout: Workout): Long
    
    @Update
    suspend fun updateWorkout(workout: Workout)
    
    @Delete
    suspend fun deleteWorkout(workout: Workout)
    
    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)
    
    @Query("SELECT COUNT(*) FROM workouts WHERE userId = :userId")
    fun getWorkoutCount(userId: Long): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM workouts WHERE userId = :userId")
    suspend fun getWorkoutCountByUser(userId: Long): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM workouts WHERE userId = :userId AND strftime('%Y-%m-%d', date/1000, 'unixepoch', 'localtime') = :date)")
    suspend fun hasWorkoutOnDate(userId: Long, date: String): Boolean
    
    @Query("""
        SELECT COALESCE(SUM(ws.weight * ws.reps), 0) 
        FROM workout_sets ws 
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id 
        INNER JOIN workouts w ON we.workoutId = w.id 
        WHERE w.userId = :userId AND ws.weight > 0 AND ws.reps > 0
    """)
    suspend fun getTotalVolumeByUser(userId: Long): Double?
    
    @Query("""
        SELECT COUNT(DISTINCT we.exerciseId) 
        FROM workout_exercises we 
        INNER JOIN workouts w ON we.workoutId = w.id 
        INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
        WHERE w.userId = :userId AND ws.weight > 0 AND ws.reps > 0
    """)
    suspend fun getExerciseCountWithRecords(userId: Long): Int
    
    @Query("SELECT COUNT(*) FROM workouts WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    suspend fun getWorkoutCountByDateRange(userId: Long, startDate: Long, endDate: Long): Int
    
    @Query("SELECT SUM(duration) FROM workouts WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    suspend fun getTotalDurationByDateRange(userId: Long, startDate: Long, endDate: Long): Long?
    
    @Query("""
        SELECT COALESCE(SUM(ws.weight * ws.reps), 0) 
        FROM workout_sets ws 
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id 
        INNER JOIN workouts w ON we.workoutId = w.id 
        WHERE w.userId = :userId AND w.date >= :startDate AND w.date < :endDate AND ws.isCompleted = 1
    """)
    suspend fun getTotalVolumeByDateRange(userId: Long, startDate: Long, endDate: Long): Double
    
    @Query("""
        SELECT e.id as exerciseId, e.name as exerciseName, e.muscleGroup, 
               ws.weight, ws.reps, (ws.weight * (1 + ws.reps / 30.0)) as estimated1RM
        FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        INNER JOIN exercises e ON we.exerciseId = e.id
        WHERE w.userId = :userId AND ws.isCompleted = 1 AND ws.weight > 0 AND ws.reps > 0
        ORDER BY e.id, estimated1RM DESC
    """)
    suspend fun getBestSetsForUser(userId: Long): List<BestSetResult>
    
    @Query("DELETE FROM workouts WHERE userId = :userId")
    suspend fun deleteByUser(userId: Long)
    
    @Query("SELECT * FROM workouts WHERE userId = :userId AND date >= :startDate ORDER BY date DESC")
    suspend fun getRecentWorkoutsSync(userId: Long, startDate: Long): List<Workout>
    
    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY date DESC")
    fun getAllWorkoutsSync(userId: Long): List<Workout>
    
    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY date DESC")
    suspend fun getWorkoutsByUserId(userId: Long): List<Workout>
}


