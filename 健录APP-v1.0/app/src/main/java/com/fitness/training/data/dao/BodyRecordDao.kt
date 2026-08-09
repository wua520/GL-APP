package com.fitness.training.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.fitness.training.data.entity.BodyRecord

@Dao
interface BodyRecordDao {
    @Query("SELECT * FROM body_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllByUser(userId: Long): LiveData<List<BodyRecord>>
    
    @Query("SELECT * FROM body_records WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    fun getLatestByUser(userId: Long): LiveData<BodyRecord?>
    
    @Query("SELECT * FROM body_records WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestByUserSync(userId: Long): BodyRecord?
    
    @Query("SELECT * FROM body_records WHERE userId = :userId AND date >= :startDate ORDER BY date ASC")
    fun getRecordsSince(userId: Long, startDate: Long): LiveData<List<BodyRecord>>
    
    @Insert
    suspend fun insert(record: BodyRecord): Long
    
    @Update
    suspend fun update(record: BodyRecord)
    
    @Delete
    suspend fun delete(record: BodyRecord)
    
    @Query("DELETE FROM body_records WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM body_records WHERE userId = :userId")
    suspend fun deleteByUser(userId: Long)
    
    @Query("SELECT * FROM body_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllByUserSync(userId: Long): List<BodyRecord>
    
    @Query("SELECT * FROM body_records WHERE userId = :userId ORDER BY date DESC")
    suspend fun getBodyRecordsByUserId(userId: Long): List<BodyRecord>
}
