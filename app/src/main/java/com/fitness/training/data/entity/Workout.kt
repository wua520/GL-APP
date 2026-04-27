package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0, // 用户ID
    val name: String = "",
    val date: Long = System.currentTimeMillis(), // 训练日期时间戳
    val duration: Long = 0, // 训练时长（毫秒）
    val notes: String = "", // 训练备注
    val templateId: Long? = null // 关联的训练模板ID
)



