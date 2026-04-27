package com.fitness.training.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.fitness.training.data.entity.DietRecord

@Dao
interface DietRecordDao {
    @Insert
    suspend fun insert(record: DietRecord): Long
    
    @Update
    suspend fun update(record: DietRecord)
    
    @Delete
    suspend fun delete(record: DietRecord)
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay ORDER BY id ASC")
    fun getRecordsByDate(userId: Long, startOfDay: Long, endOfDay: Long): LiveData<List<DietRecord>>
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay ORDER BY id ASC")
    suspend fun getRecordsByDateSync(userId: Long, startOfDay: Long, endOfDay: Long): List<DietRecord>
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllRecords(userId: Long): LiveData<List<DietRecord>>
    
    @Query("SELECT SUM(calories) FROM diet_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay")
    suspend fun getTotalCaloriesByDate(userId: Long, startOfDay: Long, endOfDay: Long): Int?
    
    @Query("SELECT SUM(protein) FROM diet_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay")
    suspend fun getTotalProteinByDate(userId: Long, startOfDay: Long, endOfDay: Long): Float?
    
    @Query("SELECT DISTINCT date FROM diet_records WHERE userId = :userId")
    suspend fun getAllRecordDates(userId: Long): List<Long>
    
    @Query("SELECT EXISTS(SELECT 1 FROM diet_records WHERE userId = :userId AND strftime('%Y-%m-%d', date/1000, 'unixepoch', 'localtime') = :date)")
    suspend fun hasRecordOnDate(userId: Long, date: String): Boolean
    
    @Query("DELETE FROM diet_records WHERE userId = :userId")
    suspend fun deleteByUser(userId: Long)
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId AND strftime('%Y-%m-%d', date/1000, 'unixepoch', 'localtime') = :date")
    suspend fun getByDateSync(userId: Long, date: String): List<DietRecord>
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllRecordsSync(userId: Long): List<DietRecord>
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId ORDER BY date DESC")
    suspend fun getDietRecordsByUserId(userId: Long): List<DietRecord>
}
