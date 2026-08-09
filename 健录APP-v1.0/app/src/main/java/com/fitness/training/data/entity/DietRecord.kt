package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diet_records",
    indices = [
        Index(
            value = ["userId", "agentActionId", "recordKey"],
            unique = true,  // 唯一约束：一个用户的一个动作的一个记录键只能有一条记录
            name = "idx_user_action_recordkey_diet"
        ),
        Index(
            value = ["userId", "agentActionId"],
            unique = false,  // 非唯一索引：便于查询同一个action下的所有记录
            name = "idx_user_action_diet"
        )
    ]
)
data class DietRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0, // 用户ID
    val agentActionId: Long?, // Agent创建用正数actionId，手动创建用null
    val recordKey: String? = null, // 记录键：Agent创建时必填，用于幂等性（格式："mealType_foodName_序号"）
    val date: Long = System.currentTimeMillis(),
    val mealType: String, // 早餐、午餐、晚餐、加餐
    val foodName: String,
    val calories: Int = 0, // 卡路里
    val protein: Float = 0f, // 蛋白质(g)
    val carbs: Float = 0f, // 碳水(g)
    val fat: Float = 0f, // 脂肪(g)
    val amount: String = "" // 份量描述
)
