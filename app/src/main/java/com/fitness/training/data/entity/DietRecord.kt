package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diet_records")
data class DietRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0, // 用户ID
    val date: Long = System.currentTimeMillis(),
    val mealType: String, // 早餐、午餐、晚餐、加餐
    val foodName: String,
    val calories: Int = 0, // 卡路里
    val protein: Float = 0f, // 蛋白质(g)
    val carbs: Float = 0f, // 碳水(g)
    val fat: Float = 0f, // 脂肪(g)
    val amount: String = "" // 份量描述
)
