package com.fitness.training.data.dao

import androidx.room.*
import com.fitness.training.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?
    
    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercises WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteExercises(): Flow<List<Exercise>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long
    
    @Update
    suspend fun updateExercise(exercise: Exercise)
    
    @Delete
    suspend fun deleteExercise(exercise: Exercise)
    
    @Query("UPDATE exercises SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
    
    @Query("DELETE FROM exercises WHERE isCustom = 1")
    suspend fun deleteCustomExercises()
    
    @Query("UPDATE exercises SET isCustom = 0 WHERE isCustom = 1 AND name != :excludeName")
    suspend fun fixFakeCustomExercises(excludeName: String)
}



