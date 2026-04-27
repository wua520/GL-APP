package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val icon: String,  // emoji
    val category: String,  // streak, workout, weight, diet
    val targetValue: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Int = 0
)

// 预定义的成就列表
object AchievementData {
    val allAchievements = listOf(
        // 新手成就
        Achievement("first_login", "初来乍到", "首次打开应用", "👋", "special", 1),
        
        // 连续训练系列
        Achievement("streak_3", "初露锋芒", "连续训练3天", "⭐", "streak", 3),
        Achievement("streak_7", "坚持一周", "连续训练7天", "⭐", "streak", 7),
        Achievement("streak_14", "两周达人", "连续训练14天", "⭐", "streak", 14),
        Achievement("streak_30", "月度冠军", "连续训练30天", "⭐", "streak", 30),
        
        // 训练次数系列
        Achievement("workout_10", "健身新手", "累计完成10次训练", "✊", "workout", 10),
        Achievement("workout_50", "健身达人", "累计完成50次训练", "✊", "workout", 50),
        Achievement("workout_100", "健身狂人", "累计完成100次训练", "✊", "workout", 100),
        Achievement("workout_500", "铁人传说", "累计完成500次训练", "✊", "workout", 500),
        
        // 总训练量系列
        Achievement("volume_1t", "一吨俱乐部", "累计举起1吨重量", "💎", "volume", 1000),
        Achievement("volume_10t", "十吨俱乐部", "累计举起10吨重量", "💎", "volume", 10000),
        Achievement("volume_100t", "百吨俱乐部", "累计举起100吨重量", "💎", "volume", 100000),
        
        // PR突破系列（有记录的动作数量）
        Achievement("pr_1", "首次突破", "完成1个动作的训练记录", "🏆", "pr", 1),
        Achievement("pr_10", "不断超越", "完成10个动作的训练记录", "🏆", "pr", 10),
        Achievement("pr_50", "极限挑战者", "完成50个动作的训练记录", "🏆", "pr", 50),
        
        // 饮食记录系列
        Achievement("diet_3", "饮食新手", "连续记录饮食3天", "🍎", "diet", 3),
        Achievement("diet_7", "饮食达人", "连续记录饮食7天", "🍎", "diet", 7),
        Achievement("diet_14", "营养专家", "连续记录饮食14天", "🍎", "diet", 14)
    )
}
