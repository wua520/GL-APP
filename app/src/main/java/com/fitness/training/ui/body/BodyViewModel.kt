package com.fitness.training.ui.body

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.fitness.training.data.entity.BodyRecord
import com.fitness.training.data.repository.BodyRecordRepository
import com.fitness.training.util.UserSession
import kotlinx.coroutines.launch
import java.util.Calendar

class BodyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BodyRecordRepository(application)
    private val userId = UserSession.getCurrentUserId(application)
    
    val allRecords: LiveData<List<BodyRecord>> = repository.getAllByUser(userId)
    val latestRecord: LiveData<BodyRecord?> = repository.getLatestByUser(userId)
    
    // 最近30天的记录用于图表
    private val thirtyDaysAgo = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -30)
    }.timeInMillis
    val recentRecords: LiveData<List<BodyRecord>> = repository.getRecordsSince(userId, thirtyDaysAgo)
    
    fun addRecord(weight: Float?, bodyFat: Float?, note: String, date: Long = System.currentTimeMillis(), onComplete: () -> Unit) {
        viewModelScope.launch {
            val record = BodyRecord(
                userId = userId,
                date = date,
                weight = weight,
                bodyFat = bodyFat,
                note = note
            )
            repository.insert(record)
            onComplete()
        }
    }
    
    fun updateRecord(record: BodyRecord, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.update(record)
            onComplete()
        }
    }
    
    fun deleteRecord(record: BodyRecord, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.delete(record)
            onComplete()
        }
    }
}
