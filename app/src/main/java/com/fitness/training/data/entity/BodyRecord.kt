package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_records")
data class BodyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val weight: Float? = null,      // 体重 kg
    val bodyFat: Float? = null,     // 体脂率 %
    val muscleMass: Float? = null,  // 肌肉量 kg
    val note: String = ""
)
