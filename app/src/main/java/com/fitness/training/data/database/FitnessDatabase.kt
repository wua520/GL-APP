package com.fitness.training.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fitness.training.data.dao.*
import com.fitness.training.data.entity.*

@Database(
    entities = [
        Exercise::class,
        Workout::class,
        WorkoutExercise::class,
        WorkoutSet::class,
        WorkoutTemplate::class,
        TemplateExercise::class,
        TrainingPlan::class,
        DietRecord::class,
        Food::class,
        User::class,
        BodyRecord::class,
        Achievement::class,
        AiChatMessage::class
    ],
    version = 16,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun templateExerciseDao(): TemplateExerciseDao
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun dietRecordDao(): DietRecordDao
    abstract fun foodDao(): FoodDao
    abstract fun userDao(): UserDao
    abstract fun bodyRecordDao(): BodyRecordDao
    abstract fun achievementDao(): AchievementDao
    abstract fun aiChatMessageDao(): AiChatMessageDao
    
    companion object {
        @Volatile
        private var INSTANCE: FitnessDatabase? = null
        
        fun getDatabase(context: Context): FitnessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitnessDatabase::class.java,
                    "fitness_database"
                )
                    .fallbackToDestructiveMigration() // 简化开发，生产环境应使用Migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


