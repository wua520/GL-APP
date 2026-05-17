package com.fitness.training.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.fitness.training.data.dao.AchievementDao
import com.fitness.training.data.dao.DietRecordDao
import com.fitness.training.data.dao.WorkoutDao
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.Achievement
import com.fitness.training.data.entity.AchievementData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AchievementRepository(context: Context) {
    
    private val database = FitnessDatabase.getDatabase(context)
    private val achievementDao: AchievementDao = database.achievementDao()
    private val workoutDao: WorkoutDao = database.workoutDao()
    private val dietRecordDao: DietRecordDao = database.dietRecordDao()
    
    val allAchievements: LiveData<List<Achievement>> = achievementDao.getAllAchievements()
    val unlockedCount: LiveData<Int> = achievementDao.getUnlockedCount()
    
    suspend fun initAchievements() {
        withContext(Dispatchers.IO) {
            // 更新成就数据，保留已有的解锁状态和进度
            for (achievement in AchievementData.allAchievements) {
                val existing = achievementDao.getAchievementById(achievement.id)
                if (existing != null) {
                    // 保留进度和解锁状态，更新其他信息
                    achievementDao.insert(achievement.copy(
                        isUnlocked = existing.isUnlocked,
                        unlockedAt = existing.unlockedAt,
                        progress = existing.progress
                    ))
                } else {
                    achievementDao.insert(achievement)
                }
            }
        }
    }
    
    suspend fun checkAndUpdateAchievements(userId: Long) {
        withContext(Dispatchers.IO) {
            checkFirstLoginAchievement()
            checkStreakAchievements(userId)
            checkWorkoutCountAchievements(userId)
            checkVolumeAchievements(userId)
            checkDietStreakAchievements(userId)
            checkPrAchievements(userId)
        }
    }
    
    private suspend fun checkFirstLoginAchievement() {
        // 首次打开应用，直接解锁
        val achievement = achievementDao.getAchievementById("first_login")
        if (achievement != null && !achievement.isUnlocked) {
            achievementDao.updateProgress("first_login", 1)
            achievementDao.unlockAchievement("first_login", System.currentTimeMillis())
        }
    }
    
    private suspend fun checkStreakAchievements(userId: Long) {
        val streak = calculateCurrentStreak(userId)
        val streakAchievements = listOf("streak_3", "streak_7", "streak_14", "streak_30")
        val targets = listOf(3, 7, 14, 30)
        
        for (i in streakAchievements.indices) {
            val achievement = achievementDao.getAchievementById(streakAchievements[i])
            if (achievement != null && !achievement.isUnlocked) {
                achievementDao.updateProgress(streakAchievements[i], streak)
                if (streak >= targets[i]) {
                    achievementDao.unlockAchievement(streakAchievements[i], System.currentTimeMillis())
                }
            }
        }
    }
    
    private suspend fun checkWorkoutCountAchievements(userId: Long) {
        val count = workoutDao.getWorkoutCountByUser(userId)
        val workoutAchievements = listOf("workout_10", "workout_50", "workout_100", "workout_500")
        val targets = listOf(10, 50, 100, 500)
        
        for (i in workoutAchievements.indices) {
            val achievement = achievementDao.getAchievementById(workoutAchievements[i])
            if (achievement != null && !achievement.isUnlocked) {
                achievementDao.updateProgress(workoutAchievements[i], count)
                if (count >= targets[i]) {
                    achievementDao.unlockAchievement(workoutAchievements[i], System.currentTimeMillis())
                }
            }
        }
    }
    
    private suspend fun checkVolumeAchievements(userId: Long) {
        val totalVolume = workoutDao.getTotalVolumeByUser(userId) ?: 0.0
        val volumeKg = totalVolume.toInt()
        val volumeAchievements = listOf("volume_1t", "volume_10t", "volume_100t")
        val targets = listOf(1000, 10000, 100000)
        
        for (i in volumeAchievements.indices) {
            val achievement = achievementDao.getAchievementById(volumeAchievements[i])
            if (achievement != null && !achievement.isUnlocked) {
                achievementDao.updateProgress(volumeAchievements[i], volumeKg)
                if (volumeKg >= targets[i]) {
                    achievementDao.unlockAchievement(volumeAchievements[i], System.currentTimeMillis())
                }
            }
        }
    }
    
    private suspend fun checkDietStreakAchievements(userId: Long) {
        val streak = calculateDietStreak(userId)
        val dietAchievements = listOf("diet_3", "diet_7", "diet_14")
        val targets = listOf(3, 7, 14)
        
        for (i in dietAchievements.indices) {
            val achievement = achievementDao.getAchievementById(dietAchievements[i])
            if (achievement != null && !achievement.isUnlocked) {
                achievementDao.updateProgress(dietAchievements[i], streak)
                if (streak >= targets[i]) {
                    achievementDao.unlockAchievement(dietAchievements[i], System.currentTimeMillis())
                }
            }
        }
    }
    
    private suspend fun checkPrAchievements(userId: Long) {
        // 计算有多少个动作有 PR 记录（即有训练数据的动作数量）
        val prCount = workoutDao.getExerciseCountWithRecords(userId)
        val prAchievements = listOf("pr_1", "pr_10", "pr_50")
        val targets = listOf(1, 10, 50)
        
        for (i in prAchievements.indices) {
            val achievement = achievementDao.getAchievementById(prAchievements[i])
            if (achievement != null && !achievement.isUnlocked) {
                achievementDao.updateProgress(prAchievements[i], prCount)
                if (prCount >= targets[i]) {
                    achievementDao.unlockAchievement(prAchievements[i], System.currentTimeMillis())
                }
            }
        }
    }
    
    private suspend fun calculateCurrentStreak(userId: Long): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var streak = 0
        
        // 从今天开始往前数
        while (true) {
            val dateStr = dateFormat.format(calendar.time)
            val hasWorkout = workoutDao.hasWorkoutOnDate(userId, dateStr)
            if (hasWorkout) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // 如果今天没有训练，检查昨天
                if (streak == 0) {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = dateFormat.format(calendar.time)
                    if (workoutDao.hasWorkoutOnDate(userId, yesterdayStr)) {
                        streak++
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        continue
                    }
                }
                break
            }
        }
        return streak
    }
    
    private suspend fun calculateDietStreak(userId: Long): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var streak = 0
        
        while (true) {
            val dateStr = dateFormat.format(calendar.time)
            val hasDiet = dietRecordDao.hasRecordOnDate(userId, dateStr)
            if (hasDiet) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                if (streak == 0) {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = dateFormat.format(calendar.time)
                    if (dietRecordDao.hasRecordOnDate(userId, yesterdayStr)) {
                        streak++
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        continue
                    }
                }
                break
            }
        }
        return streak
    }
}
