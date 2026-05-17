package com.fitness.training.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.fitness.training.data.dao.BodyRecordDao
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.BodyRecord

class BodyRecordRepository(application: Application) {
    private val bodyRecordDao: BodyRecordDao
    
    init {
        val database = FitnessDatabase.getDatabase(application)
        bodyRecordDao = database.bodyRecordDao()
    }
    
    fun getAllByUser(userId: Long): LiveData<List<BodyRecord>> {
        return bodyRecordDao.getAllByUser(userId)
    }
    
    fun getLatestByUser(userId: Long): LiveData<BodyRecord?> {
        return bodyRecordDao.getLatestByUser(userId)
    }
    
    fun getRecordsSince(userId: Long, startDate: Long): LiveData<List<BodyRecord>> {
        return bodyRecordDao.getRecordsSince(userId, startDate)
    }
    
    suspend fun insert(record: BodyRecord): Long {
        return bodyRecordDao.insert(record)
    }
    
    suspend fun update(record: BodyRecord) {
        bodyRecordDao.update(record)
    }
    
    suspend fun delete(record: BodyRecord) {
        bodyRecordDao.delete(record)
    }
    
    suspend fun deleteById(id: Long) {
        bodyRecordDao.deleteById(id)
    }
}
