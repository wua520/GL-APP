package com.fitness.training.data.dao

import androidx.room.*
import com.fitness.training.data.entity.User

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long
    
    @Update
    suspend fun update(user: User)
    
    @Delete
    suspend fun delete(user: User)
    
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?
    
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?
    
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserByIdSync(id: Long): User?
    
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun isUsernameExists(username: String): Int
}
