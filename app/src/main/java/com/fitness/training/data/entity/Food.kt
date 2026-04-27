package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0, // 用户ID（自定义食物）
    val name: String,
    val category: String = "", // 主食、肉类、蔬菜、水果、奶制品、零食等
    val calories: Int = 0, // 每100g的千卡
    val protein: Float = 0f, // 每100g的蛋白质(g)
    val carbs: Float = 0f, // 每100g的碳水(g)
    val fat: Float = 0f, // 每100g的脂肪(g)
    val unit: String = "100g", // 默认单位
    val isCustom: Boolean = false // 是否为用户自定义
)
